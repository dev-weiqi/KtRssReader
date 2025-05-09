package tw.ktrssreader.reader.sample

import java.io.Serializable
import tw.ktrssreader.annotation.RssAttribute
import tw.ktrssreader.annotation.RssTag

@RssTag(name = "channel")
data class RssData(
    val title: String?,
    @RssTag(name = "item")
    val list: List<RssItem>
) : Serializable

@RssTag(name = "item")
data class RssItem(
    val title: String?,
    val author: String?,
    @RssTag(name = "enclosure")
    val enclosure: RssItemEnclosure?
) : Serializable

@RssTag(name = "enclosure")
data class RssItemEnclosure(
    @RssAttribute("url")
    val url: String?
) : Serializable
