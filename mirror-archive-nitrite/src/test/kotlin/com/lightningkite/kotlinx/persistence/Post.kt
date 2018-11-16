package com.lightningkite.kotlinx.persistence

import com.lightningkite.mirror.archive.Model

data class Post(
        var userId: Long = 0,
        override var id: Long? = null,
        var title: String = "",
        var body: String = ""
): Model<Long>