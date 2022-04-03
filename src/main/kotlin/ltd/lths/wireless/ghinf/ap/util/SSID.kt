package ltd.lths.wireless.ghinf.ap.util

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.util.SSID
 *
 * @author Score2
 * @since 2022/04/02 15:12
 */
data class SSID(
    var id: String,
    var password: String = "",
    var encryption: Encryption = Encryption.NONE,
    var frequency: Frequency = Frequency.WLAN_2G,
    var hide: Boolean = false,
    var vlan: Int = 0,
    var property: Map<String, String> = mapOf()
) {

    override fun equals(other: Any?): Boolean {
        val ssid = other as? SSID ?: return false
        return ssid.id == id
                && ssid.password == password
                && ssid.encryption == encryption
                && ssid.frequency == frequency
                && ssid.hide == hide
                && ssid.vlan == vlan
                && ssid.property == property
    }

    enum class Encryption(val alia: String) {
        NONE("none"),
        WPA_TKIP("psk"),
        WPA2_PSK2("psk2"),
        WPA2_MIXED("psk2-mixed"),
        UNKNOWN("Unknown")
        ;
    }

    enum class Frequency(val mode: Int) {
        WLAN_2G(0),
        WLAN_5G(1),
    }
}