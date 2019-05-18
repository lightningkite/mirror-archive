//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.request.Request
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass
import kotlinx.serialization.*
import mirror.kotlin.*
import com.lightningkite.mirror.request.RequestMirror

data class RequestDatabaseInsertMirror<T: Any>(
    val TMirror: MirrorType<T>
) : MirrorClass<RequestDatabase.Insert<T>>() {
    
    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion
    companion object : MirrorClassCompanion {
        val TMirrorMinimal get() = AnyMirror
        
        override val minimal = RequestDatabaseInsertMirror(TypeArgumentMirrorType("T", Variance.INVARIANT, TMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = RequestDatabaseInsertMirror(typeArguments[0] as MirrorType<Any>)
        
        @Suppress("UNCHECKED_CAST")
        fun make(
            TMirror: MirrorType<*>? = null
        ) = RequestDatabaseInsertMirror<Any>(
            TMirror = (TMirror ?: TMirrorMinimal) as MirrorType<Any>
        )
    }
    
    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<RequestDatabase.Insert<T>> get() = RequestDatabase.Insert::class as KClass<RequestDatabase.Insert<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.database"
    override val localName: String get() = "RequestDatabase.Insert"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(RequestMirror(ListMirror(TMirror)))
    override val owningClass: KClass<*>? get() = RequestDatabase::class
    
    val fieldDatabaseRequest: Field<RequestDatabase.Insert<T>,Database.Request<T>> = Field(
        owner = this,
        index = 0,
        name = "databaseRequest",
        type = DatabaseRequestMirror(TMirror),
        optional = false,
        get = { it.databaseRequest },
        annotations = listOf<Annotation>()
    )
    
    val fieldValues: Field<RequestDatabase.Insert<T>,List<T>> = Field(
        owner = this,
        index = 1,
        name = "values",
        type = ListMirror(TMirror),
        optional = false,
        get = { it.values },
        annotations = listOf<Annotation>()
    )
    
    override val fields: Array<Field<RequestDatabase.Insert<T>, *>> = arrayOf(fieldDatabaseRequest, fieldValues)
    
    override fun deserialize(decoder: Decoder): RequestDatabase.Insert<T> {
        var databaseRequestSet = false
        var fieldDatabaseRequest: Database.Request<T>? = null
        var valuesSet = false
        var fieldValues: List<T>? = null
        val decoderStructure = decoder.beginStructure(this, TMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldDatabaseRequest = decoderStructure.decodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror))
                    databaseRequestSet = true
                    fieldValues = decoderStructure.decodeSerializableElement(this, 1, ListMirror(TMirror))
                    valuesSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldDatabaseRequest = decoderStructure.decodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror))
                    databaseRequestSet = true
                }
                1 -> {
                    fieldValues = decoderStructure.decodeSerializableElement(this, 1, ListMirror(TMirror))
                    valuesSet = true
                }
                else -> {}
            }
        }
        decoderStructure.endStructure(this)
        if(!databaseRequestSet) {
            throw MissingFieldException("databaseRequest")
        }
        if(!valuesSet) {
            throw MissingFieldException("values")
        }
        return RequestDatabase.Insert<T>(
            databaseRequest = fieldDatabaseRequest as Database.Request<T>,
            values = fieldValues as List<T>
        )
    }
    
    override fun serialize(encoder: Encoder, obj: RequestDatabase.Insert<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror), obj.databaseRequest)
        encoderStructure.encodeSerializableElement(this, 1, ListMirror(TMirror), obj.values)
        encoderStructure.endStructure(this)
    }
}
