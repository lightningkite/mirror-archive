package com.lightningkite.mirror.archive

import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.info.TypeProjection

val <T: Any> Type<T>.modification get() = Type<ModificationOnItem<T, *>>(ModificationOnItem::class, listOf(TypeProjection(this), TypeProjection.STAR))
val <T: Any> Type<T>.condition get() = Type<ConditionOnItem<T>>(ConditionOnItem::class, listOf(TypeProjection(this)))
val <T: Any> Type<T>.sort get() = Type<SortOnItem<T, *>>(SortOnItem::class, listOf(TypeProjection(this), TypeProjection.STAR))
val <T: Any> Type<T>.queryResult get() = Type<QueryResult<T>>(QueryResult::class, listOf(TypeProjection(this)))