package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.property.SuspendProperty
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.MirrorType
import io.lettuce.core.RedisClient
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.StringFormat
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.json.Json
import kotlinx.serialization.load
import java.io.File


class RedisBinarySuspendProperty<T>(
        val redis: StatefulRedisConnection<String, ByteArray>,
        val serializer: BinaryFormat,
        val key: String,
        val mirror: MirrorType<T>,
        val default: T
): SuspendProperty<T> {


    /**
     * Available options:
     *
     * If source is not present OR is "embedded":
     * - files - Where the database should go, defaults to './build/pg'
     * - clear - If set to `true`, it will clear the files before starting
     * - port - The port to use, defaults to 6379
     *
     * Otherwise, the source should be the hostname or IP address of the Postgres instance:
     * - port - Defaults to 6379
     *
     * */
    companion object FromConfiguration : SuspendProperty.Provider.FromConfiguration {
        override val name: String get() = "RedisBinary"
        override val requiredArguments = arrayOf("source")
        override val optionalArguments = arrayOf(
                "files",
                "clear",
                "port"
        )

        override fun invoke(arguments: Map<String, String>) = Provider(
                redisConnection = {
                    val source = arguments["source"]
                    if (source == null || source == "embedded") {
                        EmbeddedRedis.startWithAutoShutdown(
                                folder = arguments["files"]?.let { File(it) } ?: File("./build/pg"),
                                clearAtStart = arguments["clear"] == "true",
                                port = arguments["port"]?.toInt() ?: 6379
                        )
                    } else {
                        val port = arguments["port"]?.toInt() ?: 6379
                        RedisClient.create("redis://$source:$port")
                    }
                }().connect(StringByteArrayCodec),
                serializer = Cbor
        )
    }


    class Provider(
            val redisConnection: StatefulRedisConnection<String, ByteArray>,
            val serializer: BinaryFormat
    ) : SuspendProperty.Provider {


        override fun <T : Any> get(mirrorClass: MirrorClass<T>, name: String, default: T): SuspendProperty<T> {
            return RedisBinarySuspendProperty(
                    redis = redisConnection,
                    serializer = serializer,
                    key = name,
                    mirror = mirrorClass,
                    default = default
            )
        }
    }


    override suspend fun get(): T {
        val raw = redis.async().get(key).await()
        return if (raw != null)
            serializer.load(mirror, raw)
        else
            default
    }

    override suspend fun set(value: T) {
        redis.async().set(key, serializer.dump(mirror, value)).await()
    }

    override suspend fun compareAndSet(expected: T, value: T): Boolean {
        val expectedString = serializer.dump(mirror, expected)
        val valueString = serializer.dump(mirror, value)
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