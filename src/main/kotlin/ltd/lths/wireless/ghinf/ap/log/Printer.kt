package ltd.lths.wireless.ghinf.ap.log

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.log.Printter
 *
 * @author Score2
 * @since 2022/04/23 22:50
 */
object Printer {

    private var printing = false

    private const val weight = 80
    private const val reservedLength = 3
    private const val invalidInfo = "------"

    fun log(s: String) {
        printing = true
        println(s)
        printing = false
    }

    fun refreshThreads(vararg threadInfos: String?) {
        val maxLength = threadInfos.maxOf { it?.length ?: invalidInfo.length }
        val lineCoordinates = mutableListOf<Int>()
        repeat(weight / maxLength + reservedLength) {
            lineCoordinates.add(it * (maxLength + reservedLength))
        }

        var out = ""
        threadInfos.forEachIndexed { i, s ->
            lineCoordinates[i % lineCoordinates.size] // TODO
            out += ""
        }
    }

}