package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.TypeProjection

val <T: Any> Type<T>.modification get() = Type<Operation<T, *>>(Operation::class, listOf(TypeProjection(this), TypeProjection.STAR))
val <T: Any> Type<T>.condition get() = Type<Condition<T>>(Condition::class, listOf(TypeProjection(this)))
val <T: Any> Type<T>.sort get() = Type<Sort<T, *>>(Sort::class, listOf(TypeProjection(this), TypeProjection.STAR))
val <T: Any> Type<T>.queryResult get() = Type<QueryResult<T>>(QueryResult::class, listOf(TypeProjection(this)))