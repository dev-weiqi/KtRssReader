/*
 * Copyright 2020 Feng Hsien Hsu, Siao Syuan Yang, Wei-Qi Wang, Ya-Han Tsai, Yu Hao Wu
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package tw.ktrssreader.parser

import android.util.Xml
import java.io.ByteArrayInputStream
import java.io.IOException
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import tw.ktrssreader.kotlin.constant.ParserConst.AUTHOR
import tw.ktrssreader.kotlin.constant.ParserConst.CATEGORY
import tw.ktrssreader.kotlin.constant.ParserConst.CHANNEL
import tw.ktrssreader.kotlin.constant.ParserConst.CLOUD
import tw.ktrssreader.kotlin.constant.ParserConst.COMMENTS
import tw.ktrssreader.kotlin.constant.ParserConst.COPYRIGHT
import tw.ktrssreader.kotlin.constant.ParserConst.DAY
import tw.ktrssreader.kotlin.constant.ParserConst.DESCRIPTION
import tw.ktrssreader.kotlin.constant.ParserConst.DOCS
import tw.ktrssreader.kotlin.constant.ParserConst.DOMAIN
import tw.ktrssreader.kotlin.constant.ParserConst.ENCLOSURE
import tw.ktrssreader.kotlin.constant.ParserConst.GENERATOR
import tw.ktrssreader.kotlin.constant.ParserConst.GUID
import tw.ktrssreader.kotlin.constant.ParserConst.HEIGHT
import tw.ktrssreader.kotlin.constant.ParserConst.HOUR
import tw.ktrssreader.kotlin.constant.ParserConst.IMAGE
import tw.ktrssreader.kotlin.constant.ParserConst.ITEM
import tw.ktrssreader.kotlin.constant.ParserConst.LANGUAGE
import tw.ktrssreader.kotlin.constant.ParserConst.LAST_BUILD_DATE
import tw.ktrssreader.kotlin.constant.ParserConst.LENGTH
import tw.ktrssreader.kotlin.constant.ParserConst.LINK
import tw.ktrssreader.kotlin.constant.ParserConst.MANAGING_EDITOR
import tw.ktrssreader.kotlin.constant.ParserConst.NAME
import tw.ktrssreader.kotlin.constant.ParserConst.PATH
import tw.ktrssreader.kotlin.constant.ParserConst.PERMALINK
import tw.ktrssreader.kotlin.constant.ParserConst.PORT
import tw.ktrssreader.kotlin.constant.ParserConst.PROTOCOL
import tw.ktrssreader.kotlin.constant.ParserConst.PUB_DATE
import tw.ktrssreader.kotlin.constant.ParserConst.RATING
import tw.ktrssreader.kotlin.constant.ParserConst.REGISTER_PROCEDURE
import tw.ktrssreader.kotlin.constant.ParserConst.SKIP_DAYS
import tw.ktrssreader.kotlin.constant.ParserConst.SKIP_HOURS
import tw.ktrssreader.kotlin.constant.ParserConst.SOURCE
import tw.ktrssreader.kotlin.constant.ParserConst.TEXT_INPUT
import tw.ktrssreader.kotlin.constant.ParserConst.TITLE
import tw.ktrssreader.kotlin.constant.ParserConst.TTL
import tw.ktrssreader.kotlin.constant.ParserConst.TYPE
import tw.ktrssreader.kotlin.constant.ParserConst.URL
import tw.ktrssreader.kotlin.constant.ParserConst.WEB_MASTER
import tw.ktrssreader.kotlin.constant.ParserConst.WIDTH
import tw.ktrssreader.kotlin.model.channel.*
import tw.ktrssreader.kotlin.model.item.*
import tw.ktrssreader.utils.logD
import tw.ktrssreader.utils.logW

abstract class AndroidParserBase<out T : RssStandardChannel> : tw.ktrssreader.kotlin.parser.Parser<T> {

    abstract val logTag: String

    protected inline fun <T> parseChannel(xml: String, action: XmlPullParser.() -> T): T {
        val parser = getXmlParser(xml)

        var result: T? = null
        while (parser.next() != XmlPullParser.END_TAG) {
            if (parser.eventType != XmlPullParser.START_TAG) continue

            if (parser.name == CHANNEL) {
                result = action(parser)
                break
            } else {
                parser.skip()
            }
        }
        return result ?: throw XmlPullParserException("No valid channel tag in the RSS feed.")
    }

    protected fun parseStandardChannel(xml: String): RssStandardChannelData {
        return parseChannel(xml) { readRssStandardChannel() }
    }

    protected fun getXmlParser(xml: String): XmlPullParser {
        ByteArrayInputStream(xml.toByteArray()).use { inputStream ->
            return Xml.newPullParser().apply {
                setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
                setInput(inputStream, null)
                nextTag()
            }
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readString(tagName: String): String? {
        require(XmlPullParser.START_TAG, null, tagName)
        var content: String? = null
        if (next() == XmlPullParser.TEXT) {
            content = text
            nextTag()
            if (eventType != XmlPullParser.END_TAG) {
                logW(logTag, "[readString] Unexpected tag: name = $name, event type = $eventType.")
                skip()
                nextTag()
                content = null
            }
        }
        require(XmlPullParser.END_TAG, null, tagName)
        logD(logTag, "[readString] tag name = $tagName, content = $content")
        return content
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readAttributes(
        tagName: String,
        attributes: List<String>,
        action: (String, String?) -> Unit
    ) {
        require(XmlPullParser.START_TAG, null, tagName)
        attributes.forEach { attr ->
            action(attr, getAttributeValue(null, attr))
        }
        nextTag()
        logD(logTag, "[readAttributes]: tag name = $tagName, attributes = $attributes")
        require(XmlPullParser.END_TAG, null, tagName)
    }

    @Throws(XmlPullParserException::class, IOException::class)
    protected fun XmlPullParser.skip() {
        if (eventType != XmlPullParser.START_TAG) {
            throw IllegalStateException()
        }
        var depth = 1
        while (depth != 0) {
            when (next()) {
                XmlPullParser.END_TAG -> depth--
                XmlPullParser.START_TAG -> depth++
            }
        }
        logW(logTag, "[skip] tag name = $name, depth is $depth.")
    }

    protected fun String.toBoolOrNull(): Boolean? {
        return when (lowercase()) {
            "yes", "true" -> true
            "no", "false" -> false
            else -> null
        }
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readRssStandardChannel(): RssStandardChannelData {
        require(XmlPullParser.START_TAG, null, CHANNEL)

        var title: String? = null
        var description: String? = null
        var image: Image? = null
        var language: String? = null
        val categories = mutableListOf<Category>()
        var link: String? = null
        var copyright: String? = null
        var managingEditor: String? = null
        var webMaster: String? = null
        var pubDate: String? = null
        var lastBuildDate: String? = null
        var generator: String? = null
        var docs: String? = null
        var cloud: Cloud? = null
        var ttl: Int? = null
        var rating: String? = null
        var textInput: TextInput? = null
        var skipHours: List<Int>? = null
        var skipDays: List<String>? = null
        val items = mutableListOf<RssStandardItemData>()

        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (name) {
                TITLE -> title = readString(TITLE)
                DESCRIPTION -> description = readString(DESCRIPTION)
                LINK -> link = readString(LINK)
                IMAGE -> image = readImage()
                LANGUAGE -> language = readString(LANGUAGE)
                CATEGORY -> categories.add(readCategory())
                COPYRIGHT -> copyright = readString(COPYRIGHT)
                MANAGING_EDITOR -> managingEditor = readString(MANAGING_EDITOR)
                WEB_MASTER -> webMaster = readString(WEB_MASTER)
                PUB_DATE -> pubDate = readString(PUB_DATE)
                LAST_BUILD_DATE -> lastBuildDate = readString(LAST_BUILD_DATE)
                GENERATOR -> generator = readString(GENERATOR)
                DOCS -> docs = readString(DOCS)
                CLOUD -> cloud = readCloud()
                TTL -> ttl = readString(TTL)?.toIntOrNull()
                RATING -> rating = readString(RATING)
                TEXT_INPUT -> textInput = readTextInput()
                SKIP_HOURS -> skipHours = readSkipHours()
                SKIP_DAYS -> skipDays = readSkipDays()
                ITEM -> items.add(readRssStandardItem())
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, CHANNEL)
        return RssStandardChannelData(
            title = title,
            description = description,
            image = image,
            language = language,
            categories = if (categories.isEmpty()) null else categories,
            link = link,
            copyright = copyright,
            managingEditor = managingEditor,
            webMaster = webMaster,
            pubDate = pubDate,
            lastBuildDate = lastBuildDate,
            generator = generator,
            docs = docs,
            cloud = cloud,
            ttl = ttl,
            rating = rating,
            textInput = textInput,
            skipHours = skipHours,
            skipDays = skipDays,
            items = if (items.isEmpty()) null else items
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readRssStandardItem(): RssStandardItemData {
        require(XmlPullParser.START_TAG, null, ITEM)
        var title: String? = null
        var enclosure: Enclosure? = null
        var guid: Guid? = null
        var pubDate: String? = null
        var description: String? = null
        var link: String? = null
        var author: String? = null
        val categories: MutableList<Category> = mutableListOf()
        var comments: String? = null
        var source: Source? = null
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            logD(logTag, "[readRssStandardItem] Reading tag name $name.")
            when (this.name) {
                TITLE -> title = readString(TITLE)
                ENCLOSURE -> enclosure = readEnclosure()
                GUID -> guid = readGuid()
                PUB_DATE -> pubDate = readString(PUB_DATE)
                DESCRIPTION -> description = readString(DESCRIPTION)
                LINK -> link = readString(LINK)
                AUTHOR -> author = readString(AUTHOR)
                CATEGORY -> categories.add(readCategory())
                COMMENTS -> comments = readString(COMMENTS)
                SOURCE -> source = readSource()
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, ITEM)
        return RssStandardItemData(
            title = title,
            enclosure = enclosure,
            guid = guid,
            pubDate = pubDate,
            description = description,
            link = link,
            author = author,
            categories = if (categories.isEmpty()) null else categories,
            comments = comments,
            source = source
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readImage(): Image? {
        require(XmlPullParser.START_TAG, null, IMAGE)
        var link: String? = null
        var title: String? = null
        var url: String? = null
        var description: String? = null
        var height: Int? = null
        var width: Int? = null
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            logD(logTag, "[readImage]: RSS 2.0 tag name = $name.")
            when (name) {
                LINK -> link = readString(LINK)
                TITLE -> title = readString(TITLE)
                URL -> url = readString(URL)
                DESCRIPTION -> description = readString(DESCRIPTION)
                HEIGHT -> height = readString(HEIGHT)?.toIntOrNull()
                WIDTH -> width = readString(WIDTH)?.toIntOrNull()
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, IMAGE)
        return Image(
            link = link,
            title = title,
            url = url,
            description = description,
            height = height,
            width = width
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readEnclosure(): Enclosure {
        var url: String? = null
        var length: Long? = null
        var type: String? = null
        readAttributes(ENCLOSURE, listOf(URL, LENGTH, TYPE)) { attr, value ->
            when (attr) {
                URL -> url = value
                LENGTH -> length = value?.toLongOrNull()
                TYPE -> type = value
            }
        }
        return Enclosure(url = url, length = length, type = type)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    private fun XmlPullParser.readCategory(): Category {
        require(XmlPullParser.START_TAG, null, CATEGORY)
        val domain: String? = getAttributeValue(null, DOMAIN)
        val name: String? = readString(tagName = CATEGORY)
        require(XmlPullParser.END_TAG, null, CATEGORY)
        logD(logTag, "[readCategory]: name = $name, domain = $domain")
        return Category(name = name, domain = domain)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readCloud(): Cloud {
        var domain: String? = null
        var port: Int? = null
        var path: String? = null
        var registerProcedure: String? = null
        var protocol: String? = null
        readAttributes(
            CLOUD,
            listOf(DOMAIN, PORT, PATH, REGISTER_PROCEDURE, PROTOCOL)
        ) { attr, value ->
            when (attr) {
                DOMAIN -> domain = value
                PORT -> port = value?.toIntOrNull()
                PATH -> path = value
                REGISTER_PROCEDURE -> registerProcedure = value
                PROTOCOL -> protocol = value
            }
        }
        logD(
            logTag,
            "[readCloud]: domain = $domain, port = $port, path = $path, registerProcedure = $registerProcedure, protocol = $protocol"
        )
        return Cloud(
            domain = domain,
            port = port,
            path = path,
            registerProcedure = registerProcedure,
            protocol = protocol
        )
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readTextInput(): TextInput {
        require(XmlPullParser.START_TAG, null, TEXT_INPUT)
        var title: String? = null
        var description: String? = null
        var name: String? = null
        var link: String? = null
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (this.name) {
                TITLE -> title = readString(TITLE)
                DESCRIPTION -> description = readString(DESCRIPTION)
                NAME -> name = readString(NAME)
                LINK -> link = readString(LINK)
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, TEXT_INPUT)
        logD(
            logTag,
            "[readTextInput]: title = $title, description = $description, name = $name, link = $link"
        )
        return TextInput(title = title, description = description, name = name, link = link)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readSkipHours(): List<Int>? {
        require(XmlPullParser.START_TAG, null, SKIP_HOURS)
        val hours = mutableListOf<Int>()
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (name) {
                HOUR -> readString(HOUR)?.toIntOrNull()?.let { hours.add(it) }
                else -> skip()
            }
        }

        require(XmlPullParser.END_TAG, null, SKIP_HOURS)
        logD(logTag, "[readSkipHours]: hours = $hours")
        return if (hours.isEmpty()) null else hours
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readSkipDays(): List<String>? {
        require(XmlPullParser.START_TAG, null, SKIP_DAYS)
        val days = mutableListOf<String>()
        while (next() != XmlPullParser.END_TAG) {
            if (eventType != XmlPullParser.START_TAG) continue

            when (name) {
                DAY -> readString(DAY)?.let { days.add(it) }
                else -> skip()
            }
        }
        require(XmlPullParser.END_TAG, null, SKIP_DAYS)
        logD(logTag, "[readSkipDays]: days = $days")
        return if (days.isEmpty()) null else days
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readGuid(): Guid {
        require(XmlPullParser.START_TAG, null, GUID)
        val isPermaLink: Boolean? = getAttributeValue(null, PERMALINK)?.toBoolean()
        val value: String? = readString(GUID)
        require(XmlPullParser.END_TAG, null, GUID)
        logD(logTag, "[readGuid] value = $value, isPermaLink = $isPermaLink")
        return Guid(value = value, isPermaLink = isPermaLink)
    }

    @Throws(IOException::class, XmlPullParserException::class)
    protected fun XmlPullParser.readSource(): Source {
        require(XmlPullParser.START_TAG, null, SOURCE)
        val url: String? = getAttributeValue(null, URL)
        val title: String? = readString(SOURCE)
        require(XmlPullParser.END_TAG, null, SOURCE)
        logD(logTag, "[readSource]: title = $title, url = $url")
        return Source(title = title, url = url)
    }
}
