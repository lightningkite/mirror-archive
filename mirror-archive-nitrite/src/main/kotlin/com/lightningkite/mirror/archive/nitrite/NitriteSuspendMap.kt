package com.lightningkite.mirror.archive.nitrite

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapProvider
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.StringSerializer
import org.dizitart.no2.*
import org.dizitart.no2.exceptions.UniqueConstraintException
import org.dizitart.no2.filters.Filters
import java.lang.Exception
import java.lang.UnsupportedOperationException

class NitriteSuspendMap<T : HasId>(
        val registry: SerializationRegistry,
        val classInfo: ClassInfo<T>,
        val nitrite: NitriteCollection,
        val serializer: NitriteDocumentSerializer = NitriteDocumentSerializer(registry)
) : SuspendMap<Id, T> {

    class Provider(val nitrite: Nitrite, val registry: SerializationRegistry): SuspendMapProvider {
        val serializer = NitriteDocumentSerializer(registry)
        @Suppress("UNCHECKED_CAST")
        override fun <K, V : Any> suspendMap(key: Type<K>, value: Type<V>): SuspendMap<K, V> {
            if(key != Id::class.type) throw UnsupportedOperationException()
            if(value.nullable) throw UnsupportedOperationException()
            val info = serializer.registry.classInfoRegistry[value.kClass]!! as ClassInfo<HasId>
            return NitriteSuspendMap(
                    classInfo = info,
                    registry = serializer.registry,
                    serializer = serializer,
                    nitrite = nitrite.getCollection(info.qualifiedName)
            ) as SuspendMap<K, V>
        }
    }

    init {
        if (!nitrite.hasIndex("id")) {
            nitrite.createIndex("id", IndexOptions.indexOptions(IndexType.Unique))
        }
    }

    val idField = classInfo.fields.find { it.name == "id" }!! as FieldInfo<T, Id>

    override suspend fun getNewKey(): Id = Id.key()
    override suspend fun get(key: Id): T? {
        return nitrite.find(Filters.eq("id", key.toUUIDString())).firstOrNull()?.let { serializer.decode(it, classInfo.kClass.type) }
    }

    override suspend fun put(key: Id, value: T, conditionIfExists: Condition<T>, create: Boolean): Boolean {
        var encoded: Document? = null
        serializer.encode({
            encoded = it as Document
        }, value, classInfo.kClass.type)
        val condition = Condition.Field(idField, Condition.Equal(key)) and conditionIfExists
        try {
            nitrite.insert(encoded!!)
            return true
        } catch(e:UniqueConstraintException) {
            val res = nitrite.update(condition.toNitrite(), encoded!!, UpdateOptions.updateOptions(false, true))
            return res?.affectedCount?.let { it > 0 } ?: false
        }
    }

    override suspend fun modify(key: Id, operation: Operation<T>, condition: Condition<T>): T? = throw UnsupportedOperationException()

    override suspend fun remove(key: Id, condition: Condition<T>): Boolean {
        return nitrite.remove(Filters.eq("id", key.toUUIDString())).affectedCount > 0
    }

    override suspend fun query(condition: Condition<T>, sortedBy: Sort<T>?, after: Pair<Id, T>?, count: Int): List<Pair<Id, T>> {
        val options = when (sortedBy) {
            is Sort.Field<*, *> -> FindOptions(
                    sortedBy.field.name,
                    if (sortedBy.ascending) SortOrder.Ascending else SortOrder.Descending
            ).thenLimit(0, count)
            null -> FindOptions.limit(0, count)
            else -> throw IllegalArgumentException()
        }

        val fullCondition = if (after == null) condition
        else if (sortedBy != null) sortedBy.after(item = after.second) and condition
        else Condition.Field(idField, Condition.GreaterThan(after.first)) and condition
        return nitrite.find(fullCondition.toNitrite(), options)
                .map {
                    serializer.decode(it, classInfo.kClass.type).let{ it.id to it }
                }
    }

    @Suppress("UNCHECKED_CAST")
    fun Condition<*>.toNitrite(field: String = "value", type: Type<*> = classInfo.type): Filter? = when (this) {
        is Condition.Always<*> -> Filters.ALL
        is Condition.Never<*> -> throw UnsupportedOperationException()
        is Condition.And<*> -> Filters.and(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
        is Condition.Or<*> -> Filters.or(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
        is Condition.Not<*> -> Filters.not(condition.toNitrite())
        is Condition.Field<*, *> -> this.condition.toNitrite(field = this.field.name, type = this.field.type)
        is Condition.Equal<*> -> Filters.eq(field, serializer.encode(this.value, type as Type<Any?>))
        is Condition.EqualToOne<*> -> Filters.`in`(field, *values.map { serializer.encode(it, type as Type<Any?>) }.toTypedArray())
        is Condition.NotEqual<*> -> Filters.not(Filters.eq(field, serializer.encode(this.value, type as Type<Any?>)))
        is Condition.LessThan<*> -> Filters.lt(field, serializer.encode(this.value, type as Type<Any?>))
        is Condition.GreaterThan<*> -> Filters.gt(field, serializer.encode(this.value, type as Type<Any?>))
        is Condition.LessThanOrEqual<*> -> Filters.lte(field, serializer.encode(this.value, type as Type<Any?>))
        is Condition.GreaterThanOrEqual<*> -> Filters.gte(field, serializer.encode(this.value, type as Type<Any?>))
        is Condition.TextSearch<*> -> Filters.text(field, this.query)
        is Condition.RegexTextSearch<*> -> Filters.regex(field, this.query.pattern)
    }
}