package com.lightningkite.mirror.archive

import com.lightningkite.kotlinx.reflection.IntReflection
import com.lightningkite.kotlinx.reflection.KxClass
import com.lightningkite.kotlinx.reflection.LongReflection
import com.lightningkite.kotlinx.reflection.StringReflection
import com.lightningkite.kotlinx.serialization.copy

object InMemoryDatabase : Database {

    override fun <T : Model<ID>, ID> table(type: KxClass<T>, name: String): DatabaseTable<T, ID> {
        val idType = type.variables["id"]!!.type
        @Suppress("UNCHECKED_CAST") val generateId: () -> ID = when (idType.base) {
            IntReflection -> {
                var nextId = 0
                { nextId++ as ID }
            }
            LongReflection -> {
                var nextId = 0L
                { nextId++ as ID }
            }
            StringReflection -> {
                var nextId = 0L
                { (nextId++).toString() as ID }
            }
            else -> throw IllegalArgumentException()
        }
        return Table(generateId, {it.copy(type)})
    }

    class Table<T : Model<ID>, ID>(val generateId: () -> ID, val copy: (T)->T) : DatabaseTable<T, ID> {

        val source = HashMap<ID, T>()
//        val listenersById = ConcurrentHashMap<ID, MutableCollection<(ChangeEvent<T, ID>) -> Unit>>()
//        val listenersByFilter = ConcurrentHashMap<ConditionOnItem<T>, MutableCollection<(ChangeEvent<T, ID>) -> Unit>>()

        override suspend fun get(transaction: Transaction, id: ID): T = source[id]!!.let(copy)

        override suspend fun insert(transaction: Transaction, model: T): T {
            val toInsert = model.let(copy)
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            if (toInsert.id == null) {
                toInsert.id = generateId()
            }
            val id = toInsert.id!!
            source[id] = toInsert
//            inform(ChangeEvent(model, ChangeEvent.Type.Insertion))
            return toInsert.let(copy)
        }

        override suspend fun update(transaction: Transaction, model: T): T {
            val toUpdate = model.let(copy)
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            source[model.id!!] = toUpdate
//            inform(ChangeEvent(model, ChangeEvent.Type.Modification))
            return toUpdate.let(copy)
        }

        override suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T {
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            val result = source[id]!!.also { modifications.invoke(it) }
//            inform(ChangeEvent(result, ChangeEvent.Type.Modification))
            return result.let(copy)
        }

        override suspend fun query(
                transaction: Transaction,
                condition: ConditionOnItem<T>,
                sortedBy: List<SortOnItem<T, *>>,
                continuationToken: String?,
                count: Int
        ): QueryResult<T> {
            if (continuationToken != null) TODO()
            return if (sortedBy.isEmpty()) {
                source.values.asSequence().filter { condition.invoke(it) }.take(count).toList().let { list -> QueryResult(list) }
            } else if (sortedBy.size == 1) {
                val sort = sortedBy.first()
                if (sort.ascending) {
                    source.values.asSequence().filter { condition.invoke(it) }.sortedBy {
                        sortedBy.first().field.get.invoke(it) as? Comparable<Comparable<*>>
                                ?: object : Comparable<Comparable<*>> {
                                    override fun compareTo(other: Comparable<*>): Int = if (sort.nullsFirst) -1 else 1
                                }
                    }.take(count).map(copy).toList().let { list -> QueryResult(list) }
                } else {
                    source.values.asSequence().filter { condition.invoke(it) }.sortedByDescending {
                        sortedBy.first().field.get.invoke(it) as? Comparable<Comparable<*>>
                                ?: object : Comparable<Comparable<*>> {
                                    override fun compareTo(other: Comparable<*>): Int = if (sort.nullsFirst) -1 else 1
                                }
                    }.take(count).map(copy).toList().let { list -> QueryResult(list) }
                }
            } else TODO()
        }

        override suspend fun delete(transaction: Transaction, id: ID) {
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            val model = source.remove(id)!!
//            inform(ChangeEvent(model, ChangeEvent.Type.Deletion))
        }

//        private fun inform(event: ChangeEvent<T, ID>) {
//            GlobalScope.launch {
//                listenersById[event.item.id!!]?.invokeAll(event)
//                listenersByFilter.entries.filter { it.key.invoke(event.item) }
//                        .forEach { it.value.invokeAll(event) }
//            }
//        }
    }

}
