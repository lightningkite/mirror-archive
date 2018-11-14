//package com.lightningkite.mirror.archive
//
//import com.lightningkite.kotlinx.async.DelayedResultFunction
//import com.lightningkite.kotlinx.async.background
//import com.lightningkite.kotlinx.async.immediate
//import com.lightningkite.kotlinx.async.map
//import com.lightningkite.kotlinx.collection.ConcurrentHashMap
//import com.lightningkite.kotlinx.lambda.invokeAll
//
//class CacheDatabaseTable<T : Model<ID>, ID>(val underlying: DatabaseTable<T, ID>) : DatabaseTable<T, ID> {
//
//    val source = HashMap<ID, T>()
//    val listenersById = ConcurrentHashMap<ID, MutableCollection<(ChangeEvent<T, ID>)->Unit>>()
//    val listenersByFilter = ConcurrentHashMap<ConditionOnItem<T>, MutableCollection<(ChangeEvent<T, ID>)->Unit>>()
//
//    override fun get(transaction: Transaction, id: ID): DelayedResultFunction<T> = source[id]?.let{
//        immediate { it }
//    } ?: run {
//        underlying.get(transaction, id)
//                .map {
//                    source[id] = it
//                    it
//                }
//    }
//
//    override fun insert(transaction: Transaction, model: T): DelayedResultFunction<T> = underlying.insert(transaction, model)
//
//    override fun update(transaction: Transaction, model: T):DelayedResultFunction<T> = immediate {
//        source[model.id!!] = model
//        inform(ChangeEvent(model, ChangeEvent.Type.Modification))
//        model
//    }
//
//    override fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): DelayedResultFunction<T> = immediate {
//        val result = source[id]!!.also { modifications.invoke(it) }
//        inform(ChangeEvent(result, ChangeEvent.Type.Modification))
//        result
//    }
//
//    override fun query(
//            transaction: Transaction,
//            condition: ConditionOnItem<T>,
//            sortedBy: List<SortOnItem<T, *>>,
//            continuationToken: String?,
//            count: Int
//    ): DelayedResultFunction<QueryResult<T>> = immediate {
//        if(continuationToken != null) TODO()
//        if(sortedBy.isEmpty()){
//            source.values.asSequence().filter { condition.invoke(it) }.take(count).toList().let{ list -> QueryResult(list) }
//        } else if(sortedBy.size == 1){
//            val sort = sortedBy.first()
//            if(sort.ascending){
//                source.values.asSequence().filter { condition.invoke(it) }.sortedBy {
//                    sortedBy.first().field.get.invoke(it) as? Comparable<Comparable<*>> ?: object : Comparable<Comparable<*>> {
//                        override fun compareTo(other: Comparable<*>): Int = if(sort.nullsFirst) -1 else 1
//                    }
//                }.take(count).toList().let{ list -> QueryResult(list) }
//            } else {
//                source.values.asSequence().filter { condition.invoke(it) }.sortedByDescending {
//                    sortedBy.first().field.get.invoke(it) as? Comparable<Comparable<*>> ?: object : Comparable<Comparable<*>> {
//                        override fun compareTo(other: Comparable<*>): Int = if(sort.nullsFirst) -1 else 1
//                    }
//                }.take(count).toList().let{ list -> QueryResult(list) }
//            }
//        } else TODO()
//    }
//
//    override fun delete(transaction: Transaction, id: ID): DelayedResultFunction<Unit> = immediate {
//        val model = source.remove(id)!!
//        inform(ChangeEvent(model, ChangeEvent.Type.Deletion))
//        Unit
//    }
//
//    override fun listen(id: ID): MutableCollection<(ChangeEvent<T, ID>) -> Unit>
//            = listenersById.getOrPut(id){ ArrayList() }
//
//    override fun listen(condition: ConditionOnItem<T>): MutableCollection<(ChangeEvent<T, ID>) -> Unit>
//            = listenersByFilter.getOrPut(condition){ ArrayList() }
//
//    fun inform(event: ChangeEvent<T, ID>){
//        background {
//            listenersById[event.item.id!!]?.invokeAll(event)
//            listenersByFilter.entries.filter { it.key.invoke(event.item) }
//                    .forEach { it.value.invokeAll(event) }
//        }.invoke {  }
//    }
//}