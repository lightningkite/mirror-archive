package com.lightningkite.mirror.archive.model

import com.lightningkite.mirror.info.FieldInfo

data class Reverse<PK, PV, OTHER : Any>(
        val reference: Reference<PK, PV>,
        val field: FieldInfo<OTHER, Reference<PK, PV>>
)