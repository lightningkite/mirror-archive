package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

fun configureMirror(){
    ClassInfo.register(com.lightningkite.kotlinx.db.postgres.PostClassInfo)
}