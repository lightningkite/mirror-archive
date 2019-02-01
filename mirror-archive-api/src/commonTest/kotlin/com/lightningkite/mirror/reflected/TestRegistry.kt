package com.lightningkite.rekwest.server

import com.lightningkite.kommon.native.SharedImmutable
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

@SharedImmutable
val TestRegistry = ClassInfoRegistry(
        com.lightningkite.mirror.archive.model.ReferenceClassInfo,
        com.lightningkite.mirror.archive.model.ConditionClassInfo,
        com.lightningkite.mirror.archive.model.ConditionNeverClassInfo,
        com.lightningkite.mirror.archive.model.ConditionAlwaysClassInfo,
        com.lightningkite.mirror.archive.model.ConditionAndClassInfo,
        com.lightningkite.mirror.archive.model.ConditionOrClassInfo,
        com.lightningkite.mirror.archive.model.ConditionNotClassInfo,
        com.lightningkite.mirror.archive.model.ConditionEqualClassInfo,
        com.lightningkite.mirror.archive.model.ConditionEqualToOneClassInfo,
        com.lightningkite.mirror.archive.model.ConditionNotEqualClassInfo,
        com.lightningkite.mirror.archive.model.ConditionLessThanClassInfo,
        com.lightningkite.mirror.archive.model.ConditionGreaterThanClassInfo,
        com.lightningkite.mirror.archive.model.ConditionLessThanOrEqualClassInfo,
        com.lightningkite.mirror.archive.model.ConditionGreaterThanOrEqualClassInfo,
        com.lightningkite.mirror.archive.model.ConditionTextSearchClassInfo,
        com.lightningkite.mirror.archive.model.ConditionRegexTextSearchClassInfo,
        com.lightningkite.mirror.archive.model.OperationClassInfo,
        com.lightningkite.mirror.archive.model.OperationSetClassInfo,
        com.lightningkite.mirror.archive.model.SortClassInfo
)
