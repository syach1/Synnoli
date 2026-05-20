package dev.karipap.app.util

import java.io.File
import java.util.Locale

object PlatformFolderAliases {
    private val aliases = mapOf(
        "GB" to setOf("gameboy", "game boy", "nintendo game boy", "dmg", "sgb", "super game boy"),
        "GBC" to setOf("gameboycolor", "game boy color", "nintendo game boy color", "cgb"),
        "GBA" to setOf("gameboyadvance", "game boy advance", "nintendo game boy advance"),
        "NES" to setOf("famicom", "fc", "nintendo entertainment system"),
        "FDS" to setOf("famicom disk system", "famicomdisk", "famicom disk"),
        "SNES" to setOf("sfc", "superfamicom", "super famicom", "super nintendo", "super nintendo entertainment system"),
        "N64" to setOf("nintendo64", "nintendo 64"),
        "NDS" to setOf("ds", "nintendods", "nintendo ds"),
        "GG" to setOf("gamegear", "game gear"),
        "SMS" to setOf("mastersystem", "master system", "sega master system"),
        "MD" to setOf("genesis", "megadrive", "mega drive", "sega genesis", "sega mega drive"),
        "SG1000" to setOf("sg-1000", "sg 1000", "sega sg-1000", "sega sg1000"),
        "32X" to setOf("sega32x", "sega 32x"),
        "SEGACD" to setOf("segacd", "sega cd", "megacd", "mega cd"),
        "SATURN" to setOf("sega saturn"),
        "PS" to setOf("psx", "ps1", "playstation", "playstation 1", "sony playstation"),
        "PSP" to setOf("playstation portable", "sony psp"),
        "DC" to setOf("dreamcast", "sega dreamcast"),
        "LYNX" to setOf("atarilynx", "atari lynx"),
        "JAGUAR" to setOf("atarijaguar", "atari jaguar"),
        "ATARI2600" to setOf("a2600", "2600", "atari 2600"),
        "ATARI5200" to setOf("a5200", "5200", "atari 5200"),
        "ATARI7800" to setOf("a7800", "7800", "atari 7800"),
        "PCE" to setOf(
            "pcengine",
            "pc engine",
            "pcenginecd",
            "pc engine cd",
            "turbografx",
            "turbografx16",
            "turbografx cd",
            "turbografxcd",
            "tg16",
            "tg16cd",
            "tgcd",
        ),
        "PCFX" to setOf("pc-fx", "pc fx"),
        "NGP" to setOf("neogeopocket", "neo geo pocket"),
        "NGPC" to setOf("neogeopocketcolor", "neo geo pocket color"),
        "WS" to setOf("wonderswan", "wonder swan"),
        "WSC" to setOf("wonderswancolor", "wonder swan color"),
        "NEOGEO" to setOf("neo geo"),
        "MAME" to setOf("arcade", "mame2003", "mame2003-plus", "mame2003plus"),
        "FBN" to setOf("fbneo", "finalburnneo", "final burn neo", "fba", "fbalpha", "finalburnalpha", "final burn alpha"),
        "VIRTUALBOY" to setOf("virtualboy", "virtual boy", "vb"),
        "POKEMINI" to setOf("pokemonmini", "pokemon mini", "pokemini"),
        "COLECOVISION" to setOf("coleco", "colecovision"),
        "VECTREX" to setOf("vec"),
        "SUPERGRAFX" to setOf("sgx", "supergrafx", "pc engine supergrafx"),
        "SCUMMVM" to setOf("scumm"),
        "AMIGA" to setOf("amigacd32", "amiga cd32", "cd32"),
        "PS2" to setOf("playstation2", "playstation 2", "sony playstation 2"),
        "GC" to setOf("gamecube", "game cube", "ngc", "nintendo gamecube"),
        "WII" to setOf("nintendo wii"),
        "3DS" to setOf("n3ds", "nintendo3ds", "nintendo 3ds"),
        "WIIU" to setOf("wiiu", "wii u", "nintendo wii u"),
        "PSVITA" to setOf("vita", "ps vita", "playstation vita", "sony playstation vita"),
        "PS3" to setOf("playstation3", "playstation 3", "sony playstation 3"),
        "NSW" to setOf("switch", "nintendo switch"),
    )

    fun matches(platformTag: String, folderName: String): Boolean {
        val folderKey = aliasKey(folderName)
        if (folderKey.isEmpty()) return false
        val tag = platformTag.uppercase(Locale.US)
        if (folderKey == aliasKey(tag)) return true
        return aliases[tag].orEmpty().any { aliasKey(it) == folderKey }
    }

    fun stripLeadingPlatformSegment(platformTag: String, relativePath: String): String {
        val separator = File.separator
        val firstSegment = relativePath.substringBefore(separator)
        if (firstSegment.isEmpty() || firstSegment == relativePath) return relativePath
        return if (matches(platformTag, firstSegment)) {
            relativePath.substring(firstSegment.length + separator.length)
        } else {
            relativePath
        }
    }

    fun normalizedPlatformRelativePath(platformTag: String, relativePath: String): String =
        stripLeadingPlatformSegment(platformTag, relativePath).lowercase(Locale.US)

    private fun aliasKey(value: String): String =
        value.lowercase(Locale.US).filter { it.isLetterOrDigit() }
}
