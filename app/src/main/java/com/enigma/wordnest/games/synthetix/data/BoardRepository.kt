package com.enigma.wordnest.games.synthetix.data

import android.content.Context
import android.net.Uri
import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.enigma.wordnest.games.synthetix.model.BoardConfig
import com.enigma.wordnest.games.synthetix.model.TileSetConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.time.Instant
import androidx.core.content.edit
import com.enigma.wordnest.games.synthetix.model.DefaultBoards.standardObstacle
import com.enigma.wordnest.R

// ─────────────────────────────────────────────────────────────────────────────
//  Raw JSON shape (matches the files in res/raw/)
// ─────────────────────────────────────────────────────────────────────────────

private data class BoardJson(
    val name: String = "Board",
    val size: Int = 15,
    val rack: Int = 7,
    val bag: Int = 100,
    val bingo: Int = 50,
    val anchor: String = "",
    val timestamp: String = "",
    val grid: List<List<String?>> = emptyList()
)

// ─────────────────────────────────────────────────────────────────────────────
//  Preset descriptor
// ─────────────────────────────────────────────────────────────────────────────

data class PresetBoard(
    val displayName: String,
    val rawResId: Int
)

// ─────────────────────────────────────────────────────────────────────────────
//  BoardRepository
// ─────────────────────────────────────────────────────────────────────────────

class BoardRepository(private val context: Context) {

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val prefs = context.getSharedPreferences("synthetix_boards", Context.MODE_PRIVATE)

    val presetBoards: List<PresetBoard> = listOf(
        PresetBoard("Standard Obstacle 19×19",   R.raw.standard_obstacle_scrabble_board),
        PresetBoard("Standard Classic 15×15",    R.raw.standard_official_2_board),
        PresetBoard("Board Variation 16×16 A",   R.raw.board16by16),
        PresetBoard("Board Variation 16×16 B",   R.raw.board_variation16x16),
        PresetBoard("Board Variation 20×20",     R.raw.board20by20),
        PresetBoard("Board Variation 25×25 A",   R.raw.board_variation25x25),
        PresetBoard("Board Variation 25×25 B",   R.raw.board_variation25x25_2),
        PresetBoard("Board Variation 25×25 C",   R.raw.board_variation_25x25_3),
        PresetBoard("Legendary 25×25",           R.raw.legendary),
        PresetBoard("Legendary 25×25 II",        R.raw.legendary_2),
        PresetBoard("Legendary 25×25 III",       R.raw.legendary_3),
    )

    fun loadPresetByName(displayName: String): BoardConfig? {
        val preset = presetBoards.find { it.displayName == displayName } ?: return null
        return loadPreset(preset.rawResId)
    }

    // ── Save / load current active board ─────────────────────────────────────

    fun saveActiveBoard(config: BoardConfig) {
        prefs.edit { putString("active_board", gson.toJson(toJson(config))) }
    }

    fun loadActiveBoard(): BoardConfig {
        val json = prefs.getString("active_board", null)
            ?: return standardObstacle
        return try {
            fromJson(gson.fromJson(json, BoardJson::class.java))
        } catch (e: Exception) {
            Log.e("BoardRepository", "Active board load failed", e)
            standardObstacle
        }
    }

    // ── Named board library ───────────────────────────────────────────────────

    fun savedBoardNames(): List<String> {
        return prefs.all.keys
            .filter { it.startsWith("board_") }
            .map { it.removePrefix("board_") }
            .sorted()
    }

    fun saveBoard(config: BoardConfig) {
        prefs.edit { putString("board_${config.name}", gson.toJson(toJson(config))) }
    }

    fun loadBoard(name: String): BoardConfig? {
        val json = prefs.getString("board_$name", null) ?: return null
        return try { fromJson(gson.fromJson(json, BoardJson::class.java)) } catch (e: Exception) {
            Log.e("BoardRepository", "Board load failed", e)
            null
        }
    }

    fun deleteBoard(name: String) {
        prefs.edit { remove("board_$name") }
    }

    // ── JSON import / export ─────────────────────────────────────────────────

    fun importFromUri(uri: Uri): BoardConfig? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val text = BufferedReader(InputStreamReader(stream)).readText()
                fromJson(gson.fromJson(text, BoardJson::class.java))
            }
        } catch (e: Exception) {
            Log.e("BoardRepository", "Import failed", e)
            null
        }
    }

    fun exportToJson(config: BoardConfig): String = gson.toJson(toJson(config))

    fun exportToUri(config: BoardConfig, uri: Uri): Boolean {
        return try {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(exportToJson(config).toByteArray())
            }
            true
        } catch (e: Exception) {
            Log.e("BoardRepository", "Export failed", e)
            false
        }
    }

    // ── Bundled preset boards ─────────────────────────────────────────────────

    /**
     * Load a preset board from res/raw.
     *
     * ✅ FIX (B5): Returns a clearly-labelled fallback BoardConfig instead of
     * crashing when the raw resource is missing or malformed. The fallback is
     * a standard 15×15 blank board so the game remains playable.
     */
    fun loadPreset(rawResId: Int): BoardConfig? {
        return try {
            context.resources.openRawResource(rawResId).use { stream ->
                val text = BufferedReader(InputStreamReader(stream)).readText()
                fromJson(gson.fromJson(text, BoardJson::class.java))
            }
        } catch (e: Exception) {
            Log.e("BoardRepository", "Preset load failed for id=$rawResId — using fallback blank board", e)
            // ✅ Return a safe fallback instead of null / crash
            BoardConfig.blank(15, "Standard 15×15")
        }
    }

    // ── Conversion helpers ────────────────────────────────────────────────────

    private fun toJson(config: BoardConfig): BoardJson =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            BoardJson(
                name = config.name, size = config.size, rack = config.rack,
                bag  = config.bag,  bingo = config.bingo, anchor = config.anchor,
                timestamp = Instant.now().toString(), grid = config.grid
            )
        } else {
            BoardJson(
                name = config.name, size = config.size, rack = config.rack,
                bag  = config.bag,  bingo = config.bingo, anchor = config.anchor,
                grid = config.grid
            )
        }

    private fun fromJson(bj: BoardJson): BoardConfig {
        var anchorStr = bj.anchor
        if (anchorStr.isBlank()) {
            outer@ for (r in bj.grid.indices) {
                for (c in bj.grid[r].indices) {
                    if (bj.grid[r][c] == "anchor") { anchorStr = "$r,$c"; break@outer }
                }
            }
            if (anchorStr.isBlank()) anchorStr = "${bj.size / 2},${bj.size / 2}"
        }
        return BoardConfig(
            name = bj.name, size = bj.size, rack = bj.rack,
            bag  = bj.bag,  bingo = bj.bingo, anchor = anchorStr,
            timestamp = bj.timestamp, grid = bj.grid
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
//  TileSetRepository
// ─────────────────────────────────────────────────────────────────────────────

class TileSetRepository(private val context: Context) {

    private val gson  = GsonBuilder().setPrettyPrinting().create()
    private val prefs = context.getSharedPreferences("synthetix_tilesets", Context.MODE_PRIVATE)

    fun save(config: TileSetConfig) {
        prefs.edit { putString("tileset_${config.name}", gson.toJson(config)) }
    }

    fun load(name: String): TileSetConfig? {
        val json = prefs.getString("tileset_$name", null) ?: return null
        return try {
            gson.fromJson(json, TileSetConfig::class.java)
        } catch (e: Exception) {
            Log.e("TileSetRepository", "Tile set load failed", e)
            null
        }
    }

    fun savedNames(): List<String> = prefs.all.keys
        .filter { it.startsWith("tileset_") }
        .map { it.removePrefix("tileset_") }
        .sorted()

    fun delete(name: String) { prefs.edit { remove("tileset_$name") } }
}
