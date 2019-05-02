package com.lightningkite.mirror.archive.model

//typealias Subgraph = Map<Reference<*>, HasUuid>
//typealias MutableSubgraph = MutableMap<Reference<*>, HasUuid>
//
//
//fun <T: HasUuid> MutableSubgraph.add(reference: Reference<T>, item: T) = this.put(reference, item)
//operator fun <T: HasUuid> Subgraph.get(reference: Reference<T>): T? = this[reference] as? T
//
//data class TestObject(override val id: Uuid) : HasUuid {
//
//}
//
//fun test(graph: Subgraph){
//    val get: TestObject = graph[Reference<TestObject>(Uuid.randomUUID4())]
//}

inline class Subgraph(val map: Map<Reference<*>, HasUuid>) {
    //fun <T: HasUuid> add(reference: Reference<T>, item: T) = this.put(reference, item)
    @Suppress("UNCHECKED_CAST")
    operator fun <T : HasUuid> get(reference: Reference<T>): T? = map[reference] as? T
}