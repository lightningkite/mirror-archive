//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.kommon.collection.treeWalkDepthSequence
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*

object ConditionRegexTextSearchMirror : MirrorClass<Condition.RegexTextSearch>() {
    override val empty: Condition.RegexTextSearch get() = Condition.RegexTextSearch(
        query = StringMirror.empty
    )
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<Condition.RegexTextSearch> get() = Condition.RegexTextSearch::class as KClass<Condition.RegexTextSearch>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.model"
    override val localName: String get() = "Condition.RegexTextSearch"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(ConditionMirror(StringMirror))
    override val owningClass: KClass<*>? get() = Condition::class
    
    val fieldQuery: Field<Condition.RegexTextSearch,String> = Field(
        owner = this,
        index = 0,
        name = "query",
        type = StringMirror,
        optional = false,
        get = { it.query },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<Condition.RegexTextSearch, *>> = arrayOf(fieldQuery)
    
    override fun deserialize(decoder: Decoder): Condition.RegexTextSearch {
        var querySet = false
        var fieldQuery: String? = null
        val decoderStructure = decoder.beginStructure(this)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldQuery = decoderStructure.decodeStringElement(this, 0)
                    querySet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldQuery = decoderStructure.decodeStringElement(this, 0)
                    querySet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!querySet) {
            throw MissingFieldException("query")
        }
        return Condition.RegexTextSearch(
            query = fieldQuery as String
        )
    }
    
    override fun serialize(encoder: Encoder, obj: Condition.RegexTextSearch) {
        val encoderStructure = encoder.beginStructure(this)
        encoderStructure.encodeStringElement(this, 0, obj.query)
        encoderStructure.endStructure(this)
    }
}
