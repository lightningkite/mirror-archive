package com.lightningkite.kotlinx.persistence

import com.lightningkite.kotlinx.observable.list.ObservableList
import com.lightningkite.kotlinx.observable.property.ObservableProperty
import com.lightningkite.kotlinx.reflection.*
import org.dizitart.no2.*
import org.dizitart.no2.filters.Filters
import java.lang.IllegalArgumentException

class NitriteDatabase(val nitrite: Nitrite) : Database {
    override fun <T : Model<ID>, ID> table(type: KxClass<T>, name: String): DatabaseTable<T, ID> {
        val collection =  nitrite.getCollection(name)
        @Suppress("UNCHECKED_CAST")
        return Table(
                type = type,
                nitrite =collection,
                generateId = when(type.variables["id"]!!.type.base){
                    IntReflection -> NitriteKeyGenerators.int(collection)
                    LongReflection -> NitriteKeyGenerators.long(collection)
                    StringReflection -> NitriteKeyGenerators.string(collection)
                    else -> throw IllegalArgumentException()
                } as ()->ID
        )
    }

    class Table<T : Model<ID>, ID>(
            val type: KxClass<T>,
            val nitrite: NitriteCollection,
            val generateId:()->ID
    ) : DatabaseTable<T, ID> {

        init{
            if(!nitrite.hasIndex("_id")){
                nitrite.createIndex("_id", IndexOptions.indexOptions(IndexType.Unique))
            }
        }

        override suspend fun get(transaction: Transaction, id: ID): T {
            return NitriteDocumentSerializer.read(type.kxType, nitrite.find(Filters.eq("_id", id)).first()) as T
        }

        override suspend fun insert(transaction: Transaction, model: T): T {
            if(model.id == null) model.id = generateId()
            nitrite.insert(NitriteDocumentSerializer.write(type.kxType, model, Unit) as Document)
            return model
        }

        override suspend fun update(transaction: Transaction, model: T): T {
            nitrite.update(Filters.eq("_id", model.id), NitriteDocumentSerializer.write(type.kxType, model, Unit) as Document)
            return model
        }

        override suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T {
            val raw = nitrite.find(Filters.eq("_id", id)).first()
            val model = NitriteDocumentSerializer.read(type.kxType, raw) as T
            modifications.invoke(model)
            nitrite.update(Filters.eq("_id", model.id), NitriteDocumentSerializer.write(type.kxType, model, Unit) as Document)
            return model
        }

        fun ConditionOnItem<T>.toNitrite(): Filter? = when (this) {
            is ConditionOnItem.Always<T> -> Filters.ALL
            is ConditionOnItem.Never<T> -> TODO()
            is ConditionOnItem.And<T> -> Filters.and(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
            is ConditionOnItem.Or<T> -> Filters.or(*this.conditions.mapNotNull { it.toNitrite() }.toTypedArray())
            is ConditionOnItem.Not<T> -> Filters.not(condition.toNitrite())
            is ConditionOnItem.Equal<T, *> -> Filters.eq(this.field.name, NitriteDocumentSerializer.write(this.field.type, this.value, Unit))
            is ConditionOnItem.EqualToOne<T, *> -> Filters.`in`(this.field.name, *values.map { NitriteDocumentSerializer.write(this.field.type, it, Unit) }.toTypedArray())
            is ConditionOnItem.NotEqual<T, *> -> Filters.not(Filters.eq(this.field.name, NitriteDocumentSerializer.write(this.field.type, this.value, Unit)))
            is ConditionOnItem.LessThan<T, *> -> Filters.lt(this.field.name, NitriteDocumentSerializer.write(this.field.type, this.value, Unit))
            is ConditionOnItem.GreaterThan<T, *> -> Filters.gt(this.field.name, NitriteDocumentSerializer.write(this.field.type, this.value, Unit))
            is ConditionOnItem.LessThanOrEqual<T, *> -> Filters.lte(this.field.name, NitriteDocumentSerializer.write(this.field.type, this.value, Unit))
            is ConditionOnItem.GreaterThanOrEqual<T, *> -> Filters.gte(this.field.name, NitriteDocumentSerializer.write(this.field.type, this.value, Unit))
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
                        NitriteDocumentSerializer.read(type.kxType, it) as T
                    }
                    .toList()
                    .let { QueryResult(it) }
        }

        override suspend fun delete(transaction: Transaction, id: ID) {
            nitrite.remove(Filters.eq("_id", id))
        }

        override suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> {
            for(model in models){
                if(model.id == null) model.id = generateId()
                nitrite.insert(NitriteDocumentSerializer.write(type.kxType, model, Unit) as Document)
            }
            return models
        }
    }

}
