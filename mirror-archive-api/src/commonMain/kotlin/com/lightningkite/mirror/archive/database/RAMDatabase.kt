package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort

class RAMDatabase<T>(private val backingData: MutableList<T> = ArrayList()) : Database<T> {

    override suspend fun get(condition: Condition<T>, sort: Sort<T>?, count: Int, after: T?): List<T> {
        return backingData.asSequence()
                .filter { condition.invoke(it) }
                .let {
                    if(sort != null){
                        it.sortedWith(sort)
                    } else it
                }
                .let {
                    if (after == null)
                        it
                    else {
                        var pass = false
                        it.dropWhile { x ->
                            if (x == after) {
                                pass = true
                                false
                            } else !pass
                        }
                    }
                }
                .take(count)
                .toList()
    }

    override suspend fun insert(values: List<T>): List<T?> {
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