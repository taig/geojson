package io.taig.geojson

import cats.Eq
import cats.data.NonEmptyList
import cats.derived.*
import cats.syntax.all.*

final case class LinearRing(first: Position, second: Position, third: Position, additional: List[Position]) derives Eq:
  def toCoordinates: NonEmptyList[Position] = NonEmptyList.ofInitLast(first :: second :: third :: additional, first)

object LinearRing:
  def fromCoordinates(positions: List[Position]): Option[LinearRing] = PartialFunction.condOpt(positions):
    case first :: second :: third :: rest if rest.nonEmpty && rest.last === first =>
      LinearRing(first, second, third, rest.init)
