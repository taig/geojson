package io.taig.geojson

import cats.syntax.all.*
import io.circe.{Decoder, Encoder}
import io.circe.syntax.*
import io.circe.JsonObject
import io.circe.DecodingFailure

trait circe:
  implicit val decodeGeoJson: Decoder[GeoJson] = cursor =>
    cursor
      .get[String]("type")
      .flatMap:
        case FeatureCollection.Type => cursor.as[FeatureCollection]
        case Feature.Type           => cursor.as[Feature]
        case _                      => cursor.as[Geometry]

  implicit val encodeGeoJson: Encoder.AsObject[GeoJson] =
    case geoJson: FeatureCollection => geoJson.asJsonObject
    case geoJson: Feature           => geoJson.asJsonObject
    case geoJson: Geometry          => geoJson.asJsonObject

  implicit final val decodeFeatureCollection: Decoder[FeatureCollection] = cursor =>
    cursor.get[List[Feature]]("features").map(FeatureCollection.apply)

  implicit final val encodeFeatureCollection: Encoder.AsObject[FeatureCollection] = feature =>
    JsonObject(
      "type" := "FeatureCollection",
      "features" := feature.features
    )

  implicit final val decodeFeature: Decoder[Feature] = cursor =>
    for
      id <- cursor.get[Option[String]]("id")
      geometry <- cursor.get[Option[Geometry]]("geometry")
      properties <- cursor.get[Option[Map[String, String]]]("properties")
    yield Feature(id, geometry, properties)

  implicit final val encodeFeature: Encoder.AsObject[Feature] = feature =>
    JsonObject(
      "type" := "Feature",
      "id" := feature.geometry,
      "geometry" := feature.geometry,
      "properties" := feature.properties
    )

  implicit final val decodeGeometry: Decoder[Geometry] = cursor =>
    cursor
      .get[String]("type")
      .flatMap:
        case GeometryCollection.Type => cursor.as[GeometryCollection]
        case LineString.Type         => cursor.as[LineString]
        case MultiLineString.Type    => cursor.as[MultiLineString]
        case MultiPoint.Type         => cursor.as[MultiPoint]
        case MultiPolygon.Type       => cursor.as[MultiPolygon]
        case Point.Type              => cursor.as[Point]
        case Polygon.Type            => cursor.as[Polygon]
        case tpe                     => DecodingFailure(s"Unknown type: $tpe", cursor.downField("type").history).asLeft

  implicit final val encodeGeometry: Encoder.AsObject[Geometry] =
    case geometry: GeometryCollection => geometry.asJsonObject
    case geometry: LineString         => geometry.asJsonObject
    case geometry: MultiLineString    => geometry.asJsonObject
    case geometry: MultiPoint         => geometry.asJsonObject
    case geometry: MultiPolygon       => geometry.asJsonObject
    case geometry: Point              => geometry.asJsonObject
    case geometry: Polygon            => geometry.asJsonObject

  implicit final val decodeGeometryCollection: Decoder[GeometryCollection] =
    _.get[List[Geometry]]("geometries").map(GeometryCollection.apply)

  implicit final val encodeGeometryCollection: Encoder.AsObject[GeometryCollection] = collection =>
    JsonObject("type" := GeometryCollection.Type, "geometries" := collection.geometries)

  implicit final val decodeLineString: Decoder[LineString] = cursor =>
    cursor
      .get[List[Position]]("coordinates")
      .flatMap: coordinates =>
        LineString
          .fromCoordinates(coordinates)
          .toRight(DecodingFailure("Invalid format", cursor.downField("coordinates").history))

  implicit final val encodeLineString: Encoder.AsObject[LineString] = lineString =>
    JsonObject("type" := LineString.Type, "coordinates" := lineString.toCoordinates)

  implicit final val decodeMultiLineString: Decoder[MultiLineString] = cursor =>
    cursor
      .get[List[List[Position]]]("coordinates")
      .flatMap: coordinates =>
        MultiLineString
          .fromCoordinates(coordinates)
          .toRight(DecodingFailure("Invalid format", cursor.downField("coordinates").history))

  implicit final val encodeMultiLineString: Encoder.AsObject[MultiLineString] = multiLineString =>
    JsonObject("type" := MultiLineString.Type, "coordinates" := multiLineString.toCoordinates)

  implicit final val decodeMultiPoint: Decoder[MultiPoint] =
    _.get[List[Position]]("coordinates").map(MultiPoint.fromCoordinates)

  implicit final val encodeMultiPoint: Encoder.AsObject[MultiPoint] = multiPoint =>
    JsonObject("type" := MultiPoint.Type, "coordinates" := multiPoint.toCoordinates)

  implicit final val decodeMultiPolygon: Decoder[MultiPolygon] =
    _.get[List[List[List[Position]]]]("coordinates").map(MultiPolygon.fromCoordinates)

  implicit final val encodeMultiPolygon: Encoder.AsObject[MultiPolygon] = multiPolygon =>
    JsonObject("type" := MultiPolygon.Type, "coordinates" := multiPolygon.toCoordinates)

  implicit final val decodePoint: Decoder[Point] = _.get[Position]("coordinates").map(Point.apply)

  implicit final val encodePoint: Encoder.AsObject[Point] = point =>
    JsonObject("type" := Point.Type, "coordinates" := point.position)

  implicit final val decodePolygon: Decoder[Polygon] =
    _.get[List[List[Position]]]("coordinates").map(Polygon.fromCoordinates)

  implicit final val encodePolygon: Encoder.AsObject[Polygon] = polygon =>
    JsonObject("type" := Polygon.Type, "coordinates" := polygon.toCoordinates)

  implicit final val decodePosition: Decoder[Position] = Decoder[List[Double]].emap:
    case longitude :: latitude :: Nil              => Position(longitude, latitude, altitude = none).asRight
    case longitude :: latitude :: elevation :: Nil => Position(longitude, latitude, elevation.some).asRight
    case _                                         => "Invalid format".asLeft

  implicit final val encodePosition: Encoder.AsArray[Position] =
    case Position(longitude, latitude, None)            => Vector(longitude.asJson, latitude.asJson)
    case Position(longitude, latitude, Some(elevation)) => Vector(longitude.asJson, latitude.asJson, elevation.asJson)

object circe extends circe
