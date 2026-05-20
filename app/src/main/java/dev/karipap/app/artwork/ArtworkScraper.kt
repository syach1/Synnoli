package dev.karipap.app.artwork

import androidx.annotation.StringRes
import dev.karipap.app.R
import dev.karipap.app.config.CannoliPaths
import dev.karipap.app.config.PlatformConfig
import dev.karipap.app.db.RomsRepository
import dev.karipap.app.di.CannoliPathsProvider
import dev.karipap.app.model.Rom
import dev.karipap.app.util.ArtworkLookup
import org.json.JSONObject
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

enum class ArtworkScraperSource(@StringRes val labelRes: Int) {
    LIBRETRO(R.string.artwork_source_libretro),
    THEGAMESDB(R.string.artwork_source_thegamesdb),
    DSESS(R.string.artwork_source_dsess),
}

data class ArtworkScraperPlatform(
    val tag: String,
    val name: String,
    val romCount: Int,
)

data class ArtworkScrapeResult(
    val attempted: Int,
    val downloaded: Int,
    val skippedExisting: Int,
    val notFound: Int,
    val errors: Int,
    val message: String,
)

@Singleton
class ArtworkScraper @Inject constructor(
    private val pathsProvider: CannoliPathsProvider,
    private val romsRepository: RomsRepository,
    private val platformConfig: PlatformConfig,
    private val artworkLookup: ArtworkLookup,
) {
    private val paths: CannoliPaths get() = CannoliPaths(pathsProvider.root)
    private val robotsCache = ConcurrentHashMap<String, RobotsRules>()
    private val lastFetchMsByHost = ConcurrentHashMap<String, Long>()
    private val throttleLock = Any()

    fun platformsWithRoms(): List<ArtworkScraperPlatform> {
        return romsRepository.platformCounts()
            .filterValues { it > 0 }
            .map { (tag, count) ->
                val upper = tag.uppercase(Locale.US)
                ArtworkScraperPlatform(
                    tag = upper,
                    name = platformConfig.getDisplayName(upper),
                    romCount = count,
                )
            }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    suspend fun scrape(
        platform: ArtworkScraperPlatform,
        source: ArtworkScraperSource,
        onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> },
    ): ArtworkScrapeResult {
        return when (source) {
            ArtworkScraperSource.LIBRETRO -> scrapeLibretro(platform, onProgress)
            ArtworkScraperSource.THEGAMESDB -> scrapeTheGamesDb(platform, onProgress)
            ArtworkScraperSource.DSESS -> scrapeDsess(platform, onProgress)
        }
    }

    private suspend fun scrapeLibretro(
        platform: ArtworkScraperPlatform,
        onProgress: suspend (done: Int, total: Int) -> Unit,
    ): ArtworkScrapeResult {
        val repoName = libretroThumbnailRepos[platform.tag]
            ?: return ArtworkScrapeResult(
                attempted = 0,
                downloaded = 0,
                skippedExisting = 0,
                notFound = 0,
                errors = 0,
                message = "No Libretro thumbnail set is mapped for ${platform.name}.",
            )
        val roms = romsRepository.romsForArtwork(platform.tag)
        return scrapeRoms(platform, roms, onProgress) { rom, artDir ->
            val baseName = rom.path.nameWithoutExtension
            for (name in titleCandidates(rom)) {
                val remoteName = sanitizeLibretroName(name)
                val url = URL(
                    "https://thumbnails.libretro.com/${encodePathSegment(repoName)}/Named_Boxarts/" +
                        "${encodePathSegment(remoteName)}.png"
                )
                val dest = File(artDir, "$baseName.png")
                if (downloadImage(url, dest, "Libretro Thumbnails", url.toString(), checkRobots = true)) {
                    return@scrapeRoms ScrapeOutcome.Downloaded
                }
            }
            ScrapeOutcome.NotFound
        }
    }

    private suspend fun scrapeTheGamesDb(
        platform: ArtworkScraperPlatform,
        onProgress: suspend (done: Int, total: Int) -> Unit,
    ): ArtworkScrapeResult {
        val keyFile = File(paths.configDir, "ArtworkScraper/thegamesdb_api_key.txt")
        val apiKey = keyFile.takeIf { it.isFile }?.readText()?.trim().orEmpty()
        if (apiKey.isEmpty()) {
            return ArtworkScrapeResult(
                attempted = 0,
                downloaded = 0,
                skippedExisting = 0,
                notFound = 0,
                errors = 0,
                message = "TheGamesDB API key missing. Put it at Config/ArtworkScraper/thegamesdb_api_key.txt.",
            )
        }
        val platformId = resolveTheGamesDbPlatformId(apiKey, platform)
        val roms = romsRepository.romsForArtwork(platform.tag)
        return scrapeRoms(platform, roms, onProgress) { rom, artDir ->
            scrapeTheGamesDbRom(apiKey, platformId, rom, artDir)
        }
    }

    private suspend fun scrapeDsess(
        platform: ArtworkScraperPlatform,
        onProgress: suspend (done: Int, total: Int) -> Unit,
    ): ArtworkScrapeResult {
        val configs = readDsessConfigs()
        if (configs.isEmpty()) {
            return ArtworkScrapeResult(
                attempted = 0,
                downloaded = 0,
                skippedExisting = 0,
                notFound = 0,
                errors = 0,
                message = "DSESS config missing. Add BOX_ART lines to Config/ArtworkScraper/dsess.txt.",
            )
        }
        val roms = romsRepository.romsForArtwork(platform.tag)
        return scrapeRoms(platform, roms, onProgress) { rom, artDir ->
            val tags = dsessTags(platform, rom)
            for (config in configs) {
                if (!config.requiredTags.all { tags.containsKey(it) }) continue
                val imageUrl = resolveDsessImage(config, tags) ?: continue
                val ext = imageExtension(imageUrl)
                val dest = File(artDir, "${rom.path.nameWithoutExtension}.$ext")
                if (downloadImage(imageUrl, dest, "DSESS HTML scraper", config.urlTemplate, checkRobots = true)) {
                    return@scrapeRoms ScrapeOutcome.Downloaded
                }
            }
            ScrapeOutcome.NotFound
        }
    }

    private suspend fun scrapeRoms(
        platform: ArtworkScraperPlatform,
        roms: List<Rom>,
        onProgress: suspend (done: Int, total: Int) -> Unit,
        scraper: (Rom, File) -> ScrapeOutcome,
    ): ArtworkScrapeResult {
        if (roms.isEmpty()) {
            return ArtworkScrapeResult(0, 0, 0, 0, 0, "No ROMs found for ${platform.name}.")
        }
        val artDir = paths.artFor(platform.tag)
        artDir.mkdirs()
        var skipped = 0
        var downloaded = 0
        var notFound = 0
        var errors = 0
        roms.forEachIndexed { index, rom ->
            onProgress(index + 1, roms.size)
            if (cachedArtworkExists(artDir, rom.path.nameWithoutExtension)) {
                skipped++
                return@forEachIndexed
            }
            when (try { scraper(rom, artDir) } catch (_: Throwable) { ScrapeOutcome.Error }) {
                ScrapeOutcome.Downloaded -> downloaded++
                ScrapeOutcome.NotFound -> notFound++
                ScrapeOutcome.Error -> errors++
            }
        }
        artworkLookup.invalidate(platform.tag)
        val attempted = roms.size - skipped
        val message = buildString {
            append("${platform.name}: cached $downloaded cover")
            if (downloaded != 1) append("s")
            append(".")
            if (skipped > 0) append(" Existing: $skipped.")
            if (notFound > 0) append(" Not found: $notFound.")
            if (errors > 0) append(" Errors: $errors.")
        }
        return ArtworkScrapeResult(attempted, downloaded, skipped, notFound, errors, message)
    }

    private fun scrapeTheGamesDbRom(
        apiKey: String,
        platformId: Int?,
        rom: Rom,
        artDir: File,
    ): ScrapeOutcome {
        for (title in titleCandidates(rom)) {
            val url = buildString {
                append("https://api.thegamesdb.net/v1.1/Games/ByGameName")
                append("?apikey=").append(queryEncode(apiKey))
                append("&name=").append(queryEncode(title))
                append("&include=boxart,platform")
                append("&mode=natural")
                if (platformId != null) append("&filter%5Bplatform%5D=").append(platformId)
            }
            val body = fetchText(URL(url), "application/json", checkRobots = false) ?: continue
            val root = JSONObject(body)
            if (root.optInt("code") == 403) return ScrapeOutcome.Error
            val games = root.optJSONObject("data")?.optJSONArray("games") ?: continue
            if (games.length() == 0) continue
            val selectedId = selectTheGamesDbGameId(games, title, platformId) ?: continue
            val boxart = root.optJSONObject("include")?.optJSONObject("boxart") ?: continue
            val baseUrl = boxart.optJSONObject("base_url")
                ?.let { it.optString("medium").ifEmpty { it.optString("original") }.ifEmpty { it.optString("thumb") } }
                ?: continue
            val artEntries = boxart.optJSONObject("data")?.optJSONArray(selectedId.toString()) ?: continue
            val fileName = (0 until artEntries.length())
                .mapNotNull { artEntries.optJSONObject(it) }
                .firstOrNull {
                    it.optString("type") == "boxart" &&
                        (it.optString("side").isEmpty() || it.optString("side") == "front")
                }
                ?.optString("filename")
                ?.takeIf { it.isNotEmpty() }
                ?: continue
            val imageUrl = URL(baseUrl + fileName)
            val dest = File(artDir, "${rom.path.nameWithoutExtension}.${imageExtension(imageUrl)}")
            if (downloadImage(imageUrl, dest, "TheGamesDB", imageUrl.toString(), checkRobots = false)) {
                return ScrapeOutcome.Downloaded
            }
        }
        return ScrapeOutcome.NotFound
    }

    private fun resolveTheGamesDbPlatformId(apiKey: String, platform: ArtworkScraperPlatform): Int? {
        val names = listOfNotNull(theGamesDbPlatformNames[platform.tag], platform.name).distinct()
        for (name in names) {
            val url = URL(
                "https://api.thegamesdb.net/v1/Platforms/ByPlatformName" +
                    "?apikey=${queryEncode(apiKey)}&name=${queryEncode(name)}"
            )
            val body = fetchText(url, "application/json", checkRobots = false) ?: continue
            val platforms = JSONObject(body).optJSONObject("data")?.optJSONArray("platforms") ?: continue
            val normalizedTarget = normalizeTitle(name)
            val exact = (0 until platforms.length())
                .mapNotNull { platforms.optJSONObject(it) }
                .firstOrNull { normalizeTitle(it.optString("name")) == normalizedTarget }
            val chosen = exact ?: platforms.optJSONObject(0)
            val id = chosen?.optInt("id", 0) ?: 0
            if (id > 0) return id
        }
        return null
    }

    private fun selectTheGamesDbGameId(games: org.json.JSONArray, title: String, platformId: Int?): Int? {
        val normalized = normalizeTitle(title)
        val candidates = (0 until games.length()).mapNotNull { games.optJSONObject(it) }
        val exact = candidates.firstOrNull { game ->
            normalizeTitle(game.optString("game_title")) == normalized &&
                (platformId == null || game.optInt("platform") == platformId)
        }
        val platformMatch = candidates.firstOrNull { platformId == null || it.optInt("platform") == platformId }
        return (exact ?: platformMatch ?: candidates.firstOrNull())?.optInt("id", 0)?.takeIf { it > 0 }
    }

    private fun readDsessConfigs(): List<DsessConfig> {
        val file = File(paths.configDir, "ArtworkScraper/dsess.txt")
        if (!file.isFile) return emptyList()
        return file.readLines()
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("#") }
            .mapNotNull { line ->
                val parts = line.split(":", limit = 4)
                if (parts.size != 4 || parts[0] != "DSESS" || parts[1] != "BOX_ART") return@mapNotNull null
                val tags = Regex("""TAGS\(([^)]*)\)""").find(parts[2])
                    ?.groupValues?.getOrNull(1)
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?: emptyList()
                DsessConfig(requiredTags = tags, urlTemplate = parts[3])
            }
    }

    private fun dsessTags(platform: ArtworkScraperPlatform, rom: Rom): Map<String, String> {
        val keyword = titleCandidates(rom).firstOrNull().orEmpty()
        return mapOf(
            "scraperKeyword" to keyword,
            "scraperKeywordNormalized" to normalizeTitle(keyword),
            "platform" to platform.tag,
            "platformName" to platform.name,
            "localeLanguage" to Locale.getDefault().language,
            "localeCountry" to Locale.getDefault().country,
        )
    }

    private fun resolveDsessImage(config: DsessConfig, tags: Map<String, String>): URL? {
        val taggedUrl = applyDsessTags(config.urlTemplate, tags)
        val (requestUrl, params) = stripDsessParams(taggedUrl)
        val html = fetchText(URL(requestUrl), "text/html", checkRobots = true) ?: return null
        val document = Jsoup.parse(html, requestUrl)

        val targetUrl = resolveDsessTarget(document, requestUrl, params)
        val targetDocument = if (targetUrl == requestUrl) {
            document
        } else {
            val targetHtml = fetchText(URL(targetUrl), "text/html", checkRobots = true) ?: return null
            Jsoup.parse(targetHtml, targetUrl)
        }

        val selector = params["dsess_selector"].orEmpty()
        if (selector.isEmpty()) return null
        val element = targetDocument.select(selector).firstOrNull() ?: return null
        val attr = params["dsess_selector_attr"]
        var raw = if (attr.isNullOrEmpty()) element.text() else element.absUrl(attr).ifEmpty { element.attr(attr) }
        val extractor = params["dsess_selector_regex_extract"]
        if (!extractor.isNullOrEmpty()) {
            raw = try {
                Regex(extractor).find(raw)?.groupValues?.getOrNull(1) ?: raw
            } catch (_: Throwable) {
                raw
            }
        }
        val replacer = params["dsess_selector_regex_replace"]
        if (!replacer.isNullOrEmpty()) {
            raw = try {
                Regex(replacer).replace(raw, params["dsess_selector_regex_replace_by"].orEmpty())
            } catch (_: Throwable) {
                raw
            }
        }
        return targetDocument.resolveUrl(raw)
    }

    private fun resolveDsessTarget(
        document: org.jsoup.nodes.Document,
        requestUrl: String,
        params: Map<String, String>,
    ): String {
        val targetRegex = params["dsess_target_site"]?.takeIf { it.isNotEmpty() }?.let {
            try { Regex(it) } catch (_: Throwable) { null }
        }
        val linkSelector = params["dsess_target_selector"].orEmpty().ifEmpty { "a" }
        val labelSelector = params["dsess_target_selector_label"]
        val links = document.select(linkSelector).mapNotNull { element ->
            val href = element.absUrl("href").ifEmpty { element.attr("href") }
            if (href.isEmpty()) return@mapNotNull null
            val resolved = document.resolveUrl(href)?.toString() ?: return@mapNotNull null
            val label = labelSelector?.let { element.select(it).text() }?.ifEmpty { null } ?: element.text()
            resolved to label
        }
        val matcherTerms = params
            .filterKeys { it.startsWith("dsess_target_ordered_matcher_") }
            .toSortedMap()
            .values
            .map { normalizeTitle(it) }
            .filter { it.isNotEmpty() }
        val match = links
            .asSequence()
            .filter { (url, _) -> targetRegex?.containsMatchIn(url) ?: true }
            .map { candidate ->
                val scoreText = normalizeTitle(candidate.second + " " + candidate.first)
                val score = matcherTerms.count { scoreText.contains(it) }
                candidate to score
            }
            .sortedByDescending { it.second }
            .firstOrNull()
            ?.first
            ?.first
        return match ?: requestUrl
    }

    private fun org.jsoup.nodes.Document.resolveUrl(raw: String): URL? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val absolute = when {
            trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true) -> trimmed
            else -> this.location().trimEnd('/') + "/" + trimmed.trimStart('/')
        }
        return try { URL(absolute) } catch (_: Throwable) { null }
    }

    private fun applyDsessTags(template: String, tags: Map<String, String>): String {
        var out = template
        for ((key, value) in tags) {
            val encoded = queryEncode(value)
            out = out
                .replace("{$key}", encoded)
                .replace("%7B$key%7D", encoded)
                .replace("%7b$key%7d", encoded)
        }
        return out
    }

    private fun stripDsessParams(url: String): Pair<String, Map<String, String>> {
        val qIndex = url.indexOf('?')
        if (qIndex < 0) return url to emptyMap()
        val base = url.substring(0, qIndex)
        val query = url.substring(qIndex + 1)
        val kept = mutableListOf<String>()
        val dsess = linkedMapOf<String, String>()
        for (pair in query.split("&")) {
            if (pair.isEmpty()) continue
            val key = pair.substringBefore("=")
            val decodedKey = urlDecode(key)
            if (decodedKey.startsWith("dsess_")) {
                dsess[decodedKey] = urlDecode(pair.substringAfter("=", ""))
            } else {
                kept.add(pair)
            }
        }
        val cleaned = if (kept.isEmpty()) base else "$base?${kept.joinToString("&")}"
        return cleaned to dsess
    }

    private fun titleCandidates(rom: Rom): List<String> {
        val fileName = rom.path.nameWithoutExtension
        val displayName = rom.displayName
        val strippedDisplay = stripTitleDecorations(displayName)
        val strippedFile = stripTitleDecorations(fileName)
        return listOf(displayName, fileName, strippedDisplay, strippedFile)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    private fun stripTitleDecorations(value: String): String {
        return value
            .replace(Regex("""\s*\[[^]]*]"""), "")
            .replace(Regex("""\s*\([^)]*\)"""), "")
            .trim()
    }

    private fun sanitizeLibretroName(value: String): String {
        return value.replace(Regex("""[&*/:`<>?\\|]"""), "_")
    }

    private fun normalizeTitle(value: String): String {
        return stripTitleDecorations(value)
            .lowercase(Locale.US)
            .replace(Regex("""[^a-z0-9]+"""), " ")
            .trim()
    }

    private fun cachedArtworkExists(artDir: File, basename: String): Boolean {
        return artDir.listFiles { f -> f.isFile && f.nameWithoutExtension == basename }?.isNotEmpty() == true
    }

    private fun downloadImage(
        url: URL,
        dest: File,
        sourceName: String,
        sourceUrl: String,
        checkRobots: Boolean,
    ): Boolean {
        if (checkRobots && !robotsAllowed(url)) return false
        val conn = openGet(url, "image/*")
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return false
            if (code !in 200..299) throw IOException("HTTP $code")
            val type = conn.contentType.orEmpty().lowercase(Locale.US)
            if (!type.startsWith("image/")) return false
            dest.parentFile?.mkdirs()
            val temp = File(dest.parentFile, "${dest.name}.download")
            conn.inputStream.use { input ->
                FileOutputStream(temp).use { output -> input.copyTo(output) }
            }
            if (temp.length() <= 0L) {
                temp.delete()
                return false
            }
            if (dest.exists()) dest.delete()
            if (!temp.renameTo(dest)) {
                temp.copyTo(dest, overwrite = true)
                temp.delete()
            }
            writeAttribution(dest, sourceName, sourceUrl)
            return true
        } finally {
            conn.disconnect()
        }
    }

    private fun fetchText(url: URL, accept: String, checkRobots: Boolean): String? {
        if (checkRobots && !robotsAllowed(url)) return null
        val conn = openGet(url, accept)
        try {
            val code = conn.responseCode
            if (code == HttpURLConnection.HTTP_NOT_FOUND) return null
            if (code !in 200..299) throw IOException("HTTP $code")
            return conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    private fun openGet(url: URL, accept: String): HttpURLConnection {
        throttle(url)
        return (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            instanceFollowRedirects = true
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("User-Agent", USER_AGENT)
            setRequestProperty("Accept", accept)
        }
    }

    private fun throttle(url: URL) {
        val key = "${url.protocol}://${url.host}:${effectivePort(url)}"
        synchronized(throttleLock) {
            val now = System.currentTimeMillis()
            val last = lastFetchMsByHost[key] ?: 0L
            val waitMs = 350L - (now - last)
            if (waitMs > 0L) Thread.sleep(waitMs)
            lastFetchMsByHost[key] = System.currentTimeMillis()
        }
    }

    private fun robotsAllowed(url: URL): Boolean {
        val key = "${url.protocol}://${url.host}:${effectivePort(url)}"
        val rules = robotsCache.getOrPut(key) { loadRobotsRules(url) }
        return rules.allows(url.path.ifEmpty { "/" })
    }

    private fun loadRobotsRules(url: URL): RobotsRules {
        val robotsUrl = URL(url.protocol, url.host, url.port, "/robots.txt")
        return try {
            val conn = (robotsUrl.openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                instanceFollowRedirects = true
                connectTimeout = 5_000
                readTimeout = 8_000
                setRequestProperty("User-Agent", USER_AGENT)
            }
            try {
                val code = conn.responseCode
                if (code == HttpURLConnection.HTTP_NOT_FOUND) return RobotsRules(emptyList())
                if (code !in 200..299) return RobotsRules(listOf(RobotsRule(false, "/")))
                parseRobots(conn.inputStream.bufferedReader().use { it.readText() })
            } finally {
                conn.disconnect()
            }
        } catch (_: Throwable) {
            RobotsRules(listOf(RobotsRule(false, "/")))
        }
    }

    private fun parseRobots(text: String): RobotsRules {
        val rules = mutableListOf<RobotsRule>()
        var applies = false
        for (rawLine in text.lineSequence()) {
            val line = rawLine.substringBefore("#").trim()
            if (line.isEmpty() || !line.contains(":")) continue
            val key = line.substringBefore(":").trim().lowercase(Locale.US)
            val value = line.substringAfter(":").trim()
            when (key) {
                "user-agent" -> applies = value == "*" || value.contains("Karipap", ignoreCase = true)
                "allow" -> if (applies && value.isNotEmpty()) rules.add(RobotsRule(true, value))
                "disallow" -> if (applies && value.isNotEmpty()) rules.add(RobotsRule(false, value))
            }
        }
        return RobotsRules(rules)
    }

    private fun writeAttribution(dest: File, sourceName: String, sourceUrl: String) {
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss Z", Locale.US).format(Date())
        File(dest.parentFile, "${dest.nameWithoutExtension}.source.txt").writeText(
            "Source: $sourceName\nURL: $sourceUrl\nCached: $stamp\n"
        )
    }

    private fun imageExtension(url: URL): String {
        val ext = url.path.substringAfterLast('.', "jpg").lowercase(Locale.US)
        return if (ext in imageExtensions) ext else "jpg"
    }

    private fun encodePathSegment(value: String): String = queryEncode(value).replace("+", "%20")
    private fun queryEncode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8.name())
    private fun urlDecode(value: String): String = URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    private fun effectivePort(url: URL): Int = if (url.port > 0) url.port else url.defaultPort

    private enum class ScrapeOutcome { Downloaded, NotFound, Error }

    private data class DsessConfig(
        val requiredTags: List<String>,
        val urlTemplate: String,
    )

    private data class RobotsRule(val allow: Boolean, val path: String)

    private class RobotsRules(private val rules: List<RobotsRule>) {
        fun allows(path: String): Boolean {
            val matching = rules.filter { path.startsWith(it.path) }
            if (matching.isEmpty()) return true
            val longest = matching.maxOf { it.path.length }
            return matching.filter { it.path.length == longest }.any { it.allow }
        }
    }

    private companion object {
        private const val USER_AGENT = "KaripapArtworkScraper/1.0 (+local-cache)"

        private val imageExtensions = setOf("jpg", "jpeg", "png", "webp")

        private val libretroThumbnailRepos = mapOf(
            "GB" to "Nintendo - Game Boy",
            "GBC" to "Nintendo - Game Boy Color",
            "GBA" to "Nintendo - Game Boy Advance",
            "NES" to "Nintendo - Nintendo Entertainment System",
            "FDS" to "Nintendo - Family Computer Disk System",
            "SNES" to "Nintendo - Super Nintendo Entertainment System",
            "N64" to "Nintendo - Nintendo 64",
            "NDS" to "Nintendo - Nintendo DS",
            "3DS" to "Nintendo - Nintendo 3DS",
            "VIRTUALBOY" to "Nintendo - Virtual Boy",
            "POKEMINI" to "Nintendo - Pokemon Mini",
            "GC" to "Nintendo - GameCube",
            "WII" to "Nintendo - Wii",
            "WIIU" to "Nintendo - Wii U",
            "GG" to "Sega - Game Gear",
            "SMS" to "Sega - Master System - Mark III",
            "MD" to "Sega - Mega Drive - Genesis",
            "SG1000" to "Sega - SG-1000",
            "32X" to "Sega - 32X",
            "SEGACD" to "Sega - Mega-CD - Sega CD",
            "SATURN" to "Sega - Saturn",
            "DC" to "Sega - Dreamcast",
            "PS" to "Sony - PlayStation",
            "PS2" to "Sony - PlayStation 2",
            "PSP" to "Sony - PlayStation Portable",
            "PS3" to "Sony - PlayStation 3",
            "PSVITA" to "Sony - PlayStation Vita",
            "LYNX" to "Atari - Lynx",
            "JAGUAR" to "Atari - Jaguar",
            "ATARI2600" to "Atari - 2600",
            "ATARI5200" to "Atari - 5200",
            "ATARI7800" to "Atari - 7800",
            "PCE" to "NEC - PC Engine - TurboGrafx 16",
            "SUPERGRAFX" to "NEC - PC Engine SuperGrafx",
            "PCFX" to "NEC - PC-FX",
            "NGP" to "SNK - Neo Geo Pocket",
            "NGPC" to "SNK - Neo Geo Pocket Color",
            "NEOGEO" to "SNK - Neo Geo",
            "WS" to "Bandai - WonderSwan",
            "WSC" to "Bandai - WonderSwan Color",
            "COLECOVISION" to "Coleco - ColecoVision",
            "VECTREX" to "GCE - Vectrex",
            "INTELLIVISION" to "Mattel - Intellivision",
            "AMIGA" to "Commodore - Amiga",
            "DOS" to "DOS",
            "SCUMMVM" to "ScummVM",
        )

        private val theGamesDbPlatformNames = mapOf(
            "MD" to "Sega Genesis",
            "PS" to "Sony Playstation",
            "PCE" to "TurboGrafx-16",
            "SEGACD" to "Sega CD",
            "SNES" to "Super Nintendo Entertainment System",
            "NES" to "Nintendo Entertainment System (NES)",
            "GB" to "Nintendo Game Boy",
            "GBC" to "Nintendo Game Boy Color",
            "GBA" to "Nintendo Game Boy Advance",
            "NDS" to "Nintendo DS",
            "3DS" to "Nintendo 3DS",
            "N64" to "Nintendo 64",
            "GC" to "Nintendo GameCube",
            "WII" to "Nintendo Wii",
            "WIIU" to "Nintendo Wii U",
            "PS2" to "Sony Playstation 2",
            "PS3" to "Sony Playstation 3",
            "PSP" to "Sony PSP",
            "PSVITA" to "Sony Playstation Vita",
        )
    }
}
