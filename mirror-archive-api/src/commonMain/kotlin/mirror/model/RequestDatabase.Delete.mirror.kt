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
import com.lightningkite.mirror.archive.model.ConditionMirror
import com.lightningkite.mirror.request.RequestMirror

data class RequestDatabaseDeleteMirror<T : Any>(
        val TMirror: MirrorType<T>
) : MirrorClass<RequestDatabase.Delete<T>>() {

    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion

    companion object : MirrorClassCompanion {
        val TMirrorMinimal get() = AnyMirror

        override val minimal = RequestDatabaseDeleteMirror(TypeArgumentMirrorType("T", Variance.INVARIANT, TMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = RequestDatabaseDeleteMirror(typeArguments[0] as MirrorType<Any>)

        @Suppress("UNCHECKED_CAST")
        fun make(
                TMirror: MirrorType<*>? = null
        ) = RequestDatabaseDeleteMirror<Any>(
                TMirror = (TMirror ?: TMirrorMinimal) as MirrorType<Any>
        )
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<RequestDatabase.Delete<T>>
        get() = RequestDatabase.Delete::class as KClass<RequestDatabase.Delete<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.database"
    override val localName: String get() = "RequestDatabase.Delete"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(RequestMirror(IntMirror))
    override val owningClass: KClass<*>? get() = RequestDatabase::class

    val fieldDatabaseRequest: Field<RequestDatabase.Delete<T>, Database.Request<T>> = Field(
            owner = this,
            index = 0,
            name = "databaseRequest",
            type = DatabaseRequestMirror(TMirror),
            optional = false,
            get = { it.databaseRequest },
            annotations = listOf<Annotation>()
    )

    val fieldCondition: Field<RequestDatabase.Delete<T>, Condition<T>> = Field(
            owner = this,
            index = 1,
            name = "condition",
            type = ConditionMirror(TMirror),
            optional = false,
            get = { it.condition },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<RequestDatabase.Delete<T>, *>> = arrayOf(fieldDatabaseRequest, fieldCondition)

    override fun deserialize(decoder: Decoder): RequestDatabase.Delete<T> {
        var databaseRequestSet = false
        var fieldDatabaseRequest: Database.Request<T>? = null
        var conditionSet = false
        var fieldCondition: Condition<T>? = null
        val decoderStructure = decoder.beginStructure(this, TMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldDatabaseRequest = decoderStructure.decodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror))
                    databaseRequestSet = true
                    fieldCondition = decoderStructure.decodeSerializableElement(this, 1, ConditionMirror(TMirror))
                    conditionSet = true
                    break@loop
                }
                CompositeDecoder.READ_DONE -> break@loop
                0 -> {
                    fieldDatabaseRequest = decoderStructure.decodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror))
                    databaseRequestSet = true
                }
                1 -> {
                    fieldCondition = decoderStructure.decodeSerializableElement(this, 1, ConditionMirror(TMirror))
                    conditionSet = true
                }
                else -> {
                }
            }
        }
        decoderStructure.endStructure(this)
        if (!databaseRequestSet) {
            throw MissingFieldException("databaseRequest")
        }
        if (!conditionSet) {
            throw MissingFieldException("condition")
        }
        return RequestDatabase.Delete<T>(
                databaseRequest = fieldDatabaseRequest as Database.Request<T>,
                condition = fieldCondition as Condition<T>
        )
    }

    override fun serialize(encoder: Encoder, obj: RequestDatabase.Delete<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror), obj.databaseRequest)
        encoderStructure.encodeSerializableElement(this, 1, ConditionMirror(TMirror), obj.condition)
        encoderStructure.endStructure(this)
    }
}