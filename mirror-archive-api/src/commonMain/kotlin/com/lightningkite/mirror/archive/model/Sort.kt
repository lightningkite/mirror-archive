package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.FieldInfo

interface Sort<T>: Comparator<T> {
    fun iterable(): Iterable<Sort<T>> = listOf()

    class DontCare<T> : Sort<T> {
        override fun compare(a: T, b: T): Int {
            return -1
        }
    }

    data class Multi<T>(val comparators: List<Sort<T>>) : Sort<T> {
        override fun compare(a: T, b: T): Int {
            for (comparator in comparators) {
                val result = comparator.compare(a, b)
                if (result != 0)
                    return result
            }
            return 0
        }

        override fun iterable(): Iterable<Sort<T>> = comparators
    }

    class Natural<T : Comparable<T>> : Sort<T> {
        override fun compare(a: T, b: T): Int {
            return a.compareTo(b)
        }
    }

    data class Field<T : Any, V : Comparable<V>>(
            val field: FieldInfo<T, V>,
            val ascending: Boolean = true
    ) : Comparator<T> {
        override fun compare(a: T, b: T): Int {
            val aValue = field.get(a)
            val bValue = field.get(b)
            return aValue.compareTo(bValue)
        }
    }

}