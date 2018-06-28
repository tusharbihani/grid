package com.gu.mediaservice
package syntax

import org.elasticsearch.action._
import org.elasticsearch.action.get.GetResponse
import org.elasticsearch.search.SearchHit
import play.api.libs.json.{JsValue, Json}
import scala.concurrent.{Future, Promise}
import play.api.Logger

import scala.concurrent.{ExecutionContext, Future}


trait ElasticSearchSyntax {

  final implicit class GetResponseSyntax(self: GetResponse) {
    def sourceOpt: Option[JsValue] = Option(self.getSourceAsString) map Json.parse
  }

  final implicit class ActionRequestBuilderSyntax[A <: ActionResponse]
      (self: ActionRequestBuilder[_ <: ActionRequest, A, _]) {

    def executeAndLog(message: => String)(implicit ex: ExecutionContext): Future[A] = {
      val elapsed = {
        val start = System.currentTimeMillis
        () => System.currentTimeMillis - start
      }
      val promise = Promise[A]()
      val future = self.execute(
        new ActionListener[A] {
          def onFailure(e: Exception) { promise.failure(e) }
          def onResponse(response: A) { promise.success(response) }
        }
      )
      promise.future.foreach { case _ => Logger.info(s"$message - query returned successfully in ${elapsed()} ms") }
      promise.future.failed.foreach { case e => Logger.error(s"$message - query failed after ${elapsed()} ms: ${e.getMessage} cs: ${e.getCause}") }
      promise.future
    }
  }

  final implicit class SearchHitSyntax(self: SearchHit) {
    def sourceOpt: Option[JsValue] = Option(self.getSourceAsString) map Json.parse
  }

  final implicit class TermsBuilderSyntax(self: TermsBuilder) {
    // Annoyingly you can't exclude by array in the JAVA API
    // although you can in the REST client
    def excludeList(list: List[String]) = {
      self.exclude(list.map(Pattern.quote).mkString("|"))
    }
  }

}

object ElasticSearchSyntax extends ElasticSearchSyntax
