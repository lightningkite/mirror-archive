//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.MirrorAnnotation
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

data class MultiIndexMirror(
        val fields: Array<String>
) : MirrorAnnotation {
    override val annotationType: KClass<out Annotation> get() = MultiIndex::class
    override fun asMap(): Map<String, Any?> = mapOf(
            "fields" to fields
    )
}