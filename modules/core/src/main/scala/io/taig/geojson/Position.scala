package io.taig.geojson

final case class Position(longitude: Double, latitude: Double, altitude: Option[Double]):
  def toPoint: Point = Point(this)
