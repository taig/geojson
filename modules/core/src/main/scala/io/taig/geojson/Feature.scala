package io.taig.geojson

final case class Feature(id: Option[String], geometry: Option[Geometry], properties: Option[Map[String, String]]):
  def combine(feature: Feature): FeatureCollection = FeatureCollection(List(this, feature))
  def toFeatureCollection: FeatureCollection = FeatureCollection(List(this))
