package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.list.SuspendList
import com.lightningkite.mirror.archive.model.HasId
import com.lightningkite.mirror.info.MirrorClass
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.StringFormat


class RedisBinarySuspendList<T: Any>(
        val redis: StatefulRedisConnection<String, ByteArray>,
        val serializer: BinaryFormat,
        val key: String,
        val mirror: MirrorClass<T>
): SuspendList<T> {
    override suspend fun pushStart(item: T) {
        redis.async().lpush(key, serializer.dump(mirror, item)).await()
    }

    override suspend fun pushEnd(item: T) {
        redis.async().rpush(key, serializer.dump(mirror, item)).await()
    }

    override suspend fun popStart(): T? {
        val raw = redis.async().lpop(key).await()
        return if (raw != null)
            serializer.load(mirror, raw)
        else
            null
    }

    override suspend fun popEnd(): T? {
        val raw = redis.async().rpop(key).await()
        return if (raw != null)
            serializer.load(mirror, raw)
        else
            null
    }

    override suspend fun get(index: Int): T? {
        val raw = redis.async().lindex(key, index.toLong()).await()
        return if (raw != null)
            serializer.load(mirror, raw)
        else
            null
    }

    override suspend fun set(index: Int, value: T) {
        redis.async().lset(key, index.toLong(), serializer.dump(mirror, value)).await()
    }

    override suspend fun size(): Int {
        return redis.async().llen(key).await().toInt()
    }

    override suspend fun trim(range: IntRange) {
        redis.async().ltrim(key, range.start.toLong(), range.endInclusive.toLong()).await()
    }

    override suspend fun getRange(range: IntRange): List<T> {
        return redis.async().lrange(key, range.start.toLong(), range.endInclusive.toLong()).await().map {
            serializer.load(mirror, it)
        }
    }
}