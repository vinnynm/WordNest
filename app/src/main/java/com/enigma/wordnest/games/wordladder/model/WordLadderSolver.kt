package com.enigma.wordnest.games.wordladder.model

import java.util.ArrayDeque

/**
 * WordLadderSolver — builds a same-length "differs by exactly one letter"
 * adjacency graph and answers shortest-path / puzzle-generation queries.
 *
 * The "differs by exactly one letter" check mirrors the comparison style used
 * by AbsurdleEngine.scorePattern (position-by-position matching), just
 * reduced to a boolean distance-1 test instead of a full G/Y/X pattern.
 */
object WordLadderSolver {

    /**
     * Builds a word -> neighbors map for all words of one length, using
     * wildcard buckets (e.g. "c*t" -> [cat, cot, cut]) so construction is
     * O(words * length) instead of O(words^2).
     */
    fun buildAdjacency(wordsOfLength: Set<String>): Map<String, List<String>> {
        val buckets = mutableMapOf<String, MutableList<String>>()
        for (word in wordsOfLength) {
            for (i in word.indices) {
                val key = word.substring(0, i) + '*' + word.substring(i + 1)
                buckets.getOrPut(key) { mutableListOf() }.add(word)
            }
        }
        val adjacency = mutableMapOf<String, MutableSet<String>>()
        for (group in buckets.values) {
            if (group.size < 2) continue
            for (i in group.indices) {
                for (j in i + 1 until group.size) {
                    adjacency.getOrPut(group[i]) { mutableSetOf() }.add(group[j])
                    adjacency.getOrPut(group[j]) { mutableSetOf() }.add(group[i])
                }
            }
        }
        return adjacency.mapValues { it.value.toList() }
    }

    /** True if two same-length words differ in exactly one position. */
    fun differsByOne(a: String, b: String): Boolean {
        if (a.length != b.length) return false
        var diffs = 0
        for (i in a.indices) if (a[i] != b[i]) { diffs++; if (diffs > 1) return false }
        return diffs == 1
    }

    /** BFS shortest path between two words in the adjacency graph. Returns null if unreachable. */
    fun shortestPath(start: String, target: String, adjacency: Map<String, List<String>>): List<String>? {
        if (start == target) return listOf(start)
        val visited = mutableSetOf(start)
        val prev = mutableMapOf<String, String>()
        val queue = ArrayDeque<String>()
        queue.add(start)
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            for (neighbor in adjacency[current].orEmpty()) {
                if (neighbor in visited) continue
                visited += neighbor
                if (current != null) {
                    prev[neighbor] = current
                }
                if (neighbor == target) {
                    val path = mutableListOf(neighbor)
                    var cur = neighbor
                    while (cur != start) {
                        cur = prev[cur]!!
                        path.add(cur)
                    }
                    return path.reversed()
                }
                queue.add(neighbor)
            }
        }
        return null
    }

    /**
     * Picks a random solvable (start, target) pair whose shortest path has
     * between [minSteps] and [maxSteps] steps (edges). Falls back to whatever
     * is available if the exact range can't be found within [maxAttempts].
     */
    fun generatePuzzle(
        adjacency: Map<String, List<String>>,
        minSteps: Int = 3,
        maxSteps: Int = 6,
        maxAttempts: Int = 400
    ): Triple<String, String, List<String>>? {
        val connectedWords = adjacency.keys.filter { adjacency[it]?.isNotEmpty() == true }
        if (connectedWords.size < 2) return null

        var bestFallback: Triple<String, String, List<String>>? = null

        repeat(maxAttempts) {
            val start = connectedWords.random()
            val distances = mutableMapOf(start to 0)
            val queue = ArrayDeque<String>()
            queue.add(start)
            while (queue.isNotEmpty()) {
                val current = queue.poll()
                val d = distances.getValue(current)
                if (d >= maxSteps) continue
                for (neighbor in adjacency[current].orEmpty()) {
                    if (neighbor in distances) continue
                    distances[neighbor] = d + 1
                    queue.add(neighbor)
                }
            }
            val candidates = distances.filterValues { it in minSteps..maxSteps }.keys.filter { it != start }
            if (candidates.isNotEmpty()) {
                val target = candidates.random()
                val path = shortestPath(start, target, adjacency) ?: return@repeat
                val result = Triple(start, target, path)
                if (bestFallback == null) bestFallback = result
                if (path.size - 1 in minSteps..maxSteps) return result
            }
        }
        return bestFallback
    }
}
