package com.gu.mediaservice.lib.elasticsearch

import java.net.InetSocketAddress

import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest
import org.elasticsearch.client.Client
import org.elasticsearch.common.settings.Settings
import org.elasticsearch.common.transport.TransportAddress
import org.elasticsearch.transport.client.PreBuiltTransportClient
import play.api.Logger

import scala.collection.JavaConverters._

trait ElasticSearchClient {

  def host: String
  def port: Int
  def cluster: String
  def imagesAlias: String

  protected val imagesIndexPrefix = "images"
  protected val imageType = "image"

  val initialImagesIndex = "images"

  private lazy val settings: Settings = {
    Logger.info(s"Using cluster name $cluster")
    Settings.builder()
      .put("cluster.name", cluster)
      .put("client.transport.sniff", false)
      .build
  }

  lazy val client: Client = {
    Logger.info(s"Using cluster host $host")
    Logger.info(s"Using cluster port $port")

    new PreBuiltTransportClient(settings)
      .addTransportAddress(new TransportAddress(new InetSocketAddress(host, port)))
  }

  def ensureAliasAssigned() {
    Logger.info(s"Checking alias $imagesAlias is assigned to index…")

    if (!checkAliasExists) {
      ensureIndexExists(initialImagesIndex)
      assignAliasTo(initialImagesIndex)
    }
  }

  def ensureIndexExists(index: String) {
    Logger.info("Checking index exists…")
    val indexExists = client.admin.indices.prepareExists(index)
                        .execute.actionGet.isExists

    if (!indexExists) createIndex(index)
  }

  def createIndex(index: String) {

    Logger.info(s"Creating index $index")
    client.admin.indices
      .prepareCreate(index)
      .addMapping(imageType, Mappings.imageMapping)
      .setSettings(IndexSettings.imageSettings.asJava)
      .execute.actionGet
  }

  def deleteIndex(index: String) {
    Logger.info(s"Deleting index $index")
    client.admin.indices.delete(new DeleteIndexRequest(index)).actionGet
  }

  def checkAliasExists: Boolean = {

    import scala.collection.JavaConverters._

    client.admin.cluster
      .prepareState.execute.actionGet.getState.getMetaData.getAliasAndIndexLookup.asScala
      .get(imagesAlias)
      .isDefined

  }

  def getCurrentIndices: List[String] = {
    Option(client.admin.cluster
      .prepareState
      .get
      .getState
      .getMetaData
      .getIndices)
      .map(_.keys.toArray.map(_.toString).toList).getOrElse(Nil)
  }

  def assignAliasTo(index: String): IndicesAliasesResponse = {
    Logger.info(s"Assigning alias $imagesAlias to $index")
    client.admin.indices
      .prepareAliases
      .addAlias(index, imagesAlias)
      .execute.actionGet
  }

  def removeAliasFrom(index: String): IndicesAliasesResponse = {
    Logger.info(s"Removing alias $imagesAlias from $index")
    client.admin.indices
      .prepareAliases
      .removeAlias(index, imagesAlias)
      .execute.actionGet
  }

}
