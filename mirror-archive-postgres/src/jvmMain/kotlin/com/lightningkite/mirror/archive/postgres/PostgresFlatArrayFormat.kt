package com.lightningkite.mirror.archive.postgres

import com.lightningkite.lokalize.time.TimeStampMirror
import com.lightningkite.mirror.archive.flatarray.BinaryFlatArrayFormat
import com.lightningkite.mirror.archive.model.UuidMirror

object PostgresFlatArrayFormat : BinaryFlatArrayFormat(
        terminateAt = {
            when(it){
                TimeStampMirror.descriptor,
                UuidMirror.descriptor -> true
                else -> false
            }
        }
)