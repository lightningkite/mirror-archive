package com.lightningkite.mirror.archive.nitrite

import com.lightningkite.mirror.archive.*
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.SerializationRegistry
import org.dizitart.no2.*
import org.dizitart.no2.filters.Filters
import java.lang.IllegalArgumentException
import kotlin.reflect.KClass

class NitriteDatabase(
        val nitrite: Nitrite,
        override val registry: SerializationRegistry,
        val serializer: NitriteDocumentSerializer = NitriteDocumentSerializer(registry)
) : Database {
    override fun <T : HasId> table(type: KClass<T>, name: String): Database.Table<T> {
        val collection = nitrite.getCollection(name)
        val classInfo = registry.classInfoRegistry[type]!!
        @Suppress("UNCHECKED_CAST")
        return Table(
                type = classInfo,
                nitrite = collection,
                serializer = serializer
        )
    }

    class Table<T : HasId>(
            val type: ClassInfo<T>,
            val nitrite: NitriteCollection,
            val serializer: NitriteDocumentSerializer
    ) : Database.Table<T> {

        init {
            if (!nitrite.hasIndex("id")) {
                nitrite.createIndex("id", IndexOptions.indexOptions(IndexType.Unique))
            }
        }

        override suspend fun get(transaction: Transaction, id: Id): T? {
            return nitrite.find(Filters.eq("id", id.toUUIDString())).firstOrNull()?.let{serializer.decode(it, type.kClass.type)}
        }

        override suspend fun insert(transaction: Transaction, model: T): T {
            serializer.encode({
                nitrite.insert(it as Document)
            }, model, type.kClass.type)
            return model
        }

        override suspend fun update(transaction: Transaction, model: T): T {
            serializer.encode({
                nitrite.update(Filters.eq("id", model.id.toUUIDString()), it as Document)
            }, model, type.kClass.type)
            return model
        }

        override suspend fun modify(transaction: Transaction, id: Id, modifications: List<ModificationOnItem<T, *>>): T {
            val raw = nitrite.find(Filters.eq("id", id.toUUIDString())).first()
            val model = serializer.decode(raw, type.kClass.type)
            val changed = model.apply(type, modifications)
            serializer.encode({
                nitrite.update(Filters.eq("id", model.id.toUUIDString()), it as Document)
            }, changed, type.kClass.type)
            return model
        }

        @Suppress("UNCHECKED_CAST")
        fun ConditionOnItem<T>.toNitrite(): Filter? = when (this) {
            is ConditionOnItem.Always<T> -> Filters.ALL
            is ConditionOnItem.Never<T> -> TODO()
            is ConditionOnItem.And<T> -> Filters.and(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
            is ConditionOnItem.Or<T> -> Filters.or(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
            is ConditionOnItem.Not<T> -> Filters.not(condition.toNitrite())
            is ConditionOnItem.Equal<T, *> -> Filters.eq(this.field.name, serializer.encode(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.EqualToOne<T, *> -> Filters.`in`(this.field.name, *values.map { serializer.encode(it, this.field.type as Type<Any?>) }.toTypedArray())
            is ConditionOnItem.NotEqual<T, *> -> Filters.not(Filters.eq(this.field.name, serializer.encode(this.value, this.field.type as Type<Any?>)))
            is ConditionOnItem.LessThan<T, *> -> Filters.lt(this.field.name, serializer.encode(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.GreaterThan<T, *> -> Filters.gt(this.field.name, serializer.encode(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.LessThanOrEqual<T, *> -> Filters.lte(this.field.name, serializer.encode(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.GreaterThanOrEqual<T, *> -> Filters.gte(this.field.name, serializer.encode(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.TextSearch<T, *> -> Filters.text(this.field.name, this.query)
            is ConditionOnItem.RegexTextSearch<T, *> -> Filters.regex(this.field.name, this.query.pattern)
        }

        override suspend fun query(
                transaction: Transaction,
                condition: ConditionOnItem<T>,
                sortedBy: List<SortOnItem<T, *>>,
                continuationToken: String?,
                count: Int
        ): QueryResult<T> {
            return nitrite.find(condition.toNitrite())
                    .asSequence()
                    .take(count)
                    .map {
                        serializer.decode(it, type.kClass.type)
                    }
                    .toList()
                    .let { QueryResult(it) }
        }

        override suspend fun delete(transaction: Transaction, id: Id) {
            nitrite.remove(Filters.eq("id", id.toUUIDString()))
        }

        override suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> {
            for (model in models) {
                nitrite.insert(serializer.encode(model, type.kClass.type) as Document)
            }
            return models
        }
    }

}
