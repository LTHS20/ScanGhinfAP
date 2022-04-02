package ltd.lths.wireless.ghinf.ap

import joptsimple.OptionParser
import ltd.lths.wireless.ghinf.ap.api.toIPv4
import ltd.lths.wireless.ghinf.ap.util.IPv4
import ltd.lths.wireless.ghinf.ap.util.SSID
import org.apache.http.client.protocol.ResponseProcessCookies
import java.net.InetAddress

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.Main
 *
 * @author Score2
 * @since 2022/04/01 22:35
 */
object Main {

    @JvmStatic
    fun main(args: Array<String>) {
        println("ScanGhinfAP 运行中...")
        val parser = OptionParser()
        parser.acceptsAll(listOf("?", "help"), "获取帮助")
        parser.acceptsAll(listOf("ip", "ips"), "给定ip范围<172.10-25.*.*>")
        parser.acceptsAll(listOf("password", "pwd"), "自定义管理员密码, 默认为 admin")
        parser.acceptsAll(listOf("scan"), "扫描指定ip范围的Ghinf信息")

        parser.acceptsAll(listOf("test"), "随意调试")

        val options = kotlin.runCatching { parser.parse(*args) }.getOrElse { test(); return }

        var ips: Set<IPv4> = setOf(IPv4("172.10.0.1"))
        var password = "admin"

        when {
            options.has("ip") -> {
                ips = IPv4.from(options.valueOf("ip").toString())
            }

            options.has("pwd") -> {
                password = options.valueOf("pwd").toString()
            }


            options.has("?") -> {
                parser.printHelpOn(System.out)
                return
            }
            options.has("test") -> {
                test()
                return
            }
            else -> {
                test()
                return
            }
        }
    }

    fun test() {
/*        File("ips.txt").printWriter().let { writer ->
            IPv4.from("172.10-25,30.*.*").forEach {
                writer.println(it.toString())
            }
        }*/
        GhinfAP.of("172.10.0.3", 6996)?.let {
            println(it.cookie)
            println(it.deriveName)
            println("SSID:")
            it.ssids = mutableListOf(
                SSID("iRongHuai-2G", "", SSID.Encryption.NONE, SSID.Frequency.WLAN_2G, false, 27),
                SSID("iRongHuai-5G", "", SSID.Encryption.NONE, SSID.Frequency.WLAN_5G, false, 27),
                SSID("score2@github.com", "17308930", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_2G, false, 0),
                SSID("score2@github.com", "17308930", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_5G, false, 0),
            )
            it.confirmSsids()

        }
    }

}