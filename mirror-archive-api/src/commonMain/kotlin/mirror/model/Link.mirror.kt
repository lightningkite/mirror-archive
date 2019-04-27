//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

data class LinkMirror<A: HasId, B: HasId>(
    val AMirror: MirrorType<A>,
    val BMirror: MirrorType<B>
) : PolymorphicMirror<Link<A,B>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val AMirrorMinimal get() = HasIdMirror
        val BMirrorMinimal get() = HasIdMirror
        
        override val minimal = LinkMirror(TypeArgumentMirrorType("A", Variance.INVARIANT, AMirrorMinimal), TypeArgumentMirrorType("B", Variance.INVARIANT, BMirrorMinimal))
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = LinkMirror(typeArguments[0] as MirrorType<HasId>, typeArguments[1] as MirrorType<HasId>)
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(AMirror, BMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Link<A,B>> get() = Link::class as KClass<Link<A,B>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Interface)
    override val implements: Array<MirrorClass<*>> get() = arrayOf()
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Link"
}
