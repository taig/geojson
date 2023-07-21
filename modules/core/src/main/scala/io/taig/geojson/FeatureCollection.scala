package io.taig.geojson

opaque type FeatureCollection = List[Feature]

object FeatureCollection:
  extension (self: FeatureCollection) def features: List[Feature] = self
  def apply(features: List[Feature]): FeatureCollection = features