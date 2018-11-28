package com.lightningkite.mirror.archive

import com.lightningkite.lokalize.TimeStamp
import com.lightningkite.lokalize.now
import kotlin.random.Random

@Suppress("EXPERIMENTAL_API_USAGE")
data class Id(
        val mostSignificantBits: ULong,
        val leastSignificantBits: ULong
) : Comparable<Id> {

    val mostSignificantLong: Long get() = mostSignificantBits.toLong()
    val leastSignificantLong: Long get() = leastSignificantBits.toLong()

    override fun compareTo(other: Id): Int {
        return if (this.mostSignificantBits == other.mostSignificantBits)
            this.leastSignificantBits.compareTo(other.leastSignificantBits)
        else this.mostSignificantBits.compareTo(other.mostSignificantBits)
    }

    companion object {
        fun fromLongs(mostSignificantLong: Long, leastSignificantLong: Long) = Id(
                mostSignificantBits = mostSignificantLong.toULong(),
                leastSignificantBits = leastSignificantLong.toULong()
        )
        fun fromUUIDString(input: String): Id {
            val hexOnly = input.filter { it in '0'..'9' || it in 'a'..'f' || it in 'A'..'F' }
            if(hexOnly.length != 32) throw IllegalArgumentException("String is not a Id, should be 32 hex characters")
            return Id(
                    mostSignificantBits = hexOnly.substring(0, 16).toULong(16),
                    leastSignificantBits = hexOnly.substring(16, 32).toULong(16)
            )
        }
        fun randomUUID4(): Id = Id(
                mostSignificantBits = (Random.nextLong() and (-1L - 0x000000000000F000) or (0x4000)).toULong(),
                leastSignificantBits = (Random.nextLong() and 0x3FFFFFFFFFFFFFFF or (0x8 shl 60)).toULong()
        )
        fun key(): Id = Id(
                mostSignificantBits = TimeStamp.now().millisecondsSinceEpoch.toULong(),
                leastSignificantBits = Random.nextLong().toULong()
        )
    }

    fun toUUIDString(): String {
        val first = mostSignificantBits.toString(16).padStart(16, '0')
        val second = leastSignificantBits.toString(16).padStart(16, '0')
        return buildString(36) {
            append(first.substring(0, 8))
            append("-")
            append(first.substring(8, 12))
            append("-")
            append(first.substring(12, 16))
            append("-")
            append(second.substring(0, 4))
            append("-")
            append(second.substring(4, 16))
        }
    }
}