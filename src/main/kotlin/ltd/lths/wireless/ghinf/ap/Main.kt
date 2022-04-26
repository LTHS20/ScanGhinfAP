package ltd.lths.wireless.ghinf.ap

import joptsimple.OptionParser
import ltd.lths.wireless.ghinf.ap.api.asSection
import ltd.lths.wireless.ghinf.ap.util.IPv4
import ltd.lths.wireless.ghinf.ap.util.SSID
import taboolib.common.TabooLibCommon
import taboolib.common.platform.PlatformFactory
import taboolib.common.platform.function.releaseResourceFile
import taboolib.module.configuration.Config
import taboolib.module.configuration.Configuration
import taboolib.platform.AppExecutor
import taboolib.platform.AppIO
import java.io.File
import java.util.concurrent.CompletableFuture
import kotlin.system.exitProcess

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
        val config = Configuration.loadFromFile(File("config.yml"))
        TabooLibCommon.testSetup()
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
                acceptsAll(listOf("skip", "skips"), "跳过带有指定名称的ap, 防止覆盖到危险区域")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("行政,huiyishi,会议室,总")
                    .describedAs("名称")
                acceptsAll(listOf("wifi", "wlan"), "给定 WIFI 名称及密码, 用于覆盖")
                    .withRequiredArg()
                    .ofType(String::class.java)
                    .defaultsTo("score2@github.com/755466879")
                    .describedAs("名称/密码")
                acceptsAll(listOf("config", "c"), "根据本地配置文件 config.yml 展开扫描")

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

        if (options.has("config")) {
            println("正在遍历ip段...")
            val ips = config.getStringList("ips").flatMap { IPv4.from(it) }
            println("ip已生成 ${ips.size} 个")
            val port = config.getInt("port", 80)
            val thread = config.getInt("thread", 12)
            val password = config.getString("password", "admin")!!
            val skips: List<String> = config.getStringList("skips")
            val ssids: List<SSID> = config.getList("ssids.cover")!!.map {
                val section = it.asSection()
                SSID(
                    section.getString("id")!!,
                    section.getString("password")!!,
                    SSID.Encryption.valueOf(section.getString("encryption")!!.uppercase()),
                    SSID.Frequency.valueOf(section.getString("frequency")!!.uppercase()),
                    section.getBoolean("hide"),
                    section.getInt("vlan")
                )
            }
            val removeSsids = config.getStringList("ssids.remove")
            println("从配置文件模式开始")

            start(ips, port, thread, password, skips, ssids, removeSsids)
        } else {
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
            start(ips, port, thread, password, skips, ssids)
        }


        exitProcess(0)
    }

    fun start(
        ips: List<IPv4> = listOf(IPv4("172.10.0.1")),
        port: Int = 80,
        thread: Int = 12,
        password: String = "admin",
        skips: List<String> = listOf(),
        ssids: List<SSID> = listOf(),
        removeSsids: List<String> = listOf()
    ) {
        val onlyScan = ssids.isEmpty() && removeSsids.isEmpty()

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
        println("开始扫描")
        if (!onlyScan) {
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
                it.removeAll { ssid -> removeSsids.any {
                    if (it.endsWith("*")) ssid.id.contains(it.removeSuffix("*"))
                    else it == ssid.id
                } }
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
                    if (!onlyScan) {
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