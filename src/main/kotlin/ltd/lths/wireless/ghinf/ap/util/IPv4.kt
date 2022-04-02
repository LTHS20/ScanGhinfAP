package ltd.lths.wireless.ghinf.ap.util

import ltd.lths.wireless.ghinf.ap.util.IPv4.Companion.flat
import kotlin.math.min

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.util.IPv4
 *
 * @author Score2
 * @since 2022/04/01 23:04
 */
data class IPv4(
    val a: Int,
    val b: Int,
    val c: Int,
    val d: Int,
) {
    constructor(ip: String): this(
        ip.split(".")[0].toInt(),
        ip.split(".")[1].toInt(),
        ip.split(".")[2].toInt(),
        ip.split(".")[3].toInt(),
    )

    override fun toString(): String {
        return "$a.$b.$c.$d"
    }

    companion object {

        val range = (0..255)

        fun List<Int>.flat() =
            mapNotNull { if (!range.contains(it)) null else it }

        fun Set<Int>.flat() =
            mapNotNull { if (!range.contains(it)) null else it }

        fun IntRange.flat() =
            (first..min(last, range.last))


        fun String.readSegment(): Set<Int> {
            if (this == "*") {
                return range.toSet()
            }
            return this.split("\\s*,\\s*".toRegex()).flatMap {
                if (it.contains("-")) {
                    val split = it.split("-")
                    if (split.size == 2)
                        (split[0].toInt()..split[1].toInt()).flat().toList()
                    else
                        (split[0].toInt()..range.last).flat().toList()
                } else {
                    listOf(it.toIntOrNull() ?: 0)
                }
            }.toSet()
        }

        fun from(ip: String): Set<IPv4> {
            val segments = ip.split(".").let {
                arrayOf(
                    it.getOrNull(0)?.readSegment() ?: setOf(0),
                    it.getOrNull(1)?.readSegment() ?: setOf(0),
                    it.getOrNull(2)?.readSegment() ?: setOf(0),
                    it.getOrNull(3)?.readSegment() ?: setOf(0),
                )
            }

            val ipv4s = mutableSetOf<IPv4>()

            segments[0].forEach { a -> segments[1].forEach { b -> segments[2].forEach { c -> segments[3].forEach { d ->
                ipv4s.add(IPv4(a, b, c, d))
            } } } }

            return ipv4s
        }

    }
}