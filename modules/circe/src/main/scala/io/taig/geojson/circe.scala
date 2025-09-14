package io.taig.geojson

import cats.syntax.all.*
import io.circe.Decoder
import io.circe.DecodingFailure
import io.circe.Encoder
import io.circe.JsonObject
import io.circe.syntax.*

trait circe:
  given decodeGeoJson: Decoder[GeoJson] = cursor =>
    cursor
      .get[String]("type")
      .flatMap:
        case FeatureCollection.Type => cursor.as[FeatureCollection]
        case Feature.Type           => cursor.as[Feature]
        case _                      => cursor.as[Geometry]

  given encodeGeoJson: Encoder.AsObject[GeoJson] =
    case geoJson: FeatureCollection => geoJson.asJsonObject
    case geoJson: Feature           => geoJson.asJsonObject
    case geoJson: Geometry          => geoJson.asJsonObject

  given decodeFeatureCollection: Decoder[FeatureCollection] = cursor =>
    cursor.get[List[Feature]]("features").map(FeatureCollection.apply)

  given encodeFeatureCollection: Encoder.AsObject[FeatureCollection] = feature =>
    JsonObject(
      "type" := "FeatureCollection",
      "features" := feature.features
    )

  given decodeFeature: Decoder[Feature] = cursor =>
    for
      id <- cursor.get[Option[String]]("id")
      geometry <- cursor.get[Option[Geometry]]("geometry")
      properties <- cursor.get[Option[Map[String, String]]]("properties")
    yield Feature(id, geometry, properties)

  given encodeFeature: Encoder.AsObject[Feature] = feature =>
    JsonObject(
      "type" := "Feature",
      "id" := feature.geometry,
      "geometry" := feature.geometry,
      "properties" := feature.properties
    )

  given decodeGeometry: Decoder[Geometry] = cursor =>
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

  given encodeGeometry: Encoder.AsObject[Geometry] =
    case geometry: GeometryCollection => geometry.asJsonObject
    case geometry: LineString         => geometry.asJsonObject
    case geometry: MultiLineString    => geometry.asJsonObject
    case geometry: MultiPoint         => geometry.asJsonObject
    case geometry: MultiPolygon       => geometry.asJsonObject
    case geometry: Point              => geometry.asJsonObject
    case geometry: Polygon            => geometry.asJsonObject

  given decodeGeometryCollection: Decoder[GeometryCollection] =
    _.get[List[Geometry]]("geometries").map(GeometryCollection.apply)

  given encodeGeometryCollection: Encoder.AsObject[GeometryCollection] = collection =>
    JsonObject("type" := GeometryCollection.Type, "geometries" := collection.geometries)

  given decodeLineString: Decoder[LineString] = cursor =>
    cursor
      .get[List[Position]]("coordinates")
      .flatMap: coordinates =>
        LineString
          .fromCoordinates(coordinates)
          .toRight(DecodingFailure("Invalid format", cursor.downField("coordinates").history))

  given encodeLineString: Encoder.AsObject[LineString] = lineString =>
    JsonObject("type" := LineString.Type, "coordinates" := lineString.toCoordinates)

  given decodeMultiLineString: Decoder[MultiLineString] = cursor =>
    cursor
      .get[List[List[Position]]]("coordinates")
      .flatMap: coordinates =>
        MultiLineString
          .fromCoordinates(coordinates)
          .toRight(DecodingFailure("Invalid format", cursor.downField("coordinates").history))

  given encodeMultiLineString: Encoder.AsObject[MultiLineString] = multiLineString =>
    JsonObject("type" := MultiLineString.Type, "coordinates" := multiLineString.toCoordinates)

  given decodeMultiPoint: Decoder[MultiPoint] =
    _.get[List[Position]]("coordinates").map(MultiPoint.fromCoordinates)

  given encodeMultiPoint: Encoder.AsObject[MultiPoint] = multiPoint =>
    JsonObject("type" := MultiPoint.Type, "coordinates" := multiPoint.toCoordinates)

  given decodeMultiPolygon: Decoder[MultiPolygon] =
    _.get[List[List[List[Position]]]]("coordinates").map(MultiPolygon.fromCoordinates)

  given encodeMultiPolygon: Encoder.AsObject[MultiPolygon] = multiPolygon =>
    JsonObject("type" := MultiPolygon.Type, "coordinates" := multiPolygon.toCoordinates)

  given decodePoint: Decoder[Point] = _.get[Position]("coordinates").map(Point.apply)

  given encodePoint: Encoder.AsObject[Point] = point =>
    JsonObject("type" := Point.Type, "coordinates" := point.position)

  given decodePolygon: Decoder[Polygon] =
    _.get[List[List[Position]]]("coordinates").map(Polygon.fromCoordinates)

  given encodePolygon: Encoder.AsObject[Polygon] = polygon =>
    JsonObject("type" := Polygon.Type, "coordinates" := polygon.toCoordinates)

  given decodePosition: Decoder[Position] = Decoder[List[Double]].emap:
    case longitude :: latitude :: Nil              => Position(longitude, latitude, altitude = none).asRight
    case longitude :: latitude :: elevation :: Nil => Position(longitude, latitude, elevation.some).asRight
    case _                                         => "Invalid format".asLeft

  given encodePosition: Encoder.AsArray[Position] =
    case Position(longitude, latitude, None)            => Vector(longitude.asJson, latitude.asJson)
    case Position(longitude, latitude, Some(elevation)) => Vector(longitude.asJson, latitude.asJson, elevation.asJson)

object circe extends circe
