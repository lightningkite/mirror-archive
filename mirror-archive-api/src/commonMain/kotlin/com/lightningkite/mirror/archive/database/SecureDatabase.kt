//package com.lightningkite.mirror.archive.database
//
//import com.lightningkite.kommon.exception.ForbiddenException
//import com.lightningkite.mirror.archive.model.Condition
//import com.lightningkite.mirror.archive.model.Operation
//import com.lightningkite.mirror.archive.model.Sort
//import com.lightningkite.mirror.archive.model.and
//import com.lightningkite.mirror.info.MirrorClass
//
//class SecureDatabase<T : Any, USER>(
//        val classInfo: ClassInfo<T>,
//        val underlying: Database<T>,
//        val readable: (USER) -> Condition<T>,
//        val writable: (USER) -> Condition<T>,
//        val fieldRules: Map<MirrorClass.Field<T, *>, FieldRule<T, *, USER>>
//) {
//    data class FieldRule<T : Any, V, USER>(
//            val fieldInfo: MirrorClass.Field<T, V>,
//            val queryable: (USER) -> Boolean,
//            val mask: (USER, V) -> V,
//            val validate: (USER, V) -> V,
//            val operation: (USER, Operation<V>) -> Operation<V>
//    )
//
//    fun provide(user: USER): Database<T> {
//        return object : Database<T> {
//
//            fun Sort<T>.safety(): Sort<T>? {
//                return when (this) {
//                    is Sort.Natural -> this
//                    is Sort.Field<*, *> -> if (fieldRules[this.field as MirrorClass.Field<T, *>]?.queryable?.invoke(user) != false) this else null
//                    is Sort.Multi -> Sort.Multi(this.comparators.mapNotNull { it.safety() })
//                    else -> throw UnsupportedOperationException()
//                }
//            }
//
//            fun Condition<T>.safety(): Condition<T> {
//                return when (this) {
//                    is Condition.Not -> Condition.Not(condition.safety())
//                    is Condition.And -> Condition.And(conditions.map { it.safety() })
//                    is Condition.Or -> Condition.Or(conditions.map { it.safety() })
//                    is Condition.Field<*, *> -> {
//                        if (fieldRules[this.field as MirrorClass.Field<T, *>]?.queryable?.invoke(user) != false) this
//                        else throw ForbiddenException("User is not permitted to query by the field '${field.name}'.")
//                    }
//                    else -> this
//                }
//            }
//
//            @Suppress("UNCHECKED_CAST")
//            fun Operation<T>.safety(): Operation<T> {
//                return when (this) {
//                    is Operation.Multiple -> Operation.Multiple(operations.map { it.safety() })
//                    is Operation.Field<*, *> -> fieldRules[this.field as MirrorClass.Field<T, *>]?.operation?.let { opMod ->
//                        val untypedOpMod = opMod as (USER, Operation<Any?>) -> Operation<Any?>
//                        Operation.Field(field as MirrorClass.Field<T, Any?>, untypedOpMod.invoke(user, operation as Operation<Any?>))
//                    } ?: this
//                    else -> this
//                }
//            }
//
//            fun T.mask(): T {
//                val parts = this.toAttributeHashMap(classInfo)
//                for ((field, rule) in fieldRules) {
//                    @Suppress("UNCHECKED_CAST") val untypedRule = rule as FieldRule<T, Any?, USER>
//                    parts[field.name] = untypedRule.mask(user, parts[field.name])
//                }
//                return classInfo.construct(parts)
//            }
//
//            fun T.validate(): T {
//                val parts = this.toAttributeHashMap(classInfo)
//                for ((field, rule) in fieldRules) {
//                    @Suppress("UNCHECKED_CAST") val untypedRule = rule as FieldRule<T, Any?, USER>
//                    parts[field.name] = untypedRule.validate(user, parts[field.name])
//                }
//                return classInfo.construct(parts)
//            }
//
//            override suspend fun get(condition: Condition<T>, sort: Sort<T>?, count: Int, after: T?): List<T> {
//                return underlying.get(
//                        condition = condition.safety() and readable(user),
//                        sort = sort?.safety(),
//                        count = count,
//                        after = after
//                ).map { it.mask() }
//            }
//
//            override suspend fun insert(values: List<T>): List<T?> {
//                return underlying.insert(
//                        values = values.map { it.validate() }
//                ).map { it?.mask() }
//            }
//
//            override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
//                return underlying.update(
//                        condition = condition.safety() and writable(user),
//                        operation = operation.safety()
//                )
//            }
//
//            override suspend fun delete(condition: Condition<T>): Int {
//                return underlying.delete(
//                        condition = condition.safety() and writable(user)
//                )
//            }
//
//        }
//    }
//}