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
import com.lightningkite.mirror.archive.model.SortMirror
import com.lightningkite.mirror.archive.model.OperationMirror

data class RequestDatabaseLimitedUpdateMirror<T : Any>(
        val TMirror: MirrorType<T>
) : MirrorClass<RequestDatabase.LimitedUpdate<T>>() {

    override val mirrorClassCompanion: MirrorClassCompanion? get() = Companion

    companion object : MirrorClassCompanion {
        val TMirrorMinimal get() = AnyMirror

        override val minimal = RequestDatabaseLimitedUpdateMirror(TypeArgumentMirrorType("T", Variance.INVARIANT, TMirrorMinimal))
        @Suppress("UNCHECKED_CAST")
        override fun make(typeArguments: List<MirrorType<*>>): MirrorClass<*> = RequestDatabaseLimitedUpdateMirror(typeArguments[0] as MirrorType<Any>)

        @Suppress("UNCHECKED_CAST")
        fun make(
                TMirror: MirrorType<*>? = null
        ) = RequestDatabaseLimitedUpdateMirror<Any>(
                TMirror = (TMirror ?: TMirrorMinimal) as MirrorType<Any>
        )
    }

    override val typeParameters: Array<MirrorType<*>> get() = arrayOf(TMirror)
    @Suppress("UNCHECKED_CAST")
    override val kClass: KClass<RequestDatabase.LimitedUpdate<T>>
        get() = RequestDatabase.LimitedUpdate::class as KClass<RequestDatabase.LimitedUpdate<T>>
    override val modifiers: Array<Modifier> get() = arrayOf(Modifier.Data)
    override val packageName: String get() = "com.lightningkite.mirror.archive.database"
    override val localName: String get() = "RequestDatabase.LimitedUpdate"
    override val implements: Array<MirrorClass<*>> get() = arrayOf(RequestMirror(IntMirror))
    override val owningClass: KClass<*>? get() = RequestDatabase::class

    val fieldDatabaseRequest: Field<RequestDatabase.LimitedUpdate<T>, Database.Request<T>> = Field(
            owner = this,
            index = 0,
            name = "databaseRequest",
            type = DatabaseRequestMirror(TMirror),
            optional = false,
            get = { it.databaseRequest },
            annotations = listOf<Annotation>()
    )

    val fieldCondition: Field<RequestDatabase.LimitedUpdate<T>, Condition<T>> = Field(
            owner = this,
            index = 1,
            name = "condition",
            type = ConditionMirror(TMirror),
            optional = false,
            get = { it.condition },
            annotations = listOf<Annotation>()
    )

    val fieldOperation: Field<RequestDatabase.LimitedUpdate<T>, Operation<T>> = Field(
            owner = this,
            index = 2,
            name = "operation",
            type = OperationMirror(TMirror),
            optional = false,
            get = { it.operation },
            annotations = listOf<Annotation>()
    )

    val fieldSort: Field<RequestDatabase.LimitedUpdate<T>, List<Sort<T, *>>> = Field(
            owner = this,
            index = 3,
            name = "sort",
            type = ListMirror((SortMirror.make(TMirror, null) as MirrorType<Sort<T, *>>)),
            optional = true,
            get = { it.sort },
            annotations = listOf<Annotation>()
    )

    val fieldLimit: Field<RequestDatabase.LimitedUpdate<T>, Int> = Field(
            owner = this,
            index = 4,
            name = "limit",
            type = IntMirror,
            optional = false,
            get = { it.limit },
            annotations = listOf<Annotation>()
    )

    override val fields: Array<Field<RequestDatabase.LimitedUpdate<T>, *>> = arrayOf(fieldDatabaseRequest, fieldCondition, fieldOperation, fieldSort, fieldLimit)

    override fun deserialize(decoder: Decoder): RequestDatabase.LimitedUpdate<T> {
        var databaseRequestSet = false
        var fieldDatabaseRequest: Database.Request<T>? = null
        var conditionSet = false
        var fieldCondition: Condition<T>? = null
        var operationSet = false
        var fieldOperation: Operation<T>? = null
        var sortSet = false
        var fieldSort: List<Sort<T, *>>? = null
        var limitSet = false
        var fieldLimit: Int? = null
        val decoderStructure = decoder.beginStructure(this, TMirror)
        loop@ while (true) {
            when (decoderStructure.decodeElementIndex(this)) {
                CompositeDecoder.READ_ALL -> {
                    fieldDatabaseRequest = decoderStructure.decodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror))
                    databaseRequestSet = true
                    fieldCondition = decoderStructure.decodeSerializableElement(this, 1, ConditionMirror(TMirror))
                    conditionSet = true
                    fieldOperation = decoderStructure.decodeSerializableElement(this, 2, OperationMirror(TMirror))
                    operationSet = true
                    fieldSort = decoderStructure.decodeSerializableElement(this, 3, ListMirror((SortMirror.make(TMirror, null) as MirrorType<Sort<T, *>>)))
                    sortSet = true
                    fieldLimit = decoderStructure.decodeIntElement(this, 4)
                    limitSet = true
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
                2 -> {
                    fieldOperation = decoderStructure.decodeSerializableElement(this, 2, OperationMirror(TMirror))
                    operationSet = true
                }
                3 -> {
                    fieldSort = decoderStructure.decodeSerializableElement(this, 3, ListMirror((SortMirror.make(TMirror, null) as MirrorType<Sort<T, *>>)))
                    sortSet = true
                }
                4 -> {
                    fieldLimit = decoderStructure.decodeIntElement(this, 4)
                    limitSet = true
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
        if (!operationSet) {
            throw MissingFieldException("operation")
        }
        if (!sortSet) {
            fieldSort = listOf()
        }
        if (!limitSet) {
            throw MissingFieldException("limit")
        }
        return RequestDatabase.LimitedUpdate<T>(
                databaseRequest = fieldDatabaseRequest as Database.Request<T>,
                condition = fieldCondition as Condition<T>,
                operation = fieldOperation as Operation<T>,
                sort = fieldSort as List<Sort<T, *>>,
                limit = fieldLimit as Int
        )
    }

    override fun serialize(encoder: Encoder, obj: RequestDatabase.LimitedUpdate<T>) {
        val encoderStructure = encoder.beginStructure(this, TMirror)
        encoderStructure.encodeSerializableElement(this, 0, DatabaseRequestMirror(TMirror), obj.databaseRequest)
        encoderStructure.encodeSerializableElement(this, 1, ConditionMirror(TMirror), obj.condition)
        encoderStructure.encodeSerializableElement(this, 2, OperationMirror(TMirror), obj.operation)
        encoderStructure.encodeSerializableElement(this, 3, ListMirror((SortMirror.make(TMirror, null) as MirrorType<Sort<T, *>>)), obj.sort)
        encoderStructure.encodeIntElement(this, 4, obj.limit)
        encoderStructure.endStructure(this)
    }
}