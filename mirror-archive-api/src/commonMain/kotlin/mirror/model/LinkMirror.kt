//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*

class LinkMirror<A : HasId, B : HasId>(
        val AMirror: MirrorType<A>,
        val BMirror: MirrorType<B>
) : PolymorphicMirror<Link<A, B>>() {

    companion object {
        val minimal = LinkMirror(HasIdMirror, HasIdMirror)
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(AMirror, BMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Link<A, B>>
        get() = Link::class as KClass<Link<A, B>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Interface)
    override val implements: Array<MirrorClass<*>> get() = arrayOf()
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Link"
}
