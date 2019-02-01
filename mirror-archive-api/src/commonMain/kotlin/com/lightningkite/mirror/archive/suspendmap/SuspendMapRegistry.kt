package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Reference
import com.lightningkite.mirror.archive.model.Relation
import com.lightningkite.mirror.info.FieldInfo
import kotlin.reflect.KClass

class SuspendMapRegistry(
        val maps: HashMap<KClass<*>, SuspendMap<*, *>>,
        val relations: HashMap<KClass<*>, HashMap<FieldInfo<*, *>, Relation<*, *, *, *>>>
) {
    @Suppress("UNCHECKED_CAST")
    inline fun <KEY, reified VALUE: Any> map(): SuspendMap<KEY, VALUE> = maps[VALUE::class] as SuspendMap<KEY, VALUE>

    suspend inline fun <KEY, reified VALUE: Any> resolve(reference: Reference<KEY, VALUE>): VALUE? {
        return map<KEY, VALUE>().get(reference.key)
    }
}


//class ManyRelation<AK, AV : Any, BK, BV : Any, RK, RV : Link<AK, AV, BK, BV>>(
//        val mapRelation: SuspendMap<RK, RV>,
//        val fieldA: FieldInfo<RV, Reference<AK, AV>>,
//        val fieldB: FieldInfo<RV, Reference<BK, BV>>
//) {
//    fun get(key: AK, after: Pair<RK, RV>? = null, count: Int = 100): List<Pair<RK, RV>> = mapRelation.query(
//            condition = Condition.Field(fieldA, Condition.Equal(Reference(key))),
//            after = after,
//            count = count
//    )
//    fun get(key: BK, after: Pair<RK, RV>? = null, count: Int = 100): List<Pair<RK, RV>> = mapRelation.query(
//            condition = Condition.Field(fieldA, Condition.Equal(Reference(key))),
//            after = after,
//            count = count
//    )
//}