package io.taig.geojson

import cats.Order
import cats.derived.*

final case class Position(longitude: Double, latitude: Double, altitude: Option[Double]) derives Order:
  def toPoint: Point = Point(this)
