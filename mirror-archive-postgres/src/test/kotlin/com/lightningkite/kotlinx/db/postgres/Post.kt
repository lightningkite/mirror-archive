package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.HasId
import com.lightningkite.mirror.archive.Id

data class Post(
        override var id: Id = Id.key(),
        var userId: Long = 0,
        var title: String = "",
        var body: String = ""
): HasId