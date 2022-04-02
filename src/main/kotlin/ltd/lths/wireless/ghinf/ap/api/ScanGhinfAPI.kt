package ltd.lths.wireless.ghinf.ap.api

import ltd.lths.wireless.ghinf.ap.util.IPv4

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.api.ScanGhinfAPI
 *
 * @author Score2
 * @since 2022/04/02 9:49
 */

val String.toIPv4 get() = IPv4(this)