package io.taig.geojson

import cats.syntax.all.*
import cats.Eq
import cats.derived.*

final case class LinearRing(first: Position, second: Position, third: Position, additional: List[Position]) derives Eq:
  def toCoordinates: List[Position] = first :: second :: third :: additional ::: List(first)

object LinearRing:
  def fromCoordinates(positions: List[Position]): Option[LinearRing] = PartialFunction.condOpt(positions):
    case first :: second :: third :: rest if rest.nonEmpty && rest.last === first =>
      LinearRing(first, second, third, rest.init)
