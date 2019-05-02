package com.lightningkite.mirror.archive.model

//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT

import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

fun registerArchive() = MirrorRegistry.register(
    com.lightningkite.mirror.archive.model.OperationMultipleMirror.minimal,
    com.lightningkite.mirror.archive.model.OperationFieldMirror.minimal,
    com.lightningkite.mirror.archive.model.OperationAppendMirror,
    com.lightningkite.mirror.archive.model.OperationAddDoubleMirror,
    com.lightningkite.mirror.archive.model.OperationAddNumericMirror.minimal,
    com.lightningkite.mirror.archive.model.OperationAddFloatMirror,
    com.lightningkite.mirror.archive.model.OperationAddLongMirror,
    com.lightningkite.mirror.archive.model.OperationAddIntMirror,
    com.lightningkite.mirror.archive.model.OperationAddNumericMirror.minimal,
    com.lightningkite.mirror.archive.model.OperationSetMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionRegexTextSearchMirror,
    com.lightningkite.mirror.archive.model.ConditionEndsWithMirror,
    com.lightningkite.mirror.archive.model.ConditionStartsWithMirror,
    com.lightningkite.mirror.archive.model.ConditionTextSearchMirror,
    com.lightningkite.mirror.archive.model.ConditionGreaterThanOrEqualMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionLessThanOrEqualMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionGreaterThanMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionLessThanMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionNotEqualMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionEqualToOneMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionEqualMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionFieldMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionNotMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionOrMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionAndMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionAlwaysMirror,
    com.lightningkite.mirror.archive.model.ConditionNeverMirror,
    com.lightningkite.mirror.archive.model.HasIdMirror.minimal,
    com.lightningkite.mirror.archive.model.HasUuidMirror,
    com.lightningkite.mirror.archive.model.ReferenceMirror.minimal,
    com.lightningkite.mirror.archive.model.UuidMirror,
    com.lightningkite.mirror.archive.model.SortMirror.minimal,
    com.lightningkite.mirror.archive.model.LinkMirror.minimal,
    com.lightningkite.mirror.archive.model.OperationMirror.minimal,
    com.lightningkite.mirror.archive.model.ConditionMirror.minimal
)