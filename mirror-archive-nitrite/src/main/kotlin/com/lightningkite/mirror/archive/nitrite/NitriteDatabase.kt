package com.lightningkite.mirror.archive.nitrite

import com.lightningkite.mirror.archive.*
import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.serialization.SerializationRegistry
import org.dizitart.no2.*
import org.dizitart.no2.filters.Filters
import kotlin.reflect.KClass

class NitriteTable<T: HasId>(
        val registry: SerializationRegistry,
        val classInfo: ClassInfo<T>,
        val nitrite: NitriteCollection,
        val serializer: NitriteDocumentSerializer = NitriteDocumentSerializer(registry)
): SuspendMap<Id, T> {

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
        var result: WriteResult? = null
        serializer.encode({
            val condition = Condition.Field(idField, Condition.Equal(key)) and conditionIfExists
            result = nitrite.update(condition.toNitrite(), it as Document, UpdateOptions.updateOptions(true, true))
        }, value, classInfo.kClass.type)
        return result?.affectedCount?.let{ it > 0 } ?: false
    }

    override suspend fun remove(key: Id, condition: Condition<T>): Boolean {
        return nitrite.remove(Filters.eq("id", key.toUUIDString())).affectedCount > 0
    }

    override suspend fun query(condition: Condition<T>, sortedBy: Sort<T>, after: T?, count: Int): List<T> {
        if(sortedBy !is Sort.DontCare) throw UnsupportedOperationException()
        if(after != null) throw UnsupportedOperationException()
        return nitrite.find(condition.toNitrite())
                .asSequence()
                .take(count)
                .map {
                    serializer.decode(it, classInfo.kClass.type)
                }
                .toList()
    }

    @Suppress("UNCHECKED_CAST")
    fun Condition<*>.toNitrite(field: String = "value", type: Type<*> = classInfo.type): Filter? = when (this) {
        is Condition.Always<*> -> Filters.ALL
        is Condition.Never<*> -> TODO()
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