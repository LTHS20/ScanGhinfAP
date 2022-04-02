package ltd.lths.wireless.ghinf.ap.util

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.util.SSID
 *
 * @author Score2
 * @since 2022/04/02 15:12
 */
data class SSID(
    val id: String,
    val password: String,
    val encryption: Encryption,
    val frequency: Frequency
) {

    enum class Encryption(val alia: String) {
        NONE("none"),
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