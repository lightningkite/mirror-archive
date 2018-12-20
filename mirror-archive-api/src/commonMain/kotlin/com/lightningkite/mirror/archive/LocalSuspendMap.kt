package com.lightningkite.mirror.archive

//Map<K, V> for the top
//List<E> for sorting values, indexes - concurrently iterable
//A table would just be a map with accessible lists

class LocalSuspendMap<K, V>(val backing: MutableMap<K, V> = HashMap()): SuspendMap<K, V> {
    override suspend fun get(key: K): V? = backing[key]
    override suspend fun set(key: K, value: V): V? = backing.put(key, value)
    //TODO: Make atomic
    override suspend fun replace(key: K, old: V?, new: V): Boolean {
        return if(backing[key] == old){
            backing[key] = new
            true
        } else false
    }
}