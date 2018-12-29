package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.model.HasId
import com.lightningkite.mirror.archive.model.Id

data class Post(
        override var id: Id = Id.randomUUID4(),
        var userId: Long = 0,
        var title: String = "",
        var body: String = ""
): HasId