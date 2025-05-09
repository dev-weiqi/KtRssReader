package tw.ktrssreader.reader.sample.strategy

import java.io.Serializable
import java.nio.charset.Charset
import kotlinx.coroutines.flow.Flow
import tw.ktrssreader.generated.RssDataReader

class CustomStrategy : RssStrategy {
    override fun read(rssText: String, useCache: Boolean, charset: Charset): Serializable =
        RssDataReader.read(rssText) {
            this.useCache = useCache
            this.charset = charset
        }

    override suspend fun coRead(
        rssText: String,
        useCache: Boolean,
        charset: Charset
    ): Serializable =
        RssDataReader.coRead(rssText) {
            this.useCache = useCache
            this.charset = charset
        }

    override suspend fun flowRead(
        rssText: String,
        useCache: Boolean,
        charset: Charset
    ): Flow<Serializable> =
        RssDataReader.flowRead(rssText) {
            this.useCache = useCache
            this.charset = charset
        }
}
