package com.enigma.wordnest.games.lexicon.model

object BoardConfig {
    val letterValues = mapOf(
        'A' to 1, 'B' to 3, 'C' to 3, 'D' to 2, 'E' to 1, 'F' to 4, 'G' to 2,
        'H' to 4, 'I' to 1, 'J' to 8, 'K' to 5, 'L' to 1, 'M' to 3, 'N' to 1,
        'O' to 1, 'P' to 3, 'Q' to 10, 'R' to 1, 'S' to 1, 'T' to 1, 'U' to 1,
        'V' to 4, 'W' to 4, 'X' to 8, 'Y' to 4, 'Z' to 10, '?' to 0
    )

    val letterDistribution = mapOf(
        'A' to 9, 'B' to 2, 'C' to 2, 'D' to 4, 'E' to 12, 'F' to 2, 'G' to 3,
        'H' to 2, 'I' to 9, 'J' to 1, 'K' to 1, 'L' to 4, 'M' to 2, 'N' to 6,
        'O' to 8, 'P' to 2, 'Q' to 1, 'R' to 6, 'S' to 4, 'T' to 6, 'U' to 4,
        'V' to 2, 'W' to 2, 'X' to 1, 'Y' to 2, 'Z' to 1, '?' to 2
    )

    fun buildPremiumMap(): Map<String, String> {
        val m = mutableMapOf<String, String>()
        listOf(0 to 0, 0 to 7, 0 to 14, 7 to 0, 7 to 14, 14 to 0, 14 to 7, 14 to 14)
            .forEach { m["${it.first},${it.second}"] = "TW" }
        listOf(1 to 1, 2 to 2, 3 to 3, 4 to 4, 10 to 10, 11 to 11, 12 to 12, 13 to 13,
            1 to 13, 2 to 12, 3 to 11, 4 to 10, 10 to 4, 11 to 3, 12 to 2, 13 to 1)
            .forEach { m["${it.first},${it.second}"] = "DW" }
        listOf(1 to 5, 1 to 9, 5 to 1, 5 to 5, 5 to 9, 5 to 13, 9 to 1, 9 to 5, 9 to 9, 9 to 13, 13 to 5, 13 to 9)
            .forEach { m["${it.first},${it.second}"] = "TL" }
        listOf(0 to 3, 0 to 11, 2 to 6, 2 to 8, 3 to 0, 3 to 7, 3 to 14, 6 to 2, 6 to 6, 6 to 8,
            6 to 12, 7 to 3, 7 to 11, 8 to 2, 8 to 6, 8 to 8, 8 to 12, 11 to 0, 11 to 7,
            11 to 14, 12 to 6, 12 to 8, 14 to 3, 14 to 11)
            .forEach { m["${it.first},${it.second}"] = "DL" }
        return m
    }
}
