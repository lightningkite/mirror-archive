package com.lightningkite.mirror.archive.model

//typealias Subgraph = Map<Reference<*>, HasId>
//typealias MutableSubgraph = MutableMap<Reference<*>, HasId>
//
//
//fun <T: HasId> MutableSubgraph.add(reference: Reference<T>, item: T) = this.put(reference, item)
//operator fun <T: HasId> Subgraph.get(reference: Reference<T>): T? = this[reference] as? T
//
//data class TestObject(override val id: Uuid) : HasId {
//
//}
//
//fun test(graph: Subgraph){
//    val get: TestObject = graph[Reference<TestObject>(Uuid.randomUUID4())]
//}

inline class Subgraph(val map: Map<Reference<*>, HasId>) {
    //fun <T: HasId> add(reference: Reference<T>, item: T) = this.put(reference, item)
    @Suppress("UNCHECKED_CAST")
    operator fun <T : HasId> get(reference: Reference<T>): T? = map[reference] as? T
}