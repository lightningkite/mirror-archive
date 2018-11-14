package com.lightningkite.mirror.archive

/**
 * A key for obtaining a model.
 * You may be wondering why it uses an [Any] on the inside.
 * That's because Kotlin doesn't support using generics in inline classes yet.
 * Not quite sure why.
 * We can keep it type safe by simply never creating one of these ourselves.
 */
inline class Reference<MODEL : Model<ID>, ID : Any> @Deprecated("DO NOT USE THIS DIRECTLY.  Instead, use the other constructor.") constructor(val untypedId: Any/*ID*/) {
    @Suppress("UNCHECKED_CAST")
    val id: ID
        get() = untypedId as ID
    companion object {
        @Suppress("DEPRECATION")
        fun <MODEL : Model<ID>, ID : Any> make(key: ID): Reference<MODEL, ID> = Reference(key)
    }
}

fun <MODEL : Model<ID>, ID : Any> MODEL.key(): Reference<MODEL, ID>? = id?.let { Reference.make(it) }