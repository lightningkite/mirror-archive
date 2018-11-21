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
    override fun <T : Model<ID>, ID> table(type: KClass<T>, name: String): Database.Table<T, ID> {
        val collection = nitrite.getCollection(name)
        val classInfo = registry.classInfoRegistry[type]!!
        @Suppress("UNCHECKED_CAST")
        return Table(
                type = classInfo,
                nitrite = collection,
                serializer = serializer,
                generateId = when (classInfo.fields.find { it.name == "id" }!!.type.kClass) {
                    Int::class -> NitriteKeyGenerators.int(collection)
                    Long::class -> NitriteKeyGenerators.long(collection)
                    String::class -> NitriteKeyGenerators.string(collection)
                    else -> throw IllegalArgumentException()
                } as () -> ID
        )
    }

    class Table<T : Model<ID>, ID>(
            val type: ClassInfo<T>,
            val nitrite: NitriteCollection,
            val serializer: NitriteDocumentSerializer,
            val generateId: () -> ID
    ) : Database.Table<T, ID> {

        init {
            if (!nitrite.hasIndex("_id")) {
                nitrite.createIndex("_id", IndexOptions.indexOptions(IndexType.Unique))
            }
        }

        override suspend fun get(transaction: Transaction, id: ID): T {
            return serializer.decode(nitrite.find(Filters.eq("_id", id)).first(), type.kClass.type)
        }

        override suspend fun insert(transaction: Transaction, model: T): T {
            if (model.id == null) model.id = generateId()
            serializer.encode({
                nitrite.insert(it as Document)
            }, model, type.kClass.type)
            return model
        }

        override suspend fun update(transaction: Transaction, model: T): T {
            serializer.encode({
                nitrite.update(Filters.eq("_id", model.id), it as Document)
            }, model, type.kClass.type)
            return model
        }

        override suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T {
            val raw = nitrite.find(Filters.eq("_id", id)).first()
            val model = serializer.decode(raw, type.kClass.type)
            val changed = model.apply(type, modifications)
            serializer.encode({
                nitrite.update(Filters.eq("_id", model.id), it as Document)
            }, changed, type.kClass.type)
            return model
        }

        fun ConditionOnItem<T>.toNitrite(): Filter? = when (this) {
            is ConditionOnItem.Always<T> -> Filters.ALL
            is ConditionOnItem.Never<T> -> TODO()
            is ConditionOnItem.And<T> -> Filters.and(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
            is ConditionOnItem.Or<T> -> Filters.or(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
            is ConditionOnItem.Not<T> -> Filters.not(condition.toNitrite())
            is ConditionOnItem.Equal<T, *> -> Filters.eq(this.field.name, serializer.encode<Any?>(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.EqualToOne<T, *> -> Filters.`in`(this.field.name, *values.map { serializer.encode<Any?>(it, this.field.type as Type<Any?>) }.toTypedArray())
            is ConditionOnItem.NotEqual<T, *> -> Filters.not(Filters.eq(this.field.name, serializer.encode<Any?>(this.value, this.field.type as Type<Any?>)))
            is ConditionOnItem.LessThan<T, *> -> Filters.lt(this.field.name, serializer.encode<Any?>(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.GreaterThan<T, *> -> Filters.gt(this.field.name, serializer.encode<Any?>(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.LessThanOrEqual<T, *> -> Filters.lte(this.field.name, serializer.encode<Any?>(this.value, this.field.type as Type<Any?>))
            is ConditionOnItem.GreaterThanOrEqual<T, *> -> Filters.gte(this.field.name, serializer.encode<Any?>(this.value, this.field.type as Type<Any?>))
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

        override suspend fun delete(transaction: Transaction, id: ID) {
            nitrite.remove(Filters.eq("_id", id))
        }

        override suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> {
            for (model in models) {
                if (model.id == null) model.id = generateId()
                nitrite.insert(serializer.encode(model, type.kClass.type) as Document)
            }
            return models
        }
    }

}
