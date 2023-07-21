package io.taig.geojson

final case class Feature(id: Option[String], geometry: Option[Geometry], properties: Option[Map[String, String]])
