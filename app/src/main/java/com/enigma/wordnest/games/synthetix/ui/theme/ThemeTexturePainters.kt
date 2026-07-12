package com.enigma.wordnest.games.synthetix.ui.theme

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

fun DrawScope.drawTileTexture(type: TileTextureType, size: Float, isNew: Boolean) {
    when (type) {
        TileTextureType.WOOD_GRAIN    -> drawWoodGrainTile(size, isNew)
        TileTextureType.ICE_CRYSTAL   -> drawIceCrystalTile(size, isNew)
        TileTextureType.LAVA_ROCK     -> drawLavaRockTile(size, isNew)
        TileTextureType.CIRCUIT_TRACE -> drawCircuitTraceTile(size, isNew)
        TileTextureType.MARBLE_VEIN   -> drawMarbleVeinTile(size, isNew)
        TileTextureType.NEON_GLOW     -> drawNeonGlowTile(size, isNew)
        TileTextureType.BAMBOO        -> drawBambooTile(size, isNew)
    }
}

// ── Wood grain — realistic layered grain with knot hint ──────────────────────

private fun DrawScope.drawWoodGrainTile(sz: Float, isNew: Boolean) {
    // Rich warm base gradient (lighter top edge = light source from above)
    drawRect(
        Brush.verticalGradient(
            colors = listOf(
                Color(if (isNew) 0x18FFE0A0.toInt() else 0x22C8A060.toInt()),
                Color(0x00000000),
                Color(if (isNew) 0x12C88040.toInt() else 0x18804020.toInt())
            )
        ),
        size = Size(sz, sz)
    )

    // Fine grain lines — multiple sets at slightly different angles and spacings
    val grainColor1 = if (isNew) Color(0x44FF9900) else Color(0x338A5A20)
    val grainColor2 = if (isNew) Color(0x22FFB040) else Color(0x20603010)

    // Dense fine grain
    for (i in 0..8) {
        val y = sz * (0.08f + i * 0.105f)
        val wobble = sz * 0.018f
        val path = Path().apply {
            moveTo(0f, y + wobble * sin(i * 1.3f))
            cubicTo(
                sz * 0.30f, y + wobble * cos(i * 0.7f + 0.5f),
                sz * 0.65f, y - wobble * sin(i * 0.9f + 1.0f),
                sz,         y + wobble * cos(i * 0.6f)
            )
        }
        drawPath(path, color = grainColor1, style = Stroke(width = sz * 0.012f, cap = StrokeCap.Round))
    }

    // Medium grain overlay (fewer, darker)
    for (i in 0..3) {
        val y = sz * (0.15f + i * 0.23f)
        val wobble = sz * 0.03f
        val path = Path().apply {
            moveTo(0f, y)
            cubicTo(
                sz * 0.25f, y + wobble * sin(i.toFloat() + 0.3f),
                sz * 0.60f, y - wobble * cos(i.toFloat() + 0.8f),
                sz,         y + wobble * 0.5f * sin(i.toFloat())
            )
        }
        drawPath(path, color = grainColor2, style = Stroke(width = sz * 0.022f, cap = StrokeCap.Round))
    }

    // Top bevel highlight
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0x30FFFFFF), Color(0x00FFFFFF)),
            startY = 0f, endY = sz * 0.28f
        ),
        size = Size(sz, sz * 0.28f)
    )

    // Left edge highlight (simulating 3D raised tile)
    drawRect(
        Brush.horizontalGradient(
            colors = listOf(Color(0x22FFFFFF), Color(0x00FFFFFF)),
            startX = 0f, endX = sz * 0.20f
        ),
        size = Size(sz * 0.20f, sz)
    )

    // Bottom/right shadow for depth
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0x00000000), Color(0x28000000)),
            startY = sz * 0.7f, endY = sz
        ),
        size = Size(sz, sz)
    )
}

// ── Ice crystal — frosted glass with facets and shimmer ─────────────────────

private fun DrawScope.drawIceCrystalTile(sz: Float, isNew: Boolean) {
    val cx = sz / 2f; val cy = sz / 2f

    // Frosted base — radial gradient for depth
    drawRect(
        Brush.radialGradient(
            colors = listOf(
                Color(if (isNew) 0x40B8F8FF.toInt() else 0x30A8E8F8.toInt()),
                Color(if (isNew) 0x1060C8E8.toInt() else 0x0840A0C8.toInt())
            ),
            center = Offset(cx * 0.7f, cy * 0.6f),
            radius = sz * 0.9f
        ),
        size = Size(sz, sz)
    )

    // Hexagonal facet lines — the "crystal structure"
    val facetAlpha = if (isNew) 0xCC else 0x88
    val facetColor = Color((facetAlpha shl 24) or 0x90D8F8)

    for (i in 0..5) {
        val angle = (i * 60.0) * PI / 180.0
        val nextAngle = ((i + 1) * 60.0) * PI / 180.0
        // Spoke from center
        drawLine(
            color = facetColor,
            start = Offset(cx, cy),
            end   = Offset(cx + cos(angle).toFloat() * sz * 0.40f,
                cy + sin(angle).toFloat() * sz * 0.40f),
            strokeWidth = sz * 0.018f
        )
    }

    // Inner hexagon ring
    val hexRadius = sz * 0.28f
    for (i in 0..5) {
        val a1 = (i * 60.0) * PI / 180.0
        val a2 = ((i + 1) * 60.0) * PI / 180.0
        drawLine(
            color = facetColor.copy(alpha = 0.5f),
            start = Offset(cx + cos(a1).toFloat() * hexRadius, cy + sin(a1).toFloat() * hexRadius),
            end   = Offset(cx + cos(a2).toFloat() * hexRadius, cy + sin(a2).toFloat() * hexRadius),
            strokeWidth = sz * 0.012f
        )
    }

    // Diagonal frost crack lines
    val crackColor = Color(if (isNew) 0x5500EEFF.toInt() else 0x3360C8E0.toInt())
    drawLine(crackColor, Offset(sz*0.05f, sz*0.35f), Offset(sz*0.45f, sz*0.08f), sz*0.010f)
    drawLine(crackColor, Offset(sz*0.45f, sz*0.08f), Offset(sz*0.65f, sz*0.22f), sz*0.008f)
    drawLine(crackColor, Offset(sz*0.72f, sz*0.55f), Offset(sz*0.95f, sz*0.30f), sz*0.009f)

    // Bright sparkle at corner (ice glint)
    drawCircle(Color(0xDDFFFFFF), radius = sz * 0.055f, center = Offset(sz * 0.20f, sz * 0.18f))
    drawCircle(Color(0x88FFFFFF), radius = sz * 0.090f, center = Offset(sz * 0.20f, sz * 0.18f))

    // Top-left gloss plane (frosted glass reflection)
    drawRect(
        Brush.linearGradient(
            colors = listOf(Color(0x38FFFFFF), Color(0x00FFFFFF)),
            start = Offset(0f, 0f), end = Offset(sz * 0.6f, sz * 0.45f)
        ),
        size = Size(sz, sz)
    )

    // Subtle blue-white sheen on top edge
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0x30E8F8FF), Color(0x00FFFFFF)),
            startY = 0f, endY = sz * 0.22f
        ),
        size = Size(sz, sz * 0.22f)
    )
}

// ── Lava rock — glowing cracks on dark stone ─────────────────────────────────

private fun DrawScope.drawLavaRockTile(sz: Float, isNew: Boolean) {
    val crackColor = if (isNew) Color(0xEEFF6600) else Color(0xBBFF3300)
    val glowColor  = if (isNew) Color(0x77FF4400) else Color(0x44FF1800)
    val glowWide   = if (isNew) Color(0x44FF6600) else Color(0x22FF2200)

    // Stone texture — subtle diagonal scratches
    val scratchColor = Color(0x15FFFFFF)
    for (i in 0..4) {
        val x = sz * (0.1f + i * 0.2f)
        drawLine(scratchColor, Offset(x, 0f), Offset(x + sz * 0.15f, sz), sz * 0.008f)
    }

    // Glowing crack network — 3 layers (wide glow, medium glow, sharp crack)
    val cracks = listOf(
        listOf(Offset(sz*0.08f, sz*0.50f), Offset(sz*0.32f, sz*0.28f), Offset(sz*0.52f, sz*0.58f)),
        listOf(Offset(sz*0.52f, sz*0.58f), Offset(sz*0.72f, sz*0.38f), Offset(sz*0.94f, sz*0.52f)),
        listOf(Offset(sz*0.32f, sz*0.28f), Offset(sz*0.38f, sz*0.08f)),
        listOf(Offset(sz*0.52f, sz*0.58f), Offset(sz*0.48f, sz*0.88f)),
        listOf(Offset(sz*0.72f, sz*0.38f), Offset(sz*0.80f, sz*0.12f))
    )
    // Wide glow pass
    for (crack in cracks) for (j in 0 until crack.size - 1)
        drawLine(glowWide, crack[j], crack[j+1], strokeWidth = sz * 0.14f)
    // Medium glow
    for (crack in cracks) for (j in 0 until crack.size - 1)
        drawLine(glowColor, crack[j], crack[j+1], strokeWidth = sz * 0.07f)
    // Sharp bright crack
    for (crack in cracks) for (j in 0 until crack.size - 1)
        drawLine(crackColor, crack[j], crack[j+1], strokeWidth = sz * 0.020f)

    // Vignette — dark edges for stone look
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x55000000)),
            center = Offset(sz/2, sz/2), radius = sz * 0.65f
        ),
        size = Size(sz, sz)
    )
}

// ── Circuit trace ────────────────────────────────────────────────────────────

private fun DrawScope.drawCircuitTraceTile(sz: Float, isNew: Boolean) {
    val traceColor = if (isNew) Color(0xCC00FF88) else Color(0x8800FF88)
    val padColor   = if (isNew) Color(0xEE00FF88) else Color(0xAA00FF88)
    val glowColor  = if (isNew) Color(0x4400FF88) else Color(0x2200FF88)

    // Glow pass (thick transparent)
    val traces = listOf(
        Offset(0f, sz*0.30f) to Offset(sz*0.42f, sz*0.30f),
        Offset(sz*0.42f, sz*0.30f) to Offset(sz*0.42f, sz*0.60f),
        Offset(sz*0.42f, sz*0.60f) to Offset(sz, sz*0.60f),
        Offset(sz*0.70f, 0f) to Offset(sz*0.70f, sz*0.30f),
        Offset(sz*0.70f, sz*0.30f) to Offset(sz, sz*0.30f),
        Offset(0f, sz*0.70f) to Offset(sz*0.28f, sz*0.70f)
    )
    for ((a, b) in traces) drawLine(glowColor, a, b, strokeWidth = sz * 0.10f, cap = StrokeCap.Square)
    for ((a, b) in traces) drawLine(traceColor, a, b, strokeWidth = sz * 0.04f, cap = StrokeCap.Square)

    // Solder pads at junctions
    val pads = listOf(Offset(sz*0.42f,sz*0.30f), Offset(sz*0.42f,sz*0.60f), Offset(sz*0.70f,sz*0.30f))
    for (pad in pads) {
        drawCircle(glowColor.copy(alpha = 0.6f), radius = sz * 0.085f, center = pad)
        drawCircle(padColor, radius = sz * 0.055f, center = pad)
    }

    // Scan-line overlay
    val lineStep = sz * 0.11f; var y = 0f
    while (y < sz) { drawLine(Color(0x0800FF44), Offset(0f,y), Offset(sz,y), 1f); y += lineStep }
}

// ── Marble vein — realistic diagonal veining with shimmer ────────────────────

private fun DrawScope.drawMarbleVeinTile(sz: Float, isNew: Boolean) {
    // Base sheen — marble has a slight reflective gradient
    drawRect(
        Brush.linearGradient(
            colors = listOf(
                Color(if (isNew) 0x30FFE8CC.toInt() else 0x20F0E8E0.toInt()),
                Color(0x00000000),
                Color(if (isNew) 0x18C0A080.toInt() else 0x10D0C8C0.toInt())
            ),
            start = Offset(0f, 0f), end = Offset(sz, sz)
        ),
        size = Size(sz, sz)
    )

    // Main veins — darker, wider
    val veinDark  = if (isNew) Color(0x55B09060) else Color(0x40808080)
    val veinLight = if (isNew) Color(0x30F0D0A0) else Color(0x28C0B8B0)
    val shimmer   = if (isNew) Color(0x30FFE8CC) else Color(0x20FFFFFF)

    // Diagonal sweep veins
    for (i in 0..3) {
        val offset = sz * (i * 0.26f - 0.05f)
        val path = Path().apply {
            moveTo(-sz*0.05f + offset, 0f)
            cubicTo(
                sz*0.15f + offset, sz*0.25f,
                sz*0.05f + offset, sz*0.55f,
                sz*0.30f + offset, sz
            )
        }
        val strokeW = sz * if (i % 2 == 0) 0.030f else 0.018f
        drawPath(path, color = veinDark, style = Stroke(width = strokeW))
        // inner shimmer line
        drawPath(path, color = shimmer, style = Stroke(width = sz * 0.006f))
    }

    // Fine secondary veins (thinner, lighter)
    for (i in 0..2) {
        val offset = sz * (i * 0.30f + 0.08f)
        val path = Path().apply {
            moveTo(-sz*0.1f + offset, sz*0.1f)
            cubicTo(
                sz*0.10f + offset, sz*0.40f,
                sz*0.20f + offset, sz*0.65f,
                sz*0.15f + offset, sz
            )
        }
        drawPath(path, color = veinLight, style = Stroke(width = sz * 0.012f))
    }

    // Marble reflectance — angled highlight
    drawRect(
        Brush.linearGradient(
            colors  = listOf(Color(0x28FFFFFF), Color(0x00FFFFFF), Color(0x10FFFFFF)),
            start   = Offset(0f, 0f), end = Offset(sz, sz * 0.6f)
        ),
        size = Size(sz, sz)
    )

    // Top edge gloss
    drawRect(
        Brush.verticalGradient(
            colors = listOf(Color(0x20FFFFFF), Color(0x00FFFFFF)),
            startY = 0f, endY = sz * 0.18f
        ),
        size = Size(sz, sz * 0.18f)
    )
}

// ── Neon glow ────────────────────────────────────────────────────────────────

private fun DrawScope.drawNeonGlowTile(sz: Float, isNew: Boolean) {
    val glowColor  = if (isNew) Color(0xAAFF006E) else Color(0x779B00FF)
    val glowColor2 = if (isNew) Color(0x669B00FF) else Color(0x55FF006E)
    val glowColor3 = if (isNew) Color(0x33FF006E) else Color(0x339B00FF)

    // Outer rim glow (widest, most transparent)
    val inset = sz * 0.04f
    drawRoundRect(
        color = glowColor3,
        topLeft = Offset(inset, inset),
        size = Size(sz - inset*2, sz - inset*2),
        cornerRadius = CornerRadius(sz * 0.14f),
        style = Stroke(width = sz * 0.10f)
    )
    // Mid rim
    drawRoundRect(
        color = glowColor2,
        topLeft = Offset(inset*2f, inset*2f),
        size = Size(sz - inset*4, sz - inset*4),
        cornerRadius = CornerRadius(sz * 0.10f),
        style = Stroke(width = sz * 0.05f)
    )
    // Sharp bright rim
    drawRoundRect(
        color = glowColor,
        topLeft = Offset(inset*3f, inset*3f),
        size = Size(sz - inset*6, sz - inset*6),
        cornerRadius = CornerRadius(sz * 0.07f),
        style = Stroke(width = sz * 0.025f)
    )
    // Diagonal neon slash
    drawLine(glowColor.copy(alpha = 0.45f), Offset(sz*0.18f, sz*0.82f), Offset(sz*0.82f, sz*0.18f), sz*0.014f)
}

// ── Bamboo ──────────────────────────────────────────────────────────────────

private fun DrawScope.drawBambooTile(sz: Float, isNew: Boolean) {
    val ringColor   = if (isNew) Color(0x66FFD070) else Color(0x553A6820)
    val ringHighlight = if (isNew) Color(0x44FFEEAA) else Color(0x33A0CC60)
    val ringShadow  = if (isNew) Color(0x33A07820) else Color(0x221A3A08)

    // Horizontal segment rings
    val rings = listOf(0.22f, 0.44f, 0.66f, 0.88f)
    for (r in rings) {
        val y = sz * r
        // Shadow below ring
        drawLine(ringShadow, Offset(0f, y + sz*0.018f), Offset(sz, y + sz*0.018f), strokeWidth = sz * 0.028f)
        // Main ring band
        drawLine(ringColor, Offset(0f, y), Offset(sz, y), strokeWidth = sz * 0.048f)
        // Highlight above ring
        drawLine(ringHighlight, Offset(0f, y - sz*0.018f), Offset(sz, y - sz*0.018f), strokeWidth = sz * 0.012f)
    }
    // Vertical gloss — cylindrical sheen
    drawRect(
        Brush.horizontalGradient(
            colors = listOf(Color(0x08FFFFFF), Color(0x22FFFFFF), Color(0x08FFFFFF)),
            startX = 0f, endX = sz
        ),
        size = Size(sz, sz)
    )
    // Left edge highlight
    drawRect(
        Brush.horizontalGradient(
            colors = listOf(Color(0x25FFFFFF), Color(0x00FFFFFF)),
            startX = 0f, endX = sz * 0.25f
        ),
        size = Size(sz * 0.25f, sz)
    )
}

// ─────────────────────────────────────────────────────────────────────────────
//  Board background texture painters
// ─────────────────────────────────────────────────────────────────────────────

fun DrawScope.drawBoardTexture(type: BoardTextureType) {
    when (type) {
        BoardTextureType.DARK_WOOD    -> drawDarkWoodBoard()
        BoardTextureType.FROST        -> drawFrostBoard()
        BoardTextureType.MAGMA        -> drawMagmaBoard()
        BoardTextureType.PCB_GRID     -> drawPcbGridBoard()
        BoardTextureType.STONE_TILE   -> drawStoneTileBoard()
        BoardTextureType.NOIR_GRID    -> drawNoirGridBoard()
        BoardTextureType.LEAF_PATTERN -> drawLeafPatternBoard()
    }
}

private fun DrawScope.drawDarkWoodBoard() {
    val w = size.width; val h = size.height
    // Planks — wide horizontal bands with slight color variation
    for (i in 0..8) {
        val y = h * i / 8f
        val alpha = if (i % 2 == 0) 0x12 else 0x08
        drawRect(Color((alpha shl 24) or 0x2A1A0A), topLeft = Offset(0f, y), size = Size(w, h / 8f))
    }
    // Grain lines across planks
    for (i in 0..18) {
        val y = h * i / 18f
        drawLine(Color(0x0C2A1A0A), Offset(0f, y), Offset(w, y + w * 0.02f), strokeWidth = 1.5f)
    }
    // Diagonal sheen
    drawRect(
        Brush.linearGradient(
            listOf(Color(0x0AF2C97E), Color.Transparent, Color(0x06F2C97E)),
            start = Offset(0f, 0f), end = Offset(w, h)
        )
    )
}

private fun DrawScope.drawFrostBoard() {
    val w = size.width; val h = size.height; val cx = w/2; val cy = h/2
    drawRect(
        Brush.radialGradient(
            listOf(Color(0x2590D8F8), Color(0x1240A8D0), Color.Transparent),
            center = Offset(cx, cy), radius = maxOf(w, h) * 0.75f
        )
    )
    // Hexagonal frost cells
    val hex = 42f; val rows = (h / hex).toInt() + 2; val cols = (w / hex).toInt() + 2
    for (r in 0..rows) for (c in 0..cols) {
        val hx = c * hex * 1.73f + if (r % 2 == 0) 0f else hex * 0.87f
        val hy = r * hex * 1.5f
        drawCircle(Color(0x1490D4F0), radius = hex * 0.50f, center = Offset(hx, hy),
            style = Stroke(width = 0.8f))
    }
    // Diagonal frost streaks
    for (i in -3..5) {
        val startX = w * i * 0.22f
        drawLine(Color(0x08B8E8FF), Offset(startX, 0f), Offset(startX + h * 0.4f, h), 1.5f)
    }
}

private fun DrawScope.drawMagmaBoard() {
    val w = size.width; val h = size.height
    val rand = arrayOf(
        Offset(0f,h*0.3f) to Offset(w*0.3f,h*0.45f),
        Offset(w*0.3f,h*0.45f) to Offset(w*0.55f,h*0.2f),
        Offset(w*0.55f,h*0.2f) to Offset(w,h*0.4f),
        Offset(w*0.3f,h*0.45f) to Offset(w*0.4f,h),
        Offset(w*0.55f,h*0.2f) to Offset(w*0.7f,h*0.8f),
        Offset(0f,h*0.7f) to Offset(w*0.4f,h*0.65f)
    )
    for ((a,b) in rand) {
        drawLine(Color(0x22FF2000),a,b,strokeWidth=18f)
        drawLine(Color(0x33FF4500),a,b,strokeWidth=8f)
        drawLine(Color(0x22FF6600),a,b,strokeWidth=3f)
    }
}

private fun DrawScope.drawPcbGridBoard() {
    val w = size.width; val h = size.height; val step = 20f
    var x = 0f; while (x <= w) { drawLine(Color(0x1500FF88),Offset(x,0f),Offset(x,h),1f); x += step }
    var y = 0f; while (y <= h) { drawLine(Color(0x1500FF88),Offset(0f,y),Offset(w,y),1f); y += step }
    x = 0f; while (x <= w) { y = 0f; while (y <= h) {
        if (((x/step).toInt() + (y/step).toInt()) % 5 == 0) drawCircle(Color(0x2800FF88),2f,Offset(x,y))
        y+=step }; x+=step }
}

private fun DrawScope.drawStoneTileBoard() {
    val w = size.width; val h = size.height; val tile = 60f
    var x = 0f; while (x < w) { drawLine(Color(0x20AAAAAA),Offset(x,0f),Offset(x,h),1.5f); x+=tile }
    var y = 0f; while (y < h) { drawLine(Color(0x20AAAAAA),Offset(0f,y),Offset(w,y),1.5f); y+=tile }
    // Subtle bevel on each tile
    x = 0f; while (x < w) { y = 0f; while (y < h) {
        drawLine(Color(0x10FFFFFF), Offset(x, y), Offset(x+tile, y), 1f)
        drawLine(Color(0x10FFFFFF), Offset(x, y), Offset(x, y+tile), 1f)
        y+=tile }; x+=tile }
}

private fun DrawScope.drawNoirGridBoard() {
    val w = size.width; val h = size.height; val step = 16f
    var x = 0f; while (x <= w) { var y = 0f; while (y <= h) {
        drawCircle(Color(0x1E9B00FF),1.2f,Offset(x,y)); y+=step }; x+=step }
}

private fun DrawScope.drawLeafPatternBoard() {
    val w = size.width; val h = size.height
    for (i in -5..20) {
        val startY = i * h * 0.12f
        drawLine(Color(0x123A6820), Offset(0f,startY), Offset(w,startY + w*0.5f), 1.2f)
    }
    // Cross-diagonal (leaf vein pattern)
    for (i in -5..20) {
        val startX = i * w * 0.10f
        drawLine(Color(0x0C2A5A18), Offset(startX, 0f), Offset(startX + h*0.3f, h), 1f)
    }
}
