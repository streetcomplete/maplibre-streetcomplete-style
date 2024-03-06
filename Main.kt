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
    aerowayOutline = "#ca9",
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
    privateOverlay = "#f3dacd",
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
    aerowayOutline = "#547",
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
    privateOverlay = "#f3dacd",
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
            *languages.map { "[\"get\", \"name:$it\"]" }.toTypedArray(),
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
        padding = 12
    )

    val waterTextStyle = defaultTextStyle.copy(
        color = colors.textWater,
        outlineColor = colors.textWaterOutline
    )

    val rivers = Waterway("rivers",
        filters = listOf(tagIn("kind", "river", "canal")),
        color = colors.water,
        width = listOf(10 to 1.0, 17 to 6.0, 24 to 512.0),
        minZoom = 10.0
    )

    val streams = Waterway("streams",
        filters = listOf(tagIn("kind", "stream", "ditch", "drain")),
        color = colors.water,
        width = listOf(16 to 1.0, 24 to 256.0),
        minZoom = 10.0
    )

    val paths = Road("paths",
        filters = listOf(tagIn("kind", "footway", "path", "steps", "cycleway")),
        color = colors.path,
        colorOutline = colors.path,
        width = pathWidth,
        minZoom = 15.0
    )
    val pedestrian = Road("pedestrian",
        filters = listOf(tagIs("kind", "pedestrian")),
        color = colors.pedestrian,
        colorOutline = colors.roadOutline,
        width = listOf(13 to 1.5, 24 to 1024.0), // ~6m
        minZoom = 14.0
    )
    val serviceRoads = Road("roads-service",
        filters = listOf(tagIn("kind", "service", "track", "busway")),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(13 to 0.5, 24 to 768.0), // ~4m
        minZoom = 14.0
    )
    val minorRoads = Road("roads-minor",
        filters = listOf(tagIn("kind", "unclassified", "residential", "living_street"),),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(11 to 0.5, 13 to 1.5, 24 to 1024.0), // ~6m
        minZoom = 12.0
    )
    val majorRoadLinks = Road("roads-major-links",
        filters = listOf(
            tagIn("kind", "trunk", "primary", "secondary", "tertiary"),
            tagIs("link", true)
        ),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(11 to 0.5, 13 to 1.5, 24 to 1024.0), // ~6m
        minZoom = 12.0
    )
    val majorRoads = Road("roads-major",
        filters = listOf(
            tagIn("kind", "trunk", "primary", "secondary", "tertiary"),
            tagIsNot("link", true)
        ),
        color = colors.road,
        colorOutline = colors.roadOutline,
        width = listOf(9 to 1.0, 13 to 4.5, 24 to 1536.0), // ~8m
        minZoom = 5.0,
    )
    val motorways = Road("motorways",
        filters = listOf(tagIs("kind", "motorway"), tagIsNot("link", true)),
        color = colors.motorway,
        colorOutline = colors.motorwayOutline,
        width = listOf(8 to 1.0, 13 to 8.0, 24 to 2048.0), // ~12m
        minZoom = 5.0,
    )
    val motorwayLinks = Road("motorway-links",
        filters = listOf(tagIs("kind", "motorway"), tagIs("link", true)),
        color = colors.motorway,
        colorOutline = colors.motorwayOutline,
        width = listOf(11 to 1.0, 13 to 1.5, 24 to 1024.0), // ~6m
    )
    val aeroways = Road("aeroways",
        filters = listOf(tagIn("kind", "runway", "taxiway")),
        color = colors.aeroway,
        colorOutline = colors.aerowayOutline,
        width = listOf(10 to 1.0, 24 to 8192.0), // ~48m
    )

    val roads = listOf(
        pedestrian, serviceRoads, minorRoads, majorRoads, majorRoadLinks, motorways, motorwayLinks, aeroways
    )

    fun stepsOverlayLayer(structure: Structure) = Layer(
        id = listOfNotNull("steps", structure.id).joinToString("-"),
        src = "streets",
        filter = listOf(tagIn("kind", "steps")) + structure.filter,
        paint = Line(
            color = colors.pedestrian,
            width = byZoom(pathWidth.map { (z, w) -> z to w * 0.7 }),
            opacity = if (structure == Structure.Tunnel) "0.25" else null,
            dashes = "[0.6, 0.4]"
        )
    )

    val railwayLine = Line(
        color = colors.railway,
        // at zoom 17, the line spits up into two lines, to mimic the two tracks of a railway
        width = byZoom(12, 0.75, 13, 2.0, 16.999, 4, 17, 2, 24, 128),
        gapWidth = byZoom(12, 0, 17, 0, 24, 256),
        join = "round",
        opacity = byZoom(12, 0, 13, 1)
    )

    fun railwayLayer(structure: Structure) = Layer(
        id = listOfNotNull("railways", structure.id).joinToString("-"),
        src = "streets",
        filter = listOf(tagIn("kind",
            "light_rail", "monorail", "rail", "subway", "tram", "bus_guideway", "funicular", "narrow_gauge",
        )) + structure.filter,
        paint = railwayLine
    )

    fun pedestrianAreaLayer(structure: Structure) = Layer(
        id = listOfNotNull("pedestrian-areas", structure.id).joinToString("-"),
        src = "street_polygons",
        filter = listOf(tagIn("kind", "pedestrian")) + structure.filter,
        minZoom = 15.0,
        paint = Fill(
            color = colors.pedestrian,
            opacity = byZoom(15, 0, 16, 1),
        )
    )

    fun pedestrianAreaCasingLayer(structure: Structure) = Layer(
        id = listOfNotNull("pedestrian-areas-casing", structure.id).joinToString("-"),
        src = "street_polygons",
        filter = listOf(tagIn("kind", "pedestrian")) + structure.filter,
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

        // roads and pedestrian areas should be drawn on top of paths, as paths on
        // these are kind of "virtual", do only exist for connectivity
        paths.toLayer(structure), // paths do not have a casing
        stepsOverlayLayer(structure),
        if (structure != Structure.Tunnel)  pedestrianAreaLayer(structure) else null,
        *roads.map { it.toLayer(structure) }.toTypedArray(),
        // pedestrian area tunnels are not drawn

        paths.toLayerPrivateOverlay(structure, colors.privateOverlay),
        serviceRoads.toLayerPrivateOverlay(structure, colors.privateOverlay),

        // railway tunnels are not drawn
        // railways are drawn last because e.g. trams should appear on top of roads
        if (structure != Structure.Tunnel) railwayLayer(structure) else null,
    )

    val shoreLine = Line(
        color = colors.waterShore,
        width = byZoom(15, 1, 18, 4, 24, 256),
        offset = byZoom(15, 1, 18, 4, 24, 256),
        opacity = byZoom(15, 0, 18, 1),
        miterLimit = 6,
    )

    val layers = listOf<Layer>(

        Layer("landuse-town",
            src = "land",
            filter = listOf(tagIn("kind",
                // town
                "commercial", "residential", "retail",

                // industrial / asphalt-desert
                "brownfield", "farmyard", "garages", "industrial", "landfill", "quarry", "railway",
            )),
            minZoom = 11.0,
            paint = Fill(color = colors.town, opacity = byZoom(11, 0, 12, 1))
        ),
        Layer("landuse-sites",
            src = "sites",
            minZoom = 14.0,
            paint = Fill(color = colors.town, opacity = byZoom(14, 0, 15, 1))
        ),
        Layer("landuse-green",
            src = "land",
            filter = listOf(tagIn("kind",
                // city green
                "cemetery", "miniature_golf", "garden", "grass", "golf_course", "grave_yard",
                "greenfield", "park", "playground", "recreation_ground", "village_green",

                // farmland
                "allotments", "farmland", "greenhouse_horticulture", "plant_nursery", "vineyard",

                // natural
                "bog", "grassland", "heath", "marsh", "meadow", "string_bog", "wet_meadow",
            )),
            minZoom = 5.0,
            paint = Fill(color = colors.green, opacity = byZoom(5, 0, 6, 1))
        ),
        Layer("landuse-forest",
            src = "land",
            filter = listOf(tagIn("kind",
                "forest",
                "orchard", // are usually at least small trees
                "scrub",
                "swamp", // forest + water = swamp
            )),
            minZoom = 5.0,
            paint = Fill(color = colors.forest, opacity = byZoom(5, 0, 6, 1))
        ),
        // not rendered: shingle, scree, bare_rock, sand, beach,

        Layer("oceans",
            src = "ocean",
            paint = Fill(colors.water)
        ),
        Layer("water-areas",
            src = "water_polygons",
            filter = listOf(tagNotIn("kind", "glacier")) + Structure.None.filter,
            paint = Fill(colors.water)
        ),
        Layer("ocean-shore-lines",
            src = "ocean",
            minZoom = 15.0,
            paint = shoreLine
        ),
        Layer("water-shore-lines",
            src = "water_polygons",
            filter = listOf(tagNotIn("kind", "glacier")) + Structure.None.filter,
            minZoom = 15.0,
            paint = shoreLine
        ),
        rivers.toLayer(Structure.None),
        streams.toLayer(Structure.None),

        // TODO piers

        Layer("buildings",
            src = "buildings",
            minZoom = 15.0,
            paint = Fill(color = colors.building, opacity = byZoom(15, 0, 16, 1))
        ),

        Layer("buildings-outline",
            src = "buildings",
            minZoom = 15.5,
            paint = Line(
                color = colors.buildingOutline,
                width = byZoom(16, 1, 24, 128),
                opacity = byZoom(15.5, 0, 16.0, 1)
            )
        ),
        // TODO dam_polygons

        *allRoadLayers(Structure.Tunnel).toTypedArray(),

        *allRoadLayers(Structure.None).toTypedArray(),

        Layer("dam-lines",
            src = "dam_lines",
            minZoom = 16.0,
            paint = Line(width = byZoom(16, 4, 24, 768), color = colors.buildingOutline)
        ),
        // shortbread v1 doesn't have any "barrier"s. If it had, they would go here
        // (wall, fence, city_wall, cliffs, retaining walls, bollard ...)

        Layer("bridge-areas",
            src = "bridges",
            filter = listOf(isPolygon),
            paint = Fill(color = colors.building, opacity = "0.8")
        ),

        Layer("water-areas-bridge",
            src = "water_polygons",
            filter = listOf(tagNotIn("kind", "glacier")) + Structure.Bridge.filter,
            paint = Fill(colors.water)
        ),
        rivers.toLayer(Structure.Bridge),
        streams.toLayer(Structure.Bridge),

        *allRoadLayers(Structure.Bridge).toTypedArray(),

        Layer("aerialways",
            src = "aerialways",
            paint = railwayLine
        ),

        Layer("oneway-arrows",
            src = "streets",
            filter = listOf(tagIs("oneway", true)),
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
            src = "boundaries",
            filter = listOf(tagIs("admin_level", 2), tagIsNot("maritime", true)),
            paint = Line(color = colors.adminBoundary, width = "1", dashes = "[1, 2]")
        ),

        Layer("labels-country",
            src = "boundary_labels",
            filter = listOf(tagIs("admin_level", 2)),
            paint = defaultTextStyle.copy(fonts = listOf("Roboto Bold", "Noto Bold"))
        ),

        Layer("labels-localities",
            src = "place_labels",
            paint = defaultTextStyle
        ),

        Layer("labels-housenumbers",
            src = "addresses",
            minZoom = 19.0,
            paint = defaultTextStyle.copy(text =
                """["coalesce", ["get", "housenumber"], ["get", "housename"]]"""
            )
        ),

        Layer("labels-road",
            src = "street_labels",
            minZoom = 14.0,
            paint = defaultTextStyle.copy(wrap = 25, placement = "line-center")
        ),

        Layer("labels-road-areas",
            src = "streets_polygons_labels",
            minZoom = 14.0,
            paint = defaultTextStyle
        ),

        Layer("labels-rivers",
            src = "water_lines_labels",
            minZoom = 14.0,
            filter = listOf(
                tagIsNot("tunnel", true),
                tagIn("kind", "river", "canal")
            ),
            paint = waterTextStyle.copy(placement = "line-center")
        ),

        Layer("labels-streams",
            src = "water_lines_labels",
            minZoom = 16.0,
            filter = listOf(
                tagIsNot("tunnel", true),
                tagIn("kind", "stream", "ditch", "drain")
            ),
            paint = waterTextStyle.copy(placement = "line-center")
        ),

    )

    return """{
  "version": 8,
  "name": "$name",
  "sources": {
    "shortbread-v1": {
      "type": "vector",
      "url": "https://demo.tilekiln.xyz/shortbread_v1/tilejson.json",
      "attribution": "<a href='https://www.openstreetmap.org/copyright' title='OpenStreetMap is open data licensed under ODbL' target='_blank' class='osm-attrib'>&copy; OSM contributors</a>",
      "maxzoom": 16
    }
  },
  "transition": { "duration": 300, "delay": 0 },
  "light": { "intensity": 0.2 },
  "glyphs": "https://tiles.versatiles.org/assets/fonts/{fontstack}/{range}.pbf",
  "sprite": "https://tiles.versatiles.org/sprites/sprites",
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
    src = "water_lines",
    filter = filters + structure.filter,
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
    val width: List<Pair<Number, Double>>,
    val minZoom: Double? = null,
)

fun Road.toLayer(structure: Structure) = Layer(
    id = listOfNotNull(id, structure.id).joinToString("-"),
    src = "streets",
    filter = filters + structure.filter,
    paint = Line(
        color = color,
        width = byZoom(width.map { (z, w) -> z to w }),
        join = "round",
        cap = "round",
        opacity = when {
            structure == Structure.Tunnel -> "0.25"
            minZoom != null -> byZoom(minZoom, 0, minZoom+1, 1)
            else -> null
        }
    )
)

fun Road.toCasingLayer(structure: Structure) = Layer(
    id = listOfNotNull(id, structure.id, "casing").joinToString("-"),
    src = "streets",
    filter = filters + structure.filter,
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
    src = "streets",
    filter = filters + structure.filter + listOf(
        tagIn("access", "no", "private", "destination", "customers", "delivery", "agricultural", "forestry", "emergency"),
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

val Structure.filter: List<String> get() = when (this) {
    Structure.Bridge -> listOf(tagIs("bridge", true))
    Structure.Tunnel -> listOf(tagIs("tunnel", true))
    Structure.None -> listOf(tagIsNot("bridge", true), tagIsNot("tunnel", true))
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
    val aerowayOutline: String,
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
