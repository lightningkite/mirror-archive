package com.lightningkite.kotlinx.persistence

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

fun configureMirror(){
    ClassInfo.register(com.lightningkite.kotlinx.persistence.PostClassInfo)
}