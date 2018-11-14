package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.locale.TimeStamp
import com.lightningkite.kotlinx.locale.now
import com.lightningkite.kotlinx.persistence.*
import com.lightningkite.kotlinx.reflection.*
import com.lightningkite.kotlinx.server.ServerFunction

@ExternalReflection
data class Post(
        @Indexed var userId: Long = 0,
        override var id: Long? = null,
        var title: String = "",
        var body: String = "",
        var parent: Reference<Post, Long>? = null,
        var time: TimeStamp = TimeStamp.now()
): Model<Long>{

    @ExternalReflection
    data class Get(val id: Reference<Post, Long>) : ServerFunction<Post>

    @ExternalReflection
    data class Insert(val value: Post) : ServerFunction<Post>

    @ExternalReflection
    data class Update(val value: Post) : ServerFunction<Post>

    @ExternalReflection
    data class Modify(val id: Reference<Post, Long>, val modifications: List<ModificationOnItem<Post, *>>) : ServerFunction<Post>

    @ExternalReflection
    data class Query(
            val condition: ConditionOnItem<Post> = ConditionOnItem.Always(),
            val sortedBy: List<SortOnItem<Post, *>> = listOf(),
            val continuationToken: String? = null,
            val count: Int = 100
    ) : ServerFunction<QueryResult<Post>>

    @ExternalReflection
    data class Delete(val id: Reference<Post, Long>) : ServerFunction<Unit>
}