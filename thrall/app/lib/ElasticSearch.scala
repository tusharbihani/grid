package lib

import _root_.play.api.libs.json._
import com.gu.mediaservice.lib.elasticsearch.{ElasticSearchClient, ImageFields}
import com.gu.mediaservice.syntax._
import groovy.json.JsonSlurper
import org.elasticsearch.action.delete.DeleteResponse
import org.elasticsearch.action.update.{UpdateRequestBuilder, UpdateResponse}
import org.elasticsearch.action.updatebyquery.UpdateByQueryResponse
import org.elasticsearch.client.UpdateByQueryClientWrapper
import org.elasticsearch.index.engine.{DocumentMissingException, VersionConflictEngineException}
import org.elasticsearch.index.query.QueryBuilders
import org.elasticsearch.index.query.QueryBuilders.{existsQuery, matchAllQuery, matchQuery}
import org.elasticsearch.script.{Script, ScriptType}
import org.joda.time.DateTime

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

object ImageNotDeletable extends Throwable("Image cannot be deleted")

class ElasticSearch(config: ThrallConfig, metrics: ThrallMetrics) extends ElasticSearchClient with ImageFields {

  import com.gu.mediaservice.lib.formatting._

  private val imagesAlias = config.writeAlias
  private val host = config.elasticsearchHost
  private val port = config.int("es.port")
  private val cluster = config("es.cluster")

  val scriptType = ScriptType.valueOf("INLINE")

  lazy val updateByQueryClient = new UpdateByQueryClientWrapper(client)

  def currentIsoDateString = printDateTime(new DateTime())

  def indexImage(id: String, image: JsValue)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id) { request => request
      // Use upsert: if not present, will index the argument (the image)
      .setUpsert(Json.stringify(image))
      .setScript(new Script(
        ScriptType.INLINE,
        "groovy",
        // Note: we merge old and new identifiers (in that order) to make easier to re-ingest
        // images without forwarding any existing identifiers.
        """| previousIdentifiers = ctx._source.identifiers;
          | ctx._source += doc;
          | if (previousIdentifiers) {
          |   ctx._source.identifiers += previousIdentifiers;
          |   ctx._source.identifiers += doc.identifiers;
          | }
          |""".stripMargin +
          refreshEditsScript +
          updateLastModifiedScript +
          addToSuggestersScript,
        // if already present, will run the script with the provided parameters
        Map(
                "doc" -> asGroovy(asImageUpdate(image)),
                "lastModified" -> asGroovy(JsString(currentIsoDateString))
              ).asJava
        ))
      .executeAndLog(s"Indexing image $id")
      .incrementOnSuccess(metrics.indexedImages)
    }
  }

  def deleteImage(id: String)(implicit ex: ExecutionContext): List[Future[DeleteResponse]] = {

    val q = QueryBuilders
      .boolQuery
      .must(matchQuery("_id", id))
      .mustNot(existsQuery("exports"))
      .mustNot(existsQuery("usages"))

    prepareForMultipleIndexes { index =>
      // search for the image first, and then only delete and succeed
      // this is because the delete query does not respond with anything useful
      // TODO: is there a more efficient way to do this?
      client
        .prepareSearch(index)
        .setQuery(q)
        .executeAndLog(s"Searching for image to delete: $id")
        .flatMap { countQuery =>
          val deleteFuture = countQuery.getHits.totalHits match {
            case 1 => client.prepareDelete(index, imageType, id).executeAndLog(s"Deleting image $id")
            case _ => Future.failed(ImageNotDeletable)
          }
          deleteFuture
            .incrementOnSuccess(metrics.deletedImages)
            .incrementOnFailure(metrics.failedDeletedImages) { case ImageNotDeletable => true }
        }
    }
  }

  def updateImageUsages(id: String, usages: JsLookupResult, lastModified: JsLookupResult)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id) { request => request
      .setScript(new Script(ScriptType.INLINE,
        "groovy",
        s""" | if (!(ctx._source.usagesLastModified && ctx._source.usagesLastModified > lastModified)) {
            |   $replaceUsagesScript
            |   $updateLastModifiedScript
            | }
        """.stripMargin,
        Map(
          "usages" -> asGroovy(usages.getOrElse(JsNull)),
          "lastModified" -> asGroovy(lastModified.getOrElse(JsNull))
        ).asJava))
      .executeAndLog(s"updating usages on image $id")
      .recover { case e: DocumentMissingException => new UpdateResponse }
      .incrementOnFailure(metrics.failedUsagesUpdates) { case e: VersionConflictEngineException => true }
    }
  }

  def updateImageSyndicationRights(id: String, rights: JsLookupResult)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id) { request => request
        .setScript(new Script(ScriptType.INLINE,
        "groovy",
          s"""
             |   $replaceSyndicationRightsScript
             |   $updateLastModifiedScript
        """.stripMargin,
          Map(
            "syndicationRights" -> asGroovy(rights.getOrElse(JsNull)),
            "lastModified" -> asGroovy(Json.toJson(DateTime.now().toString()))
          ).asJava))
        .executeAndLog(s"updating syndicationRights on image $id")
        .recover { case e: DocumentMissingException => new UpdateResponse }
        .incrementOnFailure(metrics.failedSyndicationRightsUpdates) { case e: VersionConflictEngineException => true }
    }
  }

  def deleteAllImageUsages(id: String)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id) { request => request
      .setScript(new Script(ScriptType.INLINE,
        "groovy",
        deleteUsagesScript,
        Map[String,Object]().asJava))
        .executeAndLog(s"removing all usages on image $id")
        .recover { case e: DocumentMissingException => new UpdateResponse }
        .incrementOnFailure(metrics.failedUsagesUpdates) { case e: VersionConflictEngineException => true }
    }
  }

  def updateImageLeases(id: String, leaseByMedia: JsLookupResult, lastModified: JsLookupResult)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id){ request => request
      .setScript(new Script(ScriptType.INLINE,
        "groovy",
          replaceLeasesScript +
            updateLastModifiedScript,
        Map(
          "leaseByMedia" -> asGroovy(leaseByMedia.getOrElse(JsNull)),
          "lastModified" -> asGroovy(lastModified.getOrElse(JsNull))
        ).asJava))
      .executeAndLog(s"updating leases on image $id with: $leaseByMedia")
      .recover { case e: DocumentMissingException => new UpdateResponse }
      .incrementOnFailure(metrics.failedUsagesUpdates) { case e: VersionConflictEngineException => true }
    }
  }

  def updateImageExports(id: String, exports: JsLookupResult)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id) { request => request
      .setScript(new Script(ScriptType.INLINE,
      "groovy",
        addExportsScript +
          updateLastModifiedScript,
        Map(
          "exports" -> asGroovy(exports.getOrElse(JsNull)),
          "lastModified" -> asGroovy(JsString(currentIsoDateString))
        ).asJava))
      .executeAndLog(s"updating exports on image $id")
      .incrementOnFailure(metrics.failedExportsUpdates) { case e: VersionConflictEngineException => true }
    }
  }

  def deleteImageExports(id: String)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] =
    prepareImageUpdate(id) { request => request
      .setScript(new Script(ScriptType.INLINE,
      "groovy",
        deleteExportsScript +
          updateLastModifiedScript,
        Map(
          "lastModified" -> asGroovy(JsString(currentIsoDateString))
        ).asJava))
      .executeAndLog(s"removing exports from image $id")
      .incrementOnFailure(metrics.failedExportsUpdates) { case e: VersionConflictEngineException => true }
    }

  def applyImageMetadataOverride(id: String, metadata: JsLookupResult, lastModified: JsLookupResult)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] = {
    prepareImageUpdate(id) { request => request
      .setScript(new Script(ScriptType.INLINE,
        "groovy",
        s""" | if (!(ctx._source.userMetadataLastModified && ctx._source.userMetadataLastModified > lastModified)) {
            |   ctx._source.userMetadata = userMetadata;
            |   ctx._source.userMetadataLastModified = lastModified;
            |   $updateLastModifiedScript
            | }
      """.stripMargin +
          refreshEditsScript,
        Map(
          "userMetadata" -> asGroovy(metadata.getOrElse(JsNull)),
          "lastModified" -> asGroovy(lastModified.getOrElse(JsNull))
        ).asJava))
      .executeAndLog(s"updating user metadata on image $id")
      .incrementOnFailure(metrics.failedMetadataUpdates) { case e: VersionConflictEngineException => true }
    }
  }

  def setImageCollection(id: String, collections: JsLookupResult)(implicit ex: ExecutionContext): List[Future[UpdateResponse]] =
    prepareImageUpdate(id) { request => request
      .setScript(new Script(ScriptType.INLINE,
      "groovy",
        "ctx._source.collections = collections;" +
          updateLastModifiedScript,
        Map(
          "collections" -> asGroovy(collections.getOrElse(JsNull)),
          "lastModified" -> asGroovy(JsString(currentIsoDateString))
        ).asJava))
      .executeAndLog(s"setting collections on image $id")
      .incrementOnFailure(metrics.failedCollectionsUpdates) { case e: VersionConflictEngineException => true }
    }

  def prepareImageUpdate(id: String)(op: UpdateRequestBuilder => Future[UpdateResponse]): List[Future[UpdateResponse]] = {
    prepareForMultipleIndexes( index => {
          val updateRequest = client.prepareUpdate(index, imageType, id)
          op(updateRequest)
        }
    )
  }

  def prepareForMultipleIndexes[A](op: String => Future[A]) : List[Future[A]] = {
    getCurrentIndices.map( index => {
      op(index)
    })
  }


  def updateByQuery(script: String)(implicit ex: ExecutionContext): Future[UpdateByQueryResponse] =
    updateByQueryClient
      .prepareUpdateByQuery()
      .setScriptLang("groovy")
      .setIndices(imagesAlias)
      .setTypes(imageType)
      .setQuery(matchAllQuery)
      .setScript(script)
      .executeAndLog("Running update by query script")
      .incrementOnFailure(metrics.failedQueryUpdates) { case e: VersionConflictEngineException => true }

  def asGroovy(collection: JsValue) = new JsonSlurper().parseText(collection.toString)

  def asImageUpdate(image: JsValue): JsValue = {
    def removeUploadInformation: Reads[JsObject] =
      (__ \ "uploadTime").json.prune andThen
      (__ \ "userMetadata").json.prune andThen
      (__ \ "exports").json.prune andThen
      (__ \ "uploadedBy").json.prune

    image.transform(removeUploadInformation).get
  }

  private val addToSuggestersScript =
    """
      | suggestMetadataCredit = [ input: [ ctx._source.metadata.credit] ];
      | ctx._source.suggestMetadataCredit = suggestMetadataCredit;
    """.stripMargin

  // Create the exports key or add to it
  private val replaceUsagesScript =
    """
       | ctx._source.usages = usages;
       | ctx._source.usagesLastModified = lastModified;
    """

  private val replaceSyndicationRightsScript =
    """ctx._source.syndicationRights = syndicationRights;"""

  private val replaceLeasesScript =
    """ctx._source.leases = leaseByMedia;"""

  // Create the exports key or add to it
  private val addExportsScript =
    """| if (ctx._source.exports == null) {
       |   ctx._source.exports = exports;
       | } else {
       |   ctx._source.exports += exports;
       | }
    """.stripMargin

  private val deleteExportsScript =
    "ctx._source.remove('exports');".stripMargin

  private val deleteUsagesScript =
    "ctx._source.remove('usages');".stripMargin

  // Script that refreshes the "metadata" object by recomputing it
  // from the original metadata and the overrides
  private val refreshMetadataScript =
    """| ctx._source.metadata = ctx._source.originalMetadata;
       | if (ctx._source.userMetadata && ctx._source.userMetadata.metadata) {
       |   ctx._source.metadata += ctx._source.userMetadata.metadata;
       |   // Get rid of "" values
       |   def nonEmptyKeys = ctx._source.metadata.findAll { it.value != "" }.collect { it.key }
       |   ctx._source.metadata = ctx._source.metadata.subMap(nonEmptyKeys);
       | }
    """.stripMargin

  // Script that overrides the "usageRights" object from the "userMetadata".
  // We revert to the "originalUsageRights" if they are vacant.
  private val refreshUsageRightsScript =
    """| if (ctx._source.userMetadata && ctx._source.userMetadata.containsKey("usageRights")) {
       |   ur = ctx._source.userMetadata.usageRights.clone();
       |   ctx._source.usageRights = ur;
       | } else {
       |   ctx._source.usageRights = ctx._source.originalUsageRights;
       | }
    """.stripMargin

  // updating all user edits
  private val refreshEditsScript = refreshMetadataScript + refreshUsageRightsScript

  // Script that updates the "lastModified" property using the "lastModified" parameter
  private val updateLastModifiedScript =
    """| ctx._source.lastModified = lastModified;
    """.stripMargin

}
