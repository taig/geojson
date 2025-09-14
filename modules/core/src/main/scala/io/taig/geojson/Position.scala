package io.taig.geojson

import cats.derived.*
import cats.Order

final case class Position(longitude: Double, latitude: Double, altitude: Option[Double]) derives Order:
  def toPoint: Point = Point(this)
