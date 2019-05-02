package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.list.SuspendList
import com.lightningkite.mirror.archive.model.HasId
import com.lightningkite.mirror.archive.orderedset.SuspendOrderedSet
import com.lightningkite.mirror.info.MirrorClass
import io.lettuce.core.Range
import io.lettuce.core.ScoredValue
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.StringFormat


class RedisBinarySuspendOrderedSet<T: Any>(
        val redis: StatefulRedisConnection<String, ByteArray>,
        val serializer: BinaryFormat,
        val key: String,
        val mirror: MirrorClass<T>,
        val getZ: (T)->Double
): SuspendOrderedSet<T> {
    override suspend fun add(value: T) {
        redis.async().zadd(key, ScoredValue.fromNullable(value.let(getZ), serializer.dump(mirror, value))).await()
    }

    override suspend fun remove(value: T): Boolean {
        return redis.async().zrem(key, serializer.dump(mirror, value)).await() > 0
    }

    override suspend fun size(): Int {
        return redis.async().zcard(key).await().toInt()
    }

    override suspend fun popMax(): T? {
        val raw = redis.async().zpopmax(key).await().takeIf { it.hasValue() }?.value
        return if (raw != null)
            serializer.load(mirror, raw)
        else
            null
    }

    override suspend fun popMin(): T? {
        val raw = redis.async().zpopmin(key).await().takeIf { it.hasValue() }?.value
        return if (raw != null)
            serializer.load(mirror, raw)
        else
            null
    }

    override suspend fun popMax(count: Int): List<T> {
        return redis.async().zpopmax(key, count.toLong()).await().map {
            serializer.load(mirror, it.value)
        }
    }

    override suspend fun popMin(count: Int): List<T> {
        return redis.async().zpopmin(key, count.toLong()).await().map {
            serializer.load(mirror, it.value)
        }
    }
}