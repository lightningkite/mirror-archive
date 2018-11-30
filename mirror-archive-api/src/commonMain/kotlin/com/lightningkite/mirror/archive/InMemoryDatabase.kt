package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.copy
import kotlin.reflect.KClass

class InMemoryDatabase(override val registry: SerializationRegistry) : Database {

    override fun <T : HasId> table(type: KClass<T>, name: String): Database.Table<T> {
        val classInfo = registry.classInfoRegistry[type]!!
        return Table(classInfo)
    }

    class Table<T : HasId>(override val classInfo: ClassInfo<T>) : Database.Table<T> {

        val source = HashMap<Id, T>()
//        val listenersById = ConcurrentHashMap<Id, MutableCollection<(ChangeEvent<T, Id>) -> Unit>>()
//        val listenersByFilter = ConcurrentHashMap<ConditionOnItem<T>, MutableCollection<(ChangeEvent<T, Id>) -> Unit>>()

        override suspend fun get(transaction: Transaction, id: Id): T = source[id]!!.copy(classInfo)

        override suspend fun insert(transaction: Transaction, model: T): T {
            val toInsert = model.copy(classInfo)
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            source[toInsert.id] = toInsert
//            inform(ChangeEvent(model, ChangeEvent.Type.Insertion))
            return toInsert.copy(classInfo)
        }

        override suspend fun update(transaction: Transaction, model: T): T {
            val toUpdate = model.copy(classInfo)
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            source[model.id] = toUpdate
//            inform(ChangeEvent(model, ChangeEvent.Type.Modification))
            return toUpdate.copy(classInfo)
        }

        override suspend fun modify(transaction: Transaction, id: Id, modifications: List<ModificationOnItem<T, *>>): T {
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            val result = source[id]!!.apply(classInfo, modifications)
//            inform(ChangeEvent(result, ChangeEvent.Type.Modification))
            return result.copy(classInfo)
        }

        @Suppress("UNCHECKED_CAST")
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
                    }.take(count).map{ it.copy(classInfo) }.toList().let { list -> QueryResult(list) }
                } else {
                    source.values.asSequence().filter { condition.invoke(it) }.sortedByDescending {
                        sortedBy.first().field.get.invoke(it) as? Comparable<Comparable<*>>
                                ?: object : Comparable<Comparable<*>> {
                                    override fun compareTo(other: Comparable<*>): Int = if (sort.nullsFirst) -1 else 1
                                }
                    }.take(count).map{ it.copy(classInfo) }.toList().let { list -> QueryResult(list) }
                }
            } else TODO()
        }

        override suspend fun delete(transaction: Transaction, id: Id) {
            if(transaction.readOnly) throw IllegalArgumentException("Transaction must be writeable")
            val model = source.remove(id)!!
//            inform(ChangeEvent(model, ChangeEvent.Type.Deletion))
        }

//        private fun inform(event: ChangeEvent<T, Id>) {
//            GlobalScope.launch {
//                listenersById[event.item.id!!]?.invokeAll(event)
//                listenersByFilter.entries.filter { it.key.invoke(event.item) }
//                        .forEach { it.value.invokeAll(event) }
//            }
//        }
    }

}
