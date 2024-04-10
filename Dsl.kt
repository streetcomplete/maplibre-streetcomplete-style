val isPoint = """["==", ["geometry-type"], "Point"]"""
val isLines = """["==", ["geometry-type"], "LineString"]"""
val isPolygon = """["==", ["geometry-type"], "Polygon"]"""

fun byZoom(vararg n: Pair<Double, Double>): String = byZoom(n.toList())

fun byZoom(n: Iterable<Pair<Double, Double>>): String {
    val values = n.flatMap { (z, v) -> listOf(z, v) }.joinToString()
    return """["interpolate", ["exponential", 2], ["zoom"], $values]"""
}

fun tagIs(key: String, value: Any) = """["==", ["get", "$key"], ${ if (value is String) "\"$value\"" else value }]"""
fun tagIsNot(key: String, value: Any) = """["!=", ["get", "$key"], ${ if (value is String) "\"$value\"" else value }]"""

fun tagIn(key: String, vararg values: String) =
    """["in", ["get", "$key"], ["literal", [${ values.joinToString { "\"$it\"" } }]]]"""

fun tagNotIn(key: String, vararg values: String) =
    """["!", ${tagIn(key, *values)}]"""

data class Layer(
    val id: String,
    val src: String,
    val filter: List<String> = emptyList(),
    val minZoom: Double? = null,
    val maxZoom: Double? = null,
    val paint: Paint
) {
  fun toJson() = "{ " +
      listOfNotNull(
          "\"id\": \"$id\"",
          "\"source\": \"jawg-streets\"",
          "\"source-layer\": \"$src\"",
          minZoom?.let { "\"minzoom\": $it" },
          maxZoom?.let { "\"maxzoom\": $it" },
          when (filter.size) {
            0 ->    null
            1 ->    "\"filter\": " + filter.single()
            else -> "\"filter\": [\"all\", " + filter.joinToString() + "]"
          },
          paint.toJson(),
      ).joinToString() +
      " }"
}

interface Paint {
  fun toJson(): String
}

data class Fill(
    val color: String,
    val opacity: String? = null,
    val antialias: Boolean? = null
): Paint {
  override fun toJson() = listOf(
      "\"type\": \"fill\"",
      "\"paint\": { " +
          listOfNotNull(
              "\"fill-color\": \"$color\"",
              opacity?.let { "\"fill-opacity\": $it" },
              antialias?.let { "\"fill-antialias\": $it" },
          ).joinToString() +
      "}"
  ).joinToString()
}

data class FillExtrusion(
    val color: String,
    val base: String,
    val height: String,
    val opacity: String? = null,
) : Paint {
  override fun toJson() = listOf(
      "\"type\": \"fill-extrusion\"",
      "\"paint\": { " +
          listOfNotNull(
              "\"fill-extrusion-color\": \"$color\"",
              "\"fill-extrusion-height\": $height",
              "\"fill-extrusion-base\": $base",
              opacity?.let { "\"fill-extrusion-opacity\": $it" },
          ).joinToString() +
      "}"
  ).joinToString()
}

data class Line(
    val color: String,
    val width: String,
    val gapWidth: String? = null,
    val offset: String? = null,
    val blur: String? = null,
    val opacity: String? = null,
    val miterLimit: Number? = null,
    val dashes: String? = null,
    val cap: String? = null,
    val join: String? = null
): Paint {
  override fun toJson() = listOfNotNull(
      "\"type\": \"line\"",
      "\"paint\": {" +
          listOfNotNull(
              "\"line-color\": \"$color\"",
              "\"line-width\": $width",
              gapWidth?.let  { "\"line-gap-width\": $it" },
              offset?.let  { "\"line-offset\": $it" },
              blur?.let  { "\"line-blur\": $it" },
              dashes?.let { "\"line-dasharray\": $it" },
              opacity?.let   { "\"line-opacity\": $it" },
          ).joinToString() +
          "}",
      if (cap != null || join != null || miterLimit != null) {
        "\"layout\": {" +
            listOfNotNull(
                cap?.let  { "\"line-cap\": \"$it\"" },
                join?.let { "\"line-join\": \"$it\"" },
                miterLimit?.let  { "\"line-miter-limit\": $it" },
            ).joinToString() +
            "}"
      } else null
  ).joinToString(",")
}

data class Circle(
    val color: String,
    val radius: String,
    val opacity: String? = null,
): Paint {
  override fun toJson() = listOfNotNull(
      "\"type\": \"circle\"",
      "\"paint\": {" +
          listOfNotNull(
              "\"circle-color\": \"$color\"",
              "\"circle-radius\": $radius",
              opacity?.let   { "\"circle-opacity\": $it" },
          ).joinToString() + "}",
  ).joinToString()
}

data class Symbol(
    val image: String,
    val color: String? = null,
    val padding: Number? = null,
    val placement: String? = null,
    val spacing: String? = null,
    val opacity: String? = null,
    val size: String? = null,
    val rotate: Number? = null,
    val rotationAlignment: String? = null,
) : Paint {
  override fun toJson() = listOfNotNull(
      "\"type\": \"symbol\"",
      "\"paint\": {" +
          listOfNotNull(
              color?.let { "\"icon-color\": \"$it\"" },
              opacity?.let { "\"icon-opacity\": $it" },
          ).joinToString() +
          "}",
      "\"layout\": {" +
          listOfNotNull(
              "\"icon-image\": \"$image\"",
              size?.let { "\"icon-size\": $it" },
              spacing?.let { "\"symbol-spacing\": $it" },
              placement?.let { "\"symbol-placement\": \"$it\"" },
              padding?.let { "\"icon-padding\": $it" },
              rotate?.let { "\"icon-rotate\": $it"  },
              rotationAlignment?.let { "\"icon-rotation-alignment\": \"$it\""  },
          ).joinToString() +
          "}",
  ).joinToString()
}

data class Text(
    val text: String,
    val size: String,
    val color: String,
    val fonts: List<String>,
    val wrap: Number? = null,
    val padding: Number? = null,
    val outlineColor: String? = null,
    val outlineWidth: Number? = null,
    val placement: String? = null,
    val opacity: String? = null,
    val sortKey: String? = null
) : Paint {
  override fun toJson() = listOfNotNull(
      "\"type\": \"symbol\"",
      "\"paint\": {" +
          listOfNotNull(
              "\"text-color\": \"$color\"",
              outlineColor?.let { "\"text-halo-color\": \"$it\"" },
              outlineWidth?.let { "\"text-halo-width\": $it" },
              opacity?.let { "\"text-opacity\": $it" },
          ).joinToString() +
          "}",
      "\"layout\": {" +
          listOfNotNull(
              "\"text-field\": $text",
              "\"text-size\": $size",
              "\"text-font\": [${fonts.joinToString { "\"$it\""}}]",
              placement?.let { "\"symbol-placement\": \"$it\"" },
              padding?.let { "\"text-padding\": $it" },
              wrap?.let { "\"text-max-width\": $it" },
              sortKey?.let { "\"symbol-sort-key\": $it" },
          ).joinToString() +
          "}",
  ).joinToString()
}
