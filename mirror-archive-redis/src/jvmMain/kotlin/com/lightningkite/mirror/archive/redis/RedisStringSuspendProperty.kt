package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.MirrorType
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlinx.serialization.StringFormat


class RedisStringSuspendProperty<T>(
        val redis: StatefulRedisConnection<String, String>,
        val serializer: StringFormat,
        val key: String,
        val mirror: MirrorType<T>,
        val default: T
): SuspendProperty<T> {
    override suspend fun get(): T {
        val raw = redis.async().get(key).await()
        return if (raw != null)
            serializer.parse(mirror, raw)
        else
            default
    }

    override suspend fun set(value: T) {
        redis.async().set(key, serializer.stringify(mirror, value)).await()
    }

    override suspend fun compareAndSet(expected: T, value: T): Boolean {
        val expectedString = serializer.stringify(mirror, expected)
        val valueString = serializer.stringify(mirror, value)
        return 0L != redis.async().eval<Long>("""
            local value = redis.call('get', KEYS[1])
            if value == ARGV[1] then
                redis.call('set', KEYS[1], ARGV[2])
                return 1
            else
                return 0
            end
        """.trimIndent(), ScriptOutputType.INTEGER, arrayOf(key), expectedString, valueString).await()
    }
}