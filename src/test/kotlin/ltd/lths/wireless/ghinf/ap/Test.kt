package ltd.lths.wireless.ghinf.ap

import ltd.lths.wireless.ghinf.ap.other.CEE
import ltd.lths.wireless.ghinf.ap.util.SSID
import java.text.SimpleDateFormat
import java.util.*


/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.Test
 *
 * @author Score2
 * @since 2022/04/08 22:17
 */
object Test {

    @JvmStatic
    fun main(args: Array<String>) {
        println("距高考还有 ${CEE.lastDay}")

        val a = SSID("a")
        val a1 = SSID("a")
        val al = listOf(a)

        val b = SSID("b")
        val b1 = SSID("b")
        val bl = listOf(b)

        val all = listOf(a, b)
        val all1 = listOf(b, a)

        println(all.containsAll(all1))
    }

}