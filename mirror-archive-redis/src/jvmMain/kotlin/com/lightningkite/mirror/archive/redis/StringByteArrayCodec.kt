package com.lightningkite.mirror.archive.redis

import io.lettuce.core.codec.ByteArrayCodec
import io.lettuce.core.codec.RedisCodec
import io.lettuce.core.codec.StringCodec
import java.nio.ByteBuffer

object StringByteArrayCodec: RedisCodec<String, ByteArray> {
    override fun encodeKey(key: String): ByteBuffer = StringCodec.UTF8.encodeKey(key)
    override fun decodeKey(bytes: ByteBuffer): String = StringCodec.UTF8.decodeKey(bytes)

    override fun encodeValue(value: ByteArray): ByteBuffer = ByteArrayCodec.INSTANCE.encodeValue(value)
    override fun decodeValue(bytes: ByteBuffer): ByteArray = ByteArrayCodec.INSTANCE.decodeValue(bytes)
}