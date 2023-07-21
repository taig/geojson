package io.taig.geojson

sealed abstract class Geometry extends Product with Serializable

final case class GeometryCollection(geometries: List[Geometry]) extends Geometry

object GeometryCollection:
  val Type: String = "GeometryCollection"

final case class LineString(first: Position, second: Position, additional: List[Position]) extends Geometry:
  def toList: List[Position] = first :: second :: additional

object LineString:
  val Type: String = "LineString"

  def fromCoordinates(coordinates: List[Position]): Option[LineString] = coordinates match
    case first :: second :: tail => Some(LineString(first, second, tail))
    case _                       => None

final case class MultiLineString(lineStrings: List[LineString]) extends Geometry

object MultiLineString:
  val Type: String = "MultiLineString"

  def fromCoordinates(coordinates: List[List[Position]]): Option[MultiLineString] =
    coordinates
      .foldRight(Option(List.empty[LineString])):
        case (coordinates, Some(lineStrings)) => LineString.fromCoordinates(coordinates).map(_ :: lineStrings)
        case (_, None)                        => None
      .map(apply)

final case class MultiPoint(points: List[Point]) extends Geometry

object MultiPoint:
  val Type: String = "MultiPoint"

  def fromCoordinates(coordinates: List[Position]): MultiPoint = MultiPoint(coordinates.map(Point.apply))

final case class MultiPolygon(polygons: List[Polygon]) extends Geometry

object MultiPolygon:
  val Type: String = "MultiPolygon"

  val Empty: MultiPolygon = MultiPolygon(Nil)

  def fromCoordinates(coordinates: List[List[List[Position]]]): MultiPolygon =
    MultiPolygon(coordinates.map(Polygon.fromCoordinates))

final case class Point(position: Position) extends Geometry

object Point:
  val Type: String = "Point"

final case class Polygon(bounds: List[Position], holes: List[List[Position]]) extends Geometry:
  def toList: List[List[Position]] = bounds :: holes

object Polygon:
  val Type: String = "Polygon"

  val Empty: Polygon = Polygon(Nil, Nil)

  def fromCoordinates(coordinates: List[List[Position]]): Polygon = coordinates match
    case head :: tail => Polygon(head, tail)
    case Nil          => Empty
