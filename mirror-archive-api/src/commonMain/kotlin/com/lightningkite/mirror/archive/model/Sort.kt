package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorClass


data class Sort<T : Any, V : Comparable<V>>(
        val field: MirrorClass.Field<T, V>,
        val ascending: Boolean = true
) : Comparator<T> {
    override fun compare(a: T, b: T): Int {
        val aValue = field.get(a)
        val bValue = field.get(b)
        return if (ascending)
            aValue.compareTo(bValue)
        else
            -aValue.compareTo(bValue)
    }

    fun after(value: V): Condition<T> {
        return if (ascending) {
            Condition.Field(field, Condition.GreaterThan(value))
        } else {
            Condition.Field(field, Condition.LessThan(value))
        }
    }

    fun after(owner: T): Condition<T> {
        return after(field.get(owner))
    }
}

fun <T : Any> List<Sort<T, *>>.comparator(): Comparator<T> = Comparator { a, b ->
    for (sort in this) {
        val result = sort.compare(a, b)
        if (result != 0)
            return@Comparator result
    }
    if (a is Comparable<*>) {
        (a as Comparable<Any?>).compareTo(b)
    }
    return@Comparator 0
}

fun <T : Any> List<Sort<T, *>>.after(value: T, final: Sort<T, *>): Condition<T> {
    return Condition.Or<T>(ArrayList<Condition<T>>(size + 1).apply {
        this@after.forEachIndexed { index, sort ->
            add(Condition.And(ArrayList<Condition<T>>(index + 1).apply {
                for (it in this@after.subList(0, index)) {
                    add(Condition.Field(it.field, Condition.Equal(it.field.get(value))))
                }
                add(sort.after(value))
            }))
        }
        add(Condition.And(ArrayList<Condition<T>>(size + 1).apply {
            for (it in this@after) {
                add(Condition.Field(it.field, Condition.Equal(it.field.get(value))))
            }
            add(final.after(value))
        }))
    })
}

fun <T : Any> List<Sort<T, *>>.after(value: T): Condition<T> {
    return Condition.Or<T>(ArrayList<Condition<T>>(size).apply {
        this@after.forEachIndexed { index, sort ->
            add(Condition.And(ArrayList<Condition<T>>(index + 1).apply {
                for (it in this@after.subList(0, index)) {
                    add(Condition.Field(it.field, Condition.Equal(it.field.get(value))))
                }
                add(sort.after(value))
            }))
        }
    })
}