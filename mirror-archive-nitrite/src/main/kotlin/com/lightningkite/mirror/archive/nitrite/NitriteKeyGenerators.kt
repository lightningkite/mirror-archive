package com.lightningkite.mirror.archive.nitrite

import org.dizitart.no2.FindOptions
import org.dizitart.no2.NitriteCollection
import org.dizitart.no2.SortOrder
import java.util.*

object NitriteKeyGenerators {
    fun long(collection: NitriteCollection):()->Long {
        val start = collection.find(FindOptions.sort("_id", SortOrder.Descending).thenLimit(0, 1)).firstOrNull()?.get("_id") as? Long ?: 0L
        var current = start + 1L
        return { current++ }
    }
    fun int(collection: NitriteCollection):()->Int {
        val start = collection.find(FindOptions.sort("_id", SortOrder.Descending).thenLimit(0, 1)).firstOrNull()?.get("_id") as? Int ?: 0
        var current = start + 1
        return { current++ }
    }
    fun string(collection: NitriteCollection):()->String {
        return { UUID.randomUUID().toString() }
    }
}
