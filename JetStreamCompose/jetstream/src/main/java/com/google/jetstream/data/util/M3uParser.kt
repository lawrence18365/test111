package com.google.jetstream.data.util

import com.google.jetstream.data.models.xtream.XtreamChannel

data class M3uItem(
    val name: String,
    val url: String,
    val logo: String?,
    val group: String?,
    val epgId: String?
)

object M3uParser {
    fun parse(content: String): List<M3uItem> {
        val items = mutableListOf<M3uItem>()
        val lines = content.lines()
        
        var currentName = ""
        var currentLogo: String? = null
        var currentGroup: String? = null
        var currentEpgId: String? = null

        for (line in lines) {
            val trimmedLine = line.trim()
            if (trimmedLine.startsWith("#EXTINF:")) {
                // Parse EXTINF metadata
                currentName = trimmedLine.substringAfterLast(",").trim()
                currentLogo = extractAttribute(trimmedLine, "tvg-logo")
                currentGroup = extractAttribute(trimmedLine, "group-title")
                currentEpgId = extractAttribute(trimmedLine, "tvg-id")
            } else if (trimmedLine.startsWith("http") || trimmedLine.startsWith("rtmp")) {
                if (currentName.isNotEmpty()) {
                    items.add(
                        M3uItem(
                            name = currentName,
                            url = trimmedLine,
                            logo = currentLogo,
                            group = currentGroup,
                            epgId = currentEpgId
                        )
                    )
                    // Reset for next item
                    currentName = ""
                    currentLogo = null
                    currentGroup = null
                    currentEpgId = null
                }
            }
        }
        return items
    }

    private fun extractAttribute(line: String, attributeName: String): String? {
        val key = "$attributeName=\""
        if (!line.contains(key)) return null
        val start = line.indexOf(key) + key.length
        val end = line.indexOf("\"", start)
        if (start == -1 || end == -1) return null
        return line.substring(start, end)
    }
}
