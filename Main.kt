import java.io.File

val themeLight = Colors(
    earth = "#f3eacc",
    water = "#68d",
    waterShore = "#abe",
    green = "#c6ddaa",
    forest = "#a8c884",
    town = "#f3dacd",
    building = "rgb(204,214,238)",
    buildingOutline = "rgb(185,195,217)",
    pointBarrier = "#888",
    adminBoundary = "#e39",
    railway = "#99a",
    aeroway = "#fff",
    path = "#ca9",
    road = "#fff",
    roadOutline = "#ca9",
    pedestrian = "#f6eee6",
    motorway = "#fa8",
    motorwayOutline = "#a88",
    text = "#124",
    textOutline = "#fff",
    textWater = "#fff",
    textWaterOutline = "#349",
    privateOverlay = "rgba(221,221,221,0.75)",
    hillshadeLight = "hsl(220, 100%, 95%)",
    hillshadeShadow = "hsl(18, 100%, 40%)",
    onewayArrow = "#888",
)

val themeNight = Colors(
    earth = "#2e2e48",
    water = "#002",
    waterShore = "#228",
    green = "#363054",
    forest = "#403962",
    town = "#3d364e",
    building = "rgba(41,92,92,0.8)",
    buildingOutline = "rgba(31,82,82,0.8)",
    pointBarrier = "#99f",
    adminBoundary = "#e72",
    railway = "#96c",
    aeroway = "#559",
    path = "#547",
    road = "#559",
    roadOutline = "#547",
    pedestrian = "#554e7e",
    motorway = "#669",
    motorwayOutline = "#99f",
    text = "#ccf",
    textOutline = "#2e2e48",
    textWater = "#2e2e48",
    textWaterOutline = "#ccf",
    privateOverlay = "rgba(68,68,68,0.75)",
    hillshadeLight = "hsl(240, 30%, 50%)",
    hillshadeShadow = "hsl(240, 80%, 0%)",
    onewayArrow = "#ccf"
)

fun main(args: Array<String>) {
    val argMap = args.mapNotNull {
        val arg = it.split('=')
        if (arg.size != 2) null
        else arg[0] to arg[1]
    }.associate { it }

    val accessToken = argMap["access_token"] ?: ""
    val languages = argMap["languages"]?.split(",") ?: emptyList()

    File("streetcomplete.json").writeText(createStyle(
        name = "StreetComplete",
        accessToken = accessToken,
        languages = languages,
        colors = themeLight
    ))

    File("streetcomplete-night.json").writeText(createStyle(
        name = "StreetComplete-Night",
        accessToken = accessToken,
        languages = languages,
        colors = themeNight
    ))
}

fun createStyle(name: String, accessToken: String, languages: List<String>, colors: Colors): String {

    val pathWidth = listOf(14 to 0.5, 24 to 384.0)  // ~1m

    fun coalesceName() =
        "[" +
        listOf(
            "\"coalesce\"",
            *languages.map { "[\"get\", \"name_$it\"]" }.toTypedArray(),
            "[\"get\", \"name\"]"
        ).joinToString() +
        "]"

    val defaultTextStyle = Text(
        text = coalesceName(),
        size = byZoom(1, 13, 24, 64),
        fonts = listOf("Roboto Regular", "Noto Regular"),
        color = colors.text,
        outlineColor = colors.textOutline,
        outlineWidth =  2.5,
        padding = 12,
        sortKey = "[\"get\", \"scalerank\"]"
    )

    val waterTextStyle = defaultTextStyle.copy(
        color = colors.textWater,
        outlineColor = colors.textWaterOutline
    )

    val rivers = Waterway("rivers",
        filters = listOf(tagIn("class", "river", "canal")),
        color = colors.water,
        width = listOf(10 to 1.0, 17 to 6.0, 24 to 512.0),
        minZoom = 10.0
    )

    val streams = Waterway("streams",
        filters = listOf(tagIn("class", "stream", "ditch", "drain")),
        color = colors.water,
        width = listOf(16 to 1.0, 24 to 256.0),
        minZoom = 10.0
    )

    val paths = Road("paths",
        filters = listOf(tagIs("class", "path")),
        color = colors.path,
        colorOutline = colors.path,
        width = pathWidth,
        minZoom = 15.0
    )
    val pedestrian = Road("pedestrian",
        filters = listOf(tagIs("class", "street_limited"), tagIs("type", "pedestrian")),
        color = colors.pedestrian,
        colorOutline = colors.roadOutline,
        width = listOf(13 to 1.5, 24 to 1024.0), // ~6m
        minZoom = 14.0
    )
    val serviceRoads = Road("roads-service",
        filters = listOf(tagIn("class", "service", "driveway")),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(13 to 0.5, 24 to 768.0), // ~4m
        minZoom = 14.0
    )
    val minorRoads = Road("roads-minor",
        filters = listOf(tagIn("class", "street", "street_limited"), tagIsNot("type", "pedestrian")),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(11 to 0.5, 13 to 1.5, 24 to 1024.0), // ~6m
        minZoom = 12.0
    )
    val majorRoads = Road("roads-major",
        filters = listOf(tagIs("class", "main")),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(9 to 1.0, 13 to 4.5, 24 to 1536.0), // ~8m
        minZoom = 5.0,
    )
    val motorways = Road("motorways",
        filters = listOf(tagIs("class", "motorway")),
        color = colors.motorway,
        colorOutline = colors.motorwayOutline,
        width = listOf(8 to 1.0, 13 to 8.0, 24 to 2048.0), // ~12m
        minZoom = 5.0,
    )
    val motorwayLinks = Road("motorway-links",
        filters = listOf(tagIs("class", "motorway_link")),
        color = colors.motorway,
        colorOutline = colors.motorwayOutline,
        width = listOf(11 to 1.0, 13 to 1.5, 24 to 1024.0), // ~6m
    )

    val roads = listOf(pedestrian, serviceRoads, minorRoads, majorRoads, motorways, motorwayLinks)

    fun stepsOverlayLayer(structure: Structure) = Layer(
        id = listOfNotNull("steps", structure.id).joinToString("-"),
        src = "road",
        filter = listOf(tagIn("class", "path"), tagIn("type", "steps"), isLines, structure.filter),
        paint = Line(
            color = colors.pedestrian,
            width = byZoom(pathWidth.map { (z, w) -> z to w * 0.7 }),
            opacity = if (structure == Structure.Tunnel) "0.25" else null,
            dashes = "[0.6, 0.4]"
        )
    )

    fun railwayLayer(structure: Structure) = Layer(
        id = listOfNotNull("railways", structure.id).joinToString("-"),
        src = "road",
        filter = listOf(tagIn("class", "major_rail", "minor_rail"), isLines, structure.filter),
        paint = Line(
            color = colors.railway,
            // at zoom 17, the line spits up into two lines, to mimic the two tracks of a railway
            width = byZoom(12, 0.75, 13, 2.0, 16.999, 4, 17, 2, 24, 128),
            gapWidth = byZoom(12, 0, 17, 0, 24, 256),
            join = "round",
            opacity = byZoom(12, 0, 13, 1)
        )
    )

    fun pedestrianAreaLayer(structure: Structure) = Layer(
        id = listOfNotNull("pedestrian-areas", structure.id).joinToString("-"),
        src = "road",
        filter = listOf(tagIn("class", "path", "street_limited"), isPolygon, structure.filter),
        minZoom = 15.0,
        paint = Fill(
            color = colors.pedestrian,
            opacity = byZoom(15, 0, 16, 1),
        )
    )

    fun pedestrianAreaCasingLayer(structure: Structure) = Layer(
        id = listOfNotNull("pedestrian-areas-casing", structure.id).joinToString("-"),
        src = "road",
        filter = listOf(tagIn("class", "path", "street_limited"), isPolygon, structure.filter),
        minZoom = 16.0,
        paint = Line(
            color = colors.path,
            width = byZoom(16, 1, 24, 128),
            offset = byZoom(16, -0.5, 24, -64),
            opacity = byZoom(16, 0, 17, 1),
            dashes = if (structure == Structure.Tunnel) "[4, 4]" else null,
        )
    )



    fun allRoadLayers(structure: Structure) = listOfNotNull(
        // for roads, first draw the casing (= outline) of all roads
        *roads.map { it.toCasingLayer(structure) }.toTypedArray(),
        // pedestrian area tunnels are not drawn
        if (structure != Structure.Tunnel) pedestrianAreaCasingLayer(structure) else null,

        // , then draw the road color...
        paths.toLayer(structure), // paths do not have a casing
        stepsOverlayLayer(structure),
        if (structure != Structure.Tunnel)  pedestrianAreaLayer(structure) else null,
        *roads.map { it.toLayer(structure) }.toTypedArray(),
        // pedestrian area tunnels are not drawn

        paths.toLayerPrivateOverlay(structure, colors.privateOverlay),
        serviceRoads.toLayerPrivateOverlay(structure, colors.privateOverlay),

        // railway tunnels are not drawn
        if (structure != Structure.Tunnel) railwayLayer(structure) else null,
    )

    val layers = listOf<Layer>(

        Layer("landuse-town",
            src = "landuse",
            filter = listOf(
                tagNotIn("class", "pitch", "park", "grass", "cemetery", "wood", "scrub", "national_park")
            ),
            minZoom = 11.0,
            paint = Fill(color = colors.town, opacity = byZoom(11, 0, 12, 1))
        ),
        Layer("landuse-green",
            src = "landuse",
            filter = listOf(tagIn("class", "pitch", "park", "grass", "cemetery")),
            minZoom = 5.0,
            paint = Fill(color = colors.green, opacity = byZoom(5, 0, 6, 1))
        ),
        Layer("landuse-pitch-outline",
            src = "landuse",
            filter = listOf(tagIs("class", "pitch")),
            minZoom = 16.0,
            paint = Line(
                color = colors.earth,
                width = byZoom(16, 1, 24, 128),
                offset = byZoom(16, 0.5, 24, 64)
            )
        ),
        Layer("landuse-forest",
            src = "landuse",
            filter = listOf(tagIn("class", "wood", "scrub")),
            minZoom = 5.0,
            paint = Fill(color = colors.forest, opacity = byZoom(5, 0, 6, 1))
        ),

        *(1..2).map { i ->
            Layer("hillshade-highlight-$i",
                src = "hillshade",
                filter = listOf(tagIs("highlight", i)),
                maxZoom = 16.0,
                paint = Fill(
                    color = colors.hillshadeLight,
                    antialias = false,
                    opacity = byZoom(12, 0.12, 16, 0)
                )
            )
        }.toTypedArray(),

        *(1..4).map { i ->
            Layer("hillshade-shadow-$i",
                src = "hillshade",
                filter = listOf(tagIs("shadow", i)),
                maxZoom = 16.0,
                paint = Fill(
                    color = colors.hillshadeShadow,
                    antialias = false,
                    opacity = byZoom(12, 0.05, 16, 0)
                )
            )
        }.toTypedArray(),

        Layer("water-areas",
            src = "water",
            filter = listOf(Structure.None.filter),
            paint = Fill(colors.water)
        ),
        Layer("water-shore-lines",
            src = "water",
            filter = listOf(Structure.None.filter),
            minZoom = 15.0,
            paint = Line(
                color = colors.waterShore,
                width = byZoom(15, 1, 18, 4, 24, 256),
                offset = byZoom(15, 1, 18, 4, 24, 256),
                opacity = byZoom(15, 0, 18, 1),
                miterLimit = 6,
            )
        ),
        rivers.toLayer(Structure.None),
        streams.toLayer(Structure.None),

        Layer("aeroways",
            src = "aeroway",
            filter = listOf(isLines),
            paint = Line(
                color = colors.aeroway,
                width = byZoom(10, 1, 24, 8192),
                join = "round"
            )
        ),

        Layer("buildings",
            src = "building",
            minZoom = 15.0,
            paint = Fill(color = colors.building, opacity = byZoom(15, 0, 16, 1))
        ),

        Layer("buildings-outline",
            src = "building",
            minZoom = 15.5,
            paint = Line(
                color = colors.buildingOutline,
                width = byZoom(16, 1, 24, 128),
                opacity = byZoom(15.5, 0, 16.0, 1)
            )
        ),

        *allRoadLayers(Structure.Tunnel).toTypedArray(),

        *allRoadLayers(Structure.None).toTypedArray(),

        Layer("barriers-large",
            src = "structure",
            filter = listOf(tagIn("type", "city_wall", "dam", "cliff")),
            minZoom = 16.0,
            paint = Line(width = byZoom(16, 4, 24, 768), color = colors.buildingOutline)
        ),
        Layer("barriers-wall",
            src = "structure",
            filter = listOf(tagIs("class", "fence"), tagIsNot("type", "city_wall")),
            minZoom = 16.0,
            paint = Line(width = byZoom(16, 1, 24, 256), color = colors.buildingOutline)
        ),
        Layer("barriers-hedges",
            src = "structure",
            filter = listOf(tagIs("class", "hedge")),
            minZoom = 16.0,
            paint = Line(width = byZoom(16, 1, 24, 512), color = colors.forest)
        ),

        Layer("point-barriers",
            src = "structure",
            filter = listOf(isPoint),
            minZoom = 17.0,
            paint = Circle(color = colors.pointBarrier, radius = byZoom(17, 2, 24, 256))
        ),

        Layer("bridge-areas",
            src = "structure",
            filter = listOf(isPolygon, tagIs("class", "bridge")),
            paint = Fill(color = colors.building, opacity = "0.8")
        ),
        Layer("bridge-lines",
            src = "structure",
            filter = listOf(isLines, tagIs("class", "bridge")),
            paint = Line(color = colors.building, width = byZoom(16, 4, 24, 512), opacity = "0.8")
        ),

        Layer("water-areas-bridge",
            src = "water",
            filter = listOf(Structure.Bridge.filter),
            paint = Fill(colors.water)
        ),
        rivers.toLayer(Structure.Bridge),
        streams.toLayer(Structure.Bridge),

        *allRoadLayers(Structure.Bridge).toTypedArray(),

        Layer("oneway-arrows",
            src = "road",
            filter = listOf(isLines, tagIs("oneway", true)),
            minZoom = 16.0,
            paint = Symbol(
                image = "oneway",
                color = colors.onewayArrow,
                padding = 5,
                placement = "line",
                spacing = 200
            )
        ),

        Layer("boundaries",
            src = "admin",
            filter = listOf(tagIs("admin_level", 2), tagIsNot("maritime", true)),
            paint = Line(color = colors.adminBoundary, width = "1", dashes = "[1, 2]")
        ),

        Layer("labels-country",
            src = "place_label",
            filter = listOf(tagIs("class", "country")),
            paint = defaultTextStyle.copy(fonts = listOf("Roboto Bold", "Noto Bold"))
        ),

        Layer("labels-localities",
            src = "place_label",
            filter = listOf(tagIs("class", "locality")),
            paint = defaultTextStyle
        ),

        Layer("labels-housenumbers",
            src = "housenum_label",
            minZoom = 19.0,
            paint = defaultTextStyle.copy(text = "[\"get\", \"house_num\"]")
        ),

        Layer("labels-road",
            src = "road",
            minZoom = 14.0,
            filter = listOf(isLines),
            paint = defaultTextStyle.copy(wrap = 25, placement = "line-center")
        ),

        Layer("labels-rivers",
            src = "waterway",
            minZoom = 14.0,
            filter = listOf(
                tagIsNot("structure", "tunnel"),
                tagIn("class", "river", "canal")
            ),
            paint = waterTextStyle.copy(placement = "line-center")
        ),

        Layer("labels-streams",
            src = "waterway",
            minZoom = 16.0,
            filter = listOf(
                tagIsNot("structure", "tunnel"),
                tagIn("class", "stream", "ditch", "drain")
            ),
            paint = waterTextStyle.copy(placement = "line-center")
        ),

        /*
        // I don't know, kind of does not look good. Maybe it would look better if roofs were rendered?

        Layer("buildings-extrude",
            src = "building",
            filter = listOf(tagIs("extrude", true)),
            minZoom = 15.0,
            maxZoom = 19.0,
            paint = FillExtrusion(
                color = colors.building,
                base = """["get", "min_height"]""",
                height = """["get", "height"]""",
                opacity = byZoom(15, 0, 16, 0.8, 18, 0.8, 19, 0),
            )
        ),

         */
    )

    return """{
  "version": 8,
  "name": "$name",
  "sources": {
    "jawg-streets": {
      "type": "vector",
      "tiles": ["https://tile.jawg.io/streets-v2+hillshade-v1/{z}/{x}/{y}.pbf?access-token=$accessToken"],
      "attribution": "<a href='https://www.openstreetmap.org/copyright' title='OpenStreetMap is open data licensed under ODbL' target='_blank' class='osm-attrib'>&copy; OSM contributors</a> | <a href='https://jawg.io?utm_medium=map&utm_source=attribution' title='Tiles Courtesy of Jawg Maps' target='_blank' class='jawg-attrib'>&copy; <b>Jawg</b>Maps</a>",
      "maxzoom": 16
    }
  },
  "transition": { "duration": 300, "delay": 0 },
  "light": { "intensity": 0.2 },
  "glyphs": "https://api.jawg.io/glyphs/{fontstack}/{range}.pbf",
  "layers": [
    { "id": "background", "type": "background", "paint": {"background-color": "${colors.earth}"}},
    ${layers.joinToString(",\n") { it.toJson() }}
  ]
}
"""
}

data class Waterway(
    val id: String,
    val filters: List<String>,
    val color: String,
    val width: List<Pair<Number, Double>>,
    val minZoom: Double? = null,
)

fun Waterway.toLayer(structure: Structure) = Layer(
    id = listOfNotNull(id, structure.id).joinToString("-"),
    src = "waterway",
    filter = filters + listOf(isLines, structure.filter),
    minZoom = minZoom,
    paint = Line(
        color = color,
        width = byZoom(width.map { (z, w) -> z to w }),
        join = "round",
        cap = "round",
    )
)

data class Road(
    val id: String,
    val filters: List<String>,
    val color: String,
    val colorOutline: String,
    val dashes: String? = null,
    val width: List<Pair<Number, Double>>,
    val minZoom: Double? = null,
)

fun Road.toLayer(structure: Structure) = Layer(
    id = listOfNotNull(id, structure.id).joinToString("-"),
    src = "road",
    filter = filters + listOf(isLines, structure.filter),
    paint = Line(
        color = color,
        width = byZoom(width.map { (z, w) -> z to w }),
        join = "round",
        cap = "round",
        dashes = dashes,
        opacity = when {
            structure == Structure.Tunnel -> "0.25"
            minZoom != null -> byZoom(minZoom, 0, minZoom+1, 1)
            else -> null
        }
    )
)

fun Road.toCasingLayer(structure: Structure) = Layer(
    id = listOfNotNull(id, structure.id, "casing").joinToString("-"),
    src = "road",
    filter = filters + listOf(isLines, structure.filter),
    minZoom = 15.5,
    paint = Line(
        color = colorOutline,
        width = byZoom(16, 1, 24, 128),
        join = "round",
        opacity = byZoom(15.0, 0, 16, 1),
        // cap must not be round for bridges so that the casing is not drawn on top of normal roads
        cap = if (structure == Structure.None) "round" else "butt",
        dashes = if (structure == Structure.Tunnel) "[4, 4]" else null,
        gapWidth = byZoom(width.map { (z, w) -> z to w })
    )
)

fun Road.toLayerPrivateOverlay(structure: Structure, privateColor: String) = Layer(
    id = listOfNotNull(id, structure.id, "private").joinToString("-"),
    src = "road",
    filter = filters + listOf(
        isLines,
        tagIn("access", "no", "private", "destination", "customers", "delivery", "agricultural", "forestry", "emergency"),
        structure.filter
    ),
    paint = Line(
        color = privateColor,
        width = byZoom(width.map { (z, w) -> z to w * 0.5 }),
        join = "round",
        cap = "round",
        dashes = "[1, 2]",
    )
)

enum class Structure { Bridge, Tunnel, None }

val Structure.filter get() = when (this) {
    Structure.Bridge -> tagIs("structure", "bridge")
    Structure.Tunnel -> tagIs("structure", "tunnel")
    Structure.None -> tagNotIn("structure", "bridge", "tunnel")
}

val Structure.id get() = when (this) {
  Structure.Bridge -> "bridge"
  Structure.Tunnel -> "tunnel"
  Structure.None -> null
}

data class Colors(
    val earth: String,
    val water: String,
    val waterShore: String,
    val green: String,
    val forest: String,
    val town: String,
    val building: String,
    val buildingOutline: String,
    val pointBarrier: String,
    val adminBoundary: String,
    val railway: String,
    val aeroway: String,
    val path: String,
    val road: String,
    val roadOutline: String,
    val pedestrian: String,
    val motorway: String,
    val motorwayOutline: String,
    val text: String,
    val textOutline: String,
    val textWater: String,
    val textWaterOutline: String,
    val privateOverlay: String,
    val hillshadeLight: String,
    val hillshadeShadow: String,
    val onewayArrow: String,
)
