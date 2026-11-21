package dev.materii.gloom.ui.screen.editor

enum class DiffType { SAME, ADD, REMOVE }

data class DiffLine(val type: DiffType, val text: String)

private const val CONTEXT = 3
private const val MAX_LCS_CELLS = 4_000_000L

/**
 * Line diff between [old] and [new]: the changed region plus a few lines of context.
 * Returns an empty list when nothing changed.
 */
fun diffLines(old: String, new: String): List<DiffLine> {
    if (old == new) return emptyList()
    val a = old.lines()
    val b = new.lines()

    var start = 0
    while (start < a.size && start < b.size && a[start] == b[start]) start++
    var endA = a.size
    var endB = b.size
    while (endA > start && endB > start && a[endA - 1] == b[endB - 1]) {
        endA--
        endB--
    }

    val midA = a.subList(start, endA)
    val midB = b.subList(start, endB)
    val result = ArrayList<DiffLine>()

    for (i in maxOf(0, start - CONTEXT) until start) result.add(DiffLine(DiffType.SAME, a[i]))

    val n = midA.size
    val m = midB.size
    if (n.toLong() * m > MAX_LCS_CELLS) {
        // Too large for an exact diff: show the whole region as replaced.
        midA.forEach { result.add(DiffLine(DiffType.REMOVE, it)) }
        midB.forEach { result.add(DiffLine(DiffType.ADD, it)) }
    } else {
        val width = m + 1
        val lcs = IntArray((n + 1) * width)
        for (i in n - 1 downTo 0) {
            for (j in m - 1 downTo 0) {
                lcs[i * width + j] = if (midA[i] == midB[j]) {
                    lcs[(i + 1) * width + j + 1] + 1
                } else {
                    maxOf(lcs[(i + 1) * width + j], lcs[i * width + j + 1])
                }
            }
        }
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                midA[i] == midB[j] -> {
                    result.add(DiffLine(DiffType.SAME, midA[i]))
                    i++
                    j++
                }
                lcs[(i + 1) * width + j] >= lcs[i * width + j + 1] -> {
                    result.add(DiffLine(DiffType.REMOVE, midA[i]))
                    i++
                }
                else -> {
                    result.add(DiffLine(DiffType.ADD, midB[j]))
                    j++
                }
            }
        }
        while (i < n) result.add(DiffLine(DiffType.REMOVE, midA[i++]))
        while (j < m) result.add(DiffLine(DiffType.ADD, midB[j++]))
    }

    for (i in endA until minOf(a.size, endA + CONTEXT)) result.add(DiffLine(DiffType.SAME, a[i]))
    return result
}
