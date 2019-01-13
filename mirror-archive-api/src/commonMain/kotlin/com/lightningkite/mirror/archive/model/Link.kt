package com.lightningkite.mirror.archive.model

interface Link<AK, AV, BK, BV> {
    val a: Reference<AK, AV>
    val b: Reference<BK, BV>
}