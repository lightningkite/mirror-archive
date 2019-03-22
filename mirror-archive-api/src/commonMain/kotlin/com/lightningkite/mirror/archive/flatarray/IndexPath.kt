package com.lightningkite.mirror.archive.flatarray

class IndexPath(val intArray: IntArray) : List<Int> {

    companion object {
        val empty = IndexPath(intArrayOf())
    }

    override val size: Int get() = intArray.size
    override fun contains(element: Int): Boolean = intArray.contains(element)
    override fun containsAll(elements: Collection<Int>): Boolean = elements.all { intArray.contains(it) }
    override fun get(index: Int): Int = intArray[index]
    override fun indexOf(element: Int): Int = intArray.indexOf(element)
    override fun isEmpty(): Boolean = intArray.isEmpty()
    override fun iterator(): Iterator<Int> = intArray.iterator()
    override fun lastIndexOf(element: Int): Int = intArray.lastIndexOf(element)
    override fun listIterator(): ListIterator<Int> = intArray.toList().listIterator()
    override fun listIterator(index: Int): ListIterator<Int> = intArray.toList().listIterator()
    override fun subList(fromIndex: Int, toIndex: Int): List<Int> = intArray.copyOfRange(fromIndex, toIndex).toList()

    override fun equals(other: Any?): Boolean {
        return other is IndexPath && other.intArray.contentEquals(intArray)
    }

    override fun hashCode(): Int = intArray.contentHashCode()
    override fun toString(): String = intArray.joinToString { it.toString() }

    fun startsWith(other: IndexPath): Boolean {
        if (other.intArray.size > intArray.size) return false
        var index = 0
        while (index < other.intArray.size) {
            if (intArray[index] != other.intArray[index]) return false
            index++
        }
        return true
    }

    operator fun plus(index: Int): IndexPath = IndexPath(intArray + index)
}