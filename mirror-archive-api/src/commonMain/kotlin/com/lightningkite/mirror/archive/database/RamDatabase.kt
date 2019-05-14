package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.archive.model.comparator
import com.lightningkite.mirror.info.MirrorClass

class RamDatabase<T : Any>(private val backingData: MutableList<T> = ArrayList()) : Database<T> {

    companion object FromConfiguration : Database.Provider.FromConfiguration {
        override val name: String get() = "RAM"
        override fun invoke(arguments: Map<String, String>) = Provider
    }

    object Provider : Database.Provider {
        override fun <T : Any> get(mirrorClass: MirrorClass<T>, default: T, name: String): Database<T> {
            return RamDatabase()
        }
    }

    override suspend fun get(condition: Condition<T>, sort: List<Sort<T, *>>, count: Int, after: T?): List<T> {
        return backingData.asSequence()
                .filter { condition.invoke(it) }
                .sortedWith(sort.comparator())
                .let {
                    if (after == null)
                        it
                    else {
                        var pass = false
                        it.dropWhile { x ->
                            if (x == after) {
                                pass = true
                                true
                            } else !pass
                        }
                    }
                }
                .take(count)
                .toList()
    }

    override suspend fun insert(values: List<T>): List<T> {
        backingData.addAll(values)
        return values
    }

    override suspend fun update(condition: Condition<T>, operation: Operation<T>): Int {
        val iter = backingData.listIterator()
        var modifications = 0
        while(iter.hasNext()){
            val item = iter.next()
            if(condition.invoke(item)){
                iter.set(operation.invoke(item))
                modifications++
            }
        }
        return modifications
    }

    override suspend fun limitedUpdate(condition: Condition<T>, operation: Operation<T>, sort: List<Sort<T, *>>, limit: Int): Int {
        val indices = backingData.asSequence().mapIndexed { index, t -> index to t }.sortedWith(object : Comparator<Pair<Int, T>> {
            val backing = sort.comparator()
            override fun compare(a: Pair<Int, T>, b: Pair<Int, T>): Int {
                return backing.compare(a.second, b.second)
            }
        }).map { it.first }
        var modifications = 0
        for(index in indices){
            val item = this.backingData[index]
            if(condition.invoke(item)){
                this.backingData[index] = operation.invoke(item)
                modifications++
                if (modifications >= limit) break
            }
        }
        return modifications
    }

    override suspend fun delete(condition: Condition<T>): Int {
        val iter = backingData.listIterator()
        var modifications = 0
        while(iter.hasNext()){
            val item = iter.next()
            if(condition.invoke(item)){
                iter.remove()
                modifications++
            }
        }
        return modifications
    }
}