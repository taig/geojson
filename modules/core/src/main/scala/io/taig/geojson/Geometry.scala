package io.taig.geojson

sealed abstract class Geometry extends Product with Serializable:
  def combine(geometry: Geometry): Geometry

  final def toGeometryCollection: GeometryCollection = this match
    case geometry: GeometryCollection => geometry
    case _                            => GeometryCollection(List(this))

final case class GeometryCollection(geometries: List[Geometry]) extends Geometry:
  override def combine(geometry: Geometry): GeometryCollection = geometry match
    case GeometryCollection(geometries) => GeometryCollection(this.geometries ++ geometries)
    case geometry                       => GeometryCollection(this.geometries :+ geometry)

object GeometryCollection:
  val Type: String = "GeometryCollection"

final case class LineString(first: Position, second: Position, additional: List[Position]) extends Geometry:
  def combine(geometry: LineString): MultiLineString = MultiLineString(List(this, geometry))
  def combine(geometry: MultiLineString): MultiLineString = MultiLineString(geometry.lineStrings :+ this)
  override def combine(geometry: Geometry): Geometry = geometry match
    case geomtery: LineString      => combine(geomtery)
    case geomtery: MultiLineString => combine(geomtery)
    case geometry                  => toGeometryCollection.combine(geometry)
  def toMultiLineString: MultiLineString = MultiLineString(List(this))
  def toCoordinates: List[Position] = first :: second :: additional

object LineString:
  val Type: String = "LineString"

  def fromCoordinates(coordinates: List[Position]): Option[LineString] = coordinates match
    case first :: second :: tail => Some(LineString(first, second, tail))
    case _                       => None

final case class MultiLineString(lineStrings: List[LineString]) extends Geometry:
  def combine(lineString: LineString): MultiLineString = MultiLineString(lineStrings :+ lineString)
  def combine(multiLineString: MultiLineString): MultiLineString =
    MultiLineString(lineStrings ++ multiLineString.lineStrings)
  override def combine(geometry: Geometry): Geometry = geometry match
    case geomtery: LineString      => combine(geomtery)
    case geomtery: MultiLineString => combine(geomtery)
    case geometry                  => toGeometryCollection.combine(geometry)
  def toCoordinates: List[List[Position]] = lineStrings.map(_.toCoordinates)

object MultiLineString:
  val Type: String = "MultiLineString"

  def fromCoordinates(coordinates: List[List[Position]]): Option[MultiLineString] = coordinates
    .foldRight(Option(List.empty[LineString])):
      case (coordinates, Some(lineStrings)) => LineString.fromCoordinates(coordinates).map(_ :: lineStrings)
      case (_, None)                        => None
    .map(apply)

final case class MultiPoint(points: List[Point]) extends Geometry:
  def combine(point: Point): MultiPoint = MultiPoint(points :+ point)
  def combine(multiPoint: MultiPoint): MultiPoint = MultiPoint(points ++ multiPoint.points)
  override def combine(geometry: Geometry): Geometry = geometry match
    case geomtery: Point      => combine(geomtery)
    case geomtery: MultiPoint => combine(geomtery)
    case geometry             => toGeometryCollection.combine(geometry)
  def toCoordinates: List[Position] = points.map(_.position)

object MultiPoint:
  val Type: String = "MultiPoint"

  def fromCoordinates(coordinates: List[Position]): MultiPoint = MultiPoint(coordinates.map(Point.apply))

final case class MultiPolygon(polygons: List[Polygon]) extends Geometry:
  def combine(polygon: Polygon): MultiPolygon = MultiPolygon(polygons :+ polygon)
  def combine(multiPolygon: MultiPolygon): MultiPolygon = MultiPolygon(polygons ++ multiPolygon.polygons)
  override def combine(geometry: Geometry): Geometry = geometry match
    case geomtery: Polygon      => combine(geomtery)
    case geomtery: MultiPolygon => combine(geomtery)
    case geometry               => toGeometryCollection.combine(geometry)
  def toCoordinates: List[List[List[Position]]] = polygons.map(_.toCoordinates)

object MultiPolygon:
  val Type: String = "MultiPolygon"
  val Empty: MultiPolygon = MultiPolygon(Nil)

  def fromCoordinates(coordinates: List[List[List[Position]]]): MultiPolygon =
    MultiPolygon(coordinates.map(Polygon.fromCoordinates))

final case class Point(position: Position) extends Geometry:
  def combine(point: Point): MultiPoint = MultiPoint(List(this, point))
  def combine(multiPoint: MultiPoint): MultiPoint = MultiPoint(this :: multiPoint.points)
  override def combine(geometry: Geometry): Geometry = geometry match
    case geomtery: Point      => combine(geomtery)
    case geomtery: MultiPoint => combine(geomtery)
    case geometry             => toGeometryCollection.combine(geometry)
  def toMultiPoint: MultiPoint = MultiPoint(List(this))

object Point:
  val Type: String = "Point"

final case class Polygon(bounds: List[Position], holes: List[List[Position]]) extends Geometry:
  def combine(polygon: Polygon): MultiPolygon = MultiPolygon(List(this, polygon))
  def combine(multiPolygon: MultiPolygon): MultiPolygon = MultiPolygon(this :: multiPolygon.polygons)
  override def combine(geometry: Geometry): Geometry = geometry match
    case geomtery: Polygon      => combine(geomtery)
    case geomtery: MultiPolygon => combine(geomtery)
    case geometry               => toGeometryCollection.combine(geometry)
  def toMultiPolygon: MultiPolygon = MultiPolygon(List(this))
  def toCoordinates: List[List[Position]] = bounds :: holes

object Polygon:
  val Type: String = "Polygon"
  val Empty: Polygon = Polygon(Nil, Nil)

  def fromCoordinates(coordinates: List[List[Position]]): Polygon = coordinates match
    case head :: tail => Polygon(head, tail)
    case Nil          => Empty
