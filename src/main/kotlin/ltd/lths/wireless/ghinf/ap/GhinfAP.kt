package ltd.lths.wireless.ghinf.ap

import com.google.gson.JsonParser
import ltd.lths.wireless.ghinf.ap.api.toIPv4
import ltd.lths.wireless.ghinf.ap.util.IPv4
import ltd.lths.wireless.ghinf.ap.util.SSID
import org.apache.http.HttpHost
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.util.EntityUtils
import org.jsoup.Jsoup

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.GhinfAP
 *
 * @author Score2
 * @since 2022/04/02 0:28
 */
class GhinfAP private constructor(
    val ipv4: IPv4,
    val port: Int = 80,
    val password: String = "admin"
    ) {


    var cookie: String? = null

    val host get() = "$ipv4:$port"
    val deriveName: String by lazy {
        val get = HttpGet("http://$host/ac/")
        get.setHeader("Cookie", cookie)

        val response = client.execute(get)

        val doc = Jsoup.parse(EntityUtils.toString(response.entity))
        doc.getElementsByClass("am-form-group").forEach {
            it.getElementsByClass("am-u-md-3 am-u-xs-12 am-form-label").find { element -> element.text().toString() == "设备名称" }
                ?: return@forEach

            return@lazy it.getElementsByClass("am-form-field tpl-form-no-bg").first()!!.attr("value")
        }

        return@lazy "获取失败"
    }

    var ssids: MutableList<SSID>
        get() {
            val get = HttpGet("http://$host/multissid/")
            get.setHeader("Cookie", cookie)

            val response = client.execute(get)
            val doc = Jsoup.parse(EntityUtils.toString(response.entity))

            return doc.getElementsByClass("even").map {
                val td = it.getElementsByTag("td")
                val id = td[0].text()
                val pwd = td[2].text().let { if (it == "无密码") "" else it }
                val encryption = when (td[1].text()) {
                    "无加密" -> SSID.Encryption.NONE
                    "WPAPSK-TKIP" -> SSID.Encryption.WPA_TKIP
                    "WPAPSK2-AES" -> SSID.Encryption.WPA2_PSK2
                    "WPA2-Mixed" -> SSID.Encryption.WPA2_MIXED
                    else -> SSID.Encryption.UNKNOWN
                }
                val frequency = when (td[4].text()) {
                    "2.4G" -> SSID.Frequency.WLAN_2G
                    "5G" -> SSID.Frequency.WLAN_5G
                    else -> SSID.Frequency.WLAN_2G
                }
                val hide = td[3].text() == "Yes"
                val vlan = td[5].text().let { if (it == "默认VLAN") 0 else it.toInt() }
                val removeId = it.getElementsByClass("tpl-table-black-operation-del").first()!!.attr("href").substringAfterLast("&id=")

                SSID(id, pwd, encryption, frequency, hide, vlan, mapOf("removeid" to removeId))
            }.toMutableList()
        }
        set(value) {
            val removes = mutableListOf<SSID>()
            // 保留已存在的ssid
            ssids.reversed().forEach { ssid ->
                if (value.any { it == ssid }) {
                    return@forEach
                }
                removes.add(ssid)
            }
            removes.forEach {
                removeSsid(it)
            }

            value.forEach { ssid ->
                if (ssids.any { it == ssid }) {
                    return@forEach
                }
                addSsid(ssid)
            }
        }

    init {
        cookie = login()
    }

    fun addSsid(ssid: SSID) {
        val post = HttpPost("http://$host/multissid/")
        post.setHeader("Cookie", cookie)
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

        post.entity = StringEntity("mode=${ssid.frequency.mode}&ssid=${ssid.id}&encryption=${ssid.encryption.alia}&key=${ssid.password}&auth_server=&auth_port=&auth_secret=&hidden=${ssid.hide.compareTo(false)}&auth=0&res=${Math.random() * 1000}&op=add&vlan=${ssid.vlan}", Charsets.UTF_8)

        client.execute(post)
    }

    fun removeSsid(ssid: SSID) =
        removeSsid(ssid.property["removeid"]!!)

    fun removeSsid(removeId: String) {
        val get = HttpGet("http://$host/multissid/del?op=del&id=$removeId")
        get.setHeader("Cookie", cookie)

        client.execute(get)
    }

    fun confirmSsids() {
        val post = HttpPost("http://$host/multissid/confirm")
        post.setHeader("Cookie", cookie)
        post.setHeader("Accept", "application/json, text/javascript, */*; q=0.01")

        client.execute(post)
    }

    fun login(): String? {
        val post = HttpPost("http://$host/login")
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

        post.entity = StringEntity("username=admin&password=admin&lang=zh-CN", Charsets.UTF_8)

        val response = client.execute(post)

        if (!JsonParser.parseString(EntityUtils.toString(response.entity)).asJsonObject.get("success").asBoolean) {
            return null
        }

        return response.getLastHeader("Set-Cookie").value.substringBefore(";").also { cookie = it }
    }

    companion object {

        val client get() = HttpClients.custom()
            .setDefaultRequestConfig(
                RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .build()
            )
            .build()

        fun idGhinfAP(ipv4: IPv4, port: Int = 80): Boolean {
            kotlin.runCatching {
                val post = HttpPost("http://$ipv4:$port/login")
                post.config = RequestConfig.custom()
                    .setCookieSpec(CookieSpecs.STANDARD)
                    .setConnectionRequestTimeout(500)
                    .setConnectTimeout(50)
                    .setSocketTimeout(500)
                    .build()
                post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

                post.entity = StringEntity("username=admin&password=admin&lang=zh-CN", Charsets.UTF_8)

                val response = client.execute(post)
                JsonParser.parseString(EntityUtils.toString(response.entity)).asJsonObject.get("success").asBoolean

                return true
            }

            return false
        }

        fun of(host: String, port: Int = 80, password: String = "admin") =
            of(host.toIPv4, port, password)

        fun of(iPv4: IPv4, port: Int = 80, password: String = "admin"): GhinfAP? {
            val ghinfAP = GhinfAP(iPv4, port, password)

            ghinfAP.cookie ?: return null

            return ghinfAP
        }


    }
}