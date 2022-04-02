package ltd.lths.wireless.ghinf.ap

import com.google.gson.JsonParser
import ltd.lths.wireless.ghinf.ap.api.toIPv4
import ltd.lths.wireless.ghinf.ap.util.IPv4
import ltd.lths.wireless.ghinf.ap.util.SSID
import org.apache.http.client.config.CookieSpecs
import org.apache.http.client.config.RequestConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.HttpClients
import org.apache.http.message.BasicHeader
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

    val client = HttpClients.custom()
        .setDefaultRequestConfig(RequestConfig.custom()
        .setCookieSpec(CookieSpecs.STANDARD).build())
        .build()

    var cookie: String?

    val host get() = "$ipv4:$port"
    val deriveName: String get() {
        val post = HttpPost("http://$host/ac/")
        post.setHeader("Cookie", cookie)

        val response = client.execute(post)

        val doc = Jsoup.parse(EntityUtils.toString(response.entity))
        doc.getElementsByClass("am-form-group").forEach {
            it.getElementsByClass("am-u-md-3 am-u-xs-12 am-form-label").find { element -> element.text().toString() == "设备名称" }
                ?: return@forEach

            return it.getElementsByClass("am-form-field tpl-form-no-bg").first()!!.attr("value")
        }

        return "获取失败"
    }

    fun addSsid(ssid: SSID) {
        val post = HttpPost("http://$host/multissid/")
        post.setHeader("Cookie", cookie)
        post.setHeader("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")

        post.entity = StringEntity("mode=${ssid.frequency.mode}&ssid=&encryption=${ssid.encryption}&key=&auth_server=&auth_port=&auth_secret=&hidden=0&auth=0&res=619.9494729789706&op=add&vlan=0")

        val response = client.execute(post)

        val doc = Jsoup.parse(EntityUtils.toString(response.entity).also { println(it) })
    }

    val ssids: Set<SSID> get() {
        val post = HttpPost("http://$host/multissid/")
        post.setHeaders(arrayOf(
            BasicHeader("Host", "127.0.0.1:6997"),
            BasicHeader("Connection", "keep-alive"),
            BasicHeader("sec-ch-ua", "\" Not A;Brand\";v=\"99\", \"Chromium\";v=\"99\", \"Microsoft Edge\";v=\"99\""),
            BasicHeader("sec-ch-ua-mobile", "?0"),
            BasicHeader("sec-ch-ua-platform", "\"Windows\""),
            BasicHeader("Upgrade-Insecure-Requests", "1"),
            BasicHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/99.0.4844.74 Safari/537.36 Edg/99.0.1150.46"),
            BasicHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.9"),
            BasicHeader("Sec-Fetch-Site", "same-origin"),
            BasicHeader("Sec-Fetch-Mode", "navigate"),
            BasicHeader("Sec-Fetch-User", "?1"),
            BasicHeader("Sec-Fetch-Dest", "document"),
            BasicHeader("Referer", "http://127.0.0.1:6997/multissid/"),
            BasicHeader("Accept-Encoding", "gzip, deflate, br"),
            BasicHeader("Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8,en-GB;q=0.7,en-US;q=0.6"),
            BasicHeader("Cookie", "_app_=eyJ1c2VyIjp7InVzZXJuYW1lIjoiYWRtaW4iLCJ1c2VyaWQiOjEwMCwiY3JlYXRlX3RpbWUiOiIifX0.")
        ))

        val response = client.execute(post)

        println(response.statusLine)

        val doc = Jsoup.parse(EntityUtils.toString(response.entity).also { println(it) })


        return setOf()
    }

    init {
        cookie = login()
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


        fun of(host: String, port: Int = 80, password: String = "admin") =
            of(host.toIPv4, port, password)

        fun of(iPv4: IPv4, port: Int = 80, password: String = "admin"): GhinfAP? {
            val ghinfAP = GhinfAP(iPv4, port, password)

            ghinfAP.cookie ?: return null

            return ghinfAP
        }


    }
}