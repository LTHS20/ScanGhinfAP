package ltd.lths.wireless.ghinf.ap.api

import ltd.lths.wireless.ghinf.ap.util.IPv4
import taboolib.module.configuration.ConfigSection
import taboolib.module.configuration.Configuration

/**
 * ScanGhinfAP
 * ltd.lths.wireless.ghinf.ap.api.ScanGhinfAPI
 *
 * @author Score2
 * @since 2022/04/02 9:49
 */

val String.toIPv4 get() = IPv4(this)

fun Any?.asSection(): Configuration = Configuration.empty().let {
    when (this) {
        is Configuration -> return this
        is ConfigSection -> {
            return Configuration.empty().also { it.root = this.root }
        }
        is Map<*, *> -> {
            this.entries.forEach { entry -> it[entry.key.toString()] = entry.value }
            return@let it
        }
        is List<*> -> this.forEach { any ->
            val args = any.toString().split(Regex(":"), 2)
            if (args.size == 2) it[args[0]] = args[1]
            return@let it
        }
    }
    throw ClassCastException()
}