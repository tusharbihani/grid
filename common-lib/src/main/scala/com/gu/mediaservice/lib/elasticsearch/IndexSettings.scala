package com.gu.mediaservice.lib.elasticsearch

import play.api.libs.json.Json
import play.api.libs.json.Json.JsValueWrapper

object IndexSettings {
  def customAnalyzer(tokenizer: String, filters: List[String]) =
    Json.obj(
      "type" -> "custom",
      "tokenizer" -> tokenizer,
      "filter" -> filters
    )

  // TODO rename `english_s_stemmer` as its an analyzer not a stemmer - would require a reindex.
  val enslishSStemmerAnalyzerName = "english_s_stemmer"
  val englishSStemmerAnalyzer = customAnalyzer("standard", List(
    "lowercase",
    "asciifolding",
    "english_possessive_stemmer",
    "gu_stopwords",
    "s_stemmer"
  ))

  val hierarchyAnalyserName = "hierarchyAnalyzer"
  val hierarchyAnalyzer = customAnalyzer("path_hierarchy", List("lowercase"))

  val filter: Map[String, Any] = List(
    ("s_stemmer"                  -> List(("type" -> "stemmer"), ("language"  -> "minimal_english")).toMap),
    ("gu_stopwords"               -> List(("type" -> "stop"),    ("stopwords" -> "_english_")).toMap),
    ("english_possessive_stemmer" -> List(("type" -> "stemmer"), ("language"  -> "possessive_english")).toMap)
  ).toMap

  val analyzers: Map[String, Any] = List(
    (enslishSStemmerAnalyzerName -> englishSStemmerAnalyzer),
    (hierarchyAnalyserName -> hierarchyAnalyzer)
  ).toMap

  val analysis: Map[String, Any] = List(
    ("filter" -> filter),
    ("analyzer" -> analyzers)
  ).toMap
  )

  val imageSettings: Map[String, Any] = List(
    ("analysis" -> analysis)
  ).toMap

}
