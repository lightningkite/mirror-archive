package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.reflection.KxClass
import com.lightningkite.kotlinx.reflection.KxField
import com.lightningkite.kotlinx.reflection.KxVariable
import java.util.*

/**
 * Returns the primary key of the type.
 */
fun <T : Any> KxClass<T>.primaryKey(): KxVariable<T, *> = variables["id"]!!


/**
 * Returns the variables of a class in order.
 */
@Suppress("UNCHECKED_CAST")
val <T : Any> KxClass<T>.orderedAuthenticVariables: List<KxVariable<T, *>>
    get() = KxClass_orderedVariables.getOrPut(this) {
        variables.values.sortedBy { it.name }.filter { !it.artificial }
    } as List<KxVariable<T, *>>
private val KxClass_orderedVariables = WeakHashMap<KxClass<*>, List<KxVariable<*, *>>>()