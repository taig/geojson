package io.taig.geojson

import io.taig.geojson.circe.*
import io.circe.syntax.*

object App:
  @main
  def run = {
    val geometry: Geometry = Point(Position(123, 456, None))
    println(geometry.asJson)
  }
