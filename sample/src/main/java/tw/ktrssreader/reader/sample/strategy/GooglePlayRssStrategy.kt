package tw.ktrssreader.reader.sample.strategy

import java.io.Serializable
import java.nio.charset.Charset
import kotlinx.coroutines.flow.Flow
import tw.ktrssreader.Reader
import tw.ktrssreader.kotlin.model.channel.GoogleChannelData

class GooglePlayRssStrategy : RssStrategy {
    override fun read(rssText: String, useCache: Boolean, charset: Charset): Serializable =
        Reader.read<GoogleChannelData>(rssText) {
            this.useCache = useCache
            this.charset = charset
        }

    override suspend fun coRead(
        rssText: String,
        useCache: Boolean,
        charset: Charset
    ): Serializable =
        Reader.coRead<GoogleChannelData>(rssText) {
            this.useCache = useCache
            this.charset = charset
        }

    override suspend fun flowRead(
        rssText: String,
        useCache: Boolean,
        charset: Charset
    ): Flow<Serializable> =
        Reader.flowRead<GoogleChannelData>(rssText) {
            this.useCache = useCache
            this.charset = charset
        }
}
