package com.lightningkite.kotlinx.server.base.security

import com.lightningkite.kotlinx.persistence.ModificationOnItem
import com.lightningkite.kotlinx.persistence.invoke
import com.lightningkite.kotlinx.reflection.KxClass
import com.lightningkite.kotlinx.reflection.KxVariable
import com.lightningkite.kotlinx.reflection.untyped
import com.lightningkite.kotlinx.server.invoke
import java.util.*

sealed class SecurityAction<T> {

    @Suppress("NOTHING_TO_INLINE")
    inline fun typeless() = this as SecurityAction<*>

    object Allow : SecurityAction<Any?>()
    object Deny : SecurityAction<Any?>()
    object Ignore : SecurityAction<Any?>()
    class Obscure<T>(val value:T) : SecurityAction<T>()
    class Tweak<T>(val value:T) : SecurityAction<T>()

    @Suppress("UNCHECKED_CAST")
    companion object {
        fun <T> allow(): SecurityAction<T> = Allow as SecurityAction<T>
        fun <T> deny(): SecurityAction<T> = Deny as SecurityAction<T>
        fun <T> ignore(): SecurityAction<T> = Ignore as SecurityAction<T>
        fun <T> obscure(value:T): SecurityAction<T> = Obscure(value)
        fun <T> tweak(value:T): SecurityAction<T> = Tweak(value)
    }
}
