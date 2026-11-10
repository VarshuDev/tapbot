package com.tapbot.core.runner.rss

import org.w3c.dom.Element
import org.w3c.dom.Node
import org.xml.sax.InputSource
import java.io.StringReader
import javax.xml.parsers.DocumentBuilderFactory

data class RssItem(
    val id: String,
    val title: String,
    val link: String,
    val publishedAt: String? = null,
    val summary: String? = null
)

data class RssFeed(
    val title: String,
    val items: List<RssItem>
)

/**
 * Pure JDK XML parser supporting both RSS 2.0 (`<rss><channel><item>`)
 * and Atom 1.0 (`<feed><entry>`) syndication feeds on JVM and Android.
 */
object RssFeedParser {

    fun parse(xml: String): Result<RssFeed> = runCatching {
        val trimmed = xml.trimStart('\uFEFF', ' ', '\t', '\r', '\n')
        require(trimmed.isNotBlank()) { "Feed XML cannot be empty" }

        val dbf = DocumentBuilderFactory.newInstance().apply {
            isNamespaceAware = false
            isExpandEntityReferences = false
            runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
            runCatching { setFeature("http://xml.org/sax/features/external-general-entities", false) }
            runCatching { setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        }

        val builder = dbf.newDocumentBuilder()
        val doc = builder.parse(InputSource(StringReader(trimmed)))
        val root = doc.documentElement ?: throw IllegalArgumentException("Missing XML root element")
        val rootName = root.tagName.substringAfter(':').lowercase()

        when (rootName) {
            "feed" -> parseAtom(root)
            "rss", "rdf" -> parseRss(root)
            else -> {
                if (root.getElementsByTagName("channel").length > 0) parseRss(root)
                else if (root.getElementsByTagName("entry").length > 0) parseAtom(root)
                else throw IllegalArgumentException("Unsupported feed root element: <${root.tagName}>")
            }
        }
    }

    private fun parseRss(root: Element): RssFeed {
        val channel = firstChildElement(root, "channel") ?: root
        val feedTitle = firstChildText(channel, "title").ifBlank { "RSS Feed" }
        val itemElements = directChildElements(channel, "item").ifEmpty {
            // Support RDF 1.0 where <item> may be direct child of <rdf:RDF>
            directChildElements(root, "item")
        }

        val items = itemElements.mapNotNull { el ->
            val title = cleanText(firstChildText(el, "title"))
            val link = firstChildText(el, "link").trim()
            val guid = firstChildText(el, "guid").trim()
            val pubDate = firstChildText(el, "pubDate").ifBlank {
                firstChildText(el, "dc:date")
            }.trim().takeIf { it.isNotBlank() }
            val desc = cleanText(
                firstChildText(el, "description").ifBlank { firstChildText(el, "content:encoded") }
            ).takeIf { it.isNotBlank() }

            val itemId = guid.ifBlank { link }.ifBlank { title }
            if (itemId.isBlank()) return@mapNotNull null

            RssItem(
                id = itemId,
                title = title.ifBlank { link.ifBlank { "Untitled Article" } },
                link = link,
                publishedAt = pubDate,
                summary = desc?.take(320)
            )
        }
        return RssFeed(title = cleanText(feedTitle), items = items)
    }

    private fun parseAtom(feedRoot: Element): RssFeed {
        val feedTitle = firstChildText(feedRoot, "title").ifBlank { "Atom Feed" }
        val entries = directChildElements(feedRoot, "entry")

        val items = entries.mapNotNull { entry ->
            val title = cleanText(firstChildText(entry, "title"))
            val linkEl = directChildElements(entry, "link").firstOrNull {
                val rel = it.getAttribute("rel")
                rel.isBlank() || rel.equals("alternate", ignoreCase = true)
            } ?: directChildElements(entry, "link").firstOrNull()
            val link = linkEl?.getAttribute("href")?.trim()?.takeIf { it.isNotBlank() }
                ?: linkEl?.textContent?.trim().orEmpty()
            val id = firstChildText(entry, "id").trim()
            val published = firstChildText(entry, "published").ifBlank {
                firstChildText(entry, "updated")
            }.trim().takeIf { it.isNotBlank() }
            val summary = cleanText(
                firstChildText(entry, "summary").ifBlank { firstChildText(entry, "content") }
            ).takeIf { it.isNotBlank() }

            val itemId = id.ifBlank { link }.ifBlank { title }
            if (itemId.isBlank()) return@mapNotNull null

            RssItem(
                id = itemId,
                title = title.ifBlank { link.ifBlank { "Untitled Entry" } },
                link = link,
                publishedAt = published,
                summary = summary?.take(320)
            )
        }
        return RssFeed(title = cleanText(feedTitle), items = items)
    }

    private fun firstChildElement(parent: Element, localName: String): Element? {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) {
                val tag = (n as Element).tagName
                if (tag.equals(localName, ignoreCase = true) ||
                    tag.substringAfter(':').equals(localName, ignoreCase = true)
                ) {
                    return n
                }
            }
        }
        return null
    }

    private fun directChildElements(parent: Element, localName: String): List<Element> {
        val out = mutableListOf<Element>()
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) {
                val el = n as Element
                val tag = el.tagName
                if (tag.equals(localName, ignoreCase = true) ||
                    tag.substringAfter(':').equals(localName, ignoreCase = true)
                ) {
                    out.add(el)
                }
            }
        }
        return out
    }

    private fun firstChildText(parent: Element, tagName: String): String {
        val children = parent.childNodes
        for (i in 0 until children.length) {
            val n = children.item(i)
            if (n.nodeType == Node.ELEMENT_NODE) {
                val el = n as Element
                if (el.tagName.equals(tagName, ignoreCase = true) ||
                    el.tagName.substringAfter(':').equals(tagName, ignoreCase = true)
                ) {
                    return el.textContent.orEmpty()
                }
            }
        }
        return ""
    }

    private fun cleanText(raw: String): String {
        return raw
            .replace(Regex("<[^>]+>"), " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
