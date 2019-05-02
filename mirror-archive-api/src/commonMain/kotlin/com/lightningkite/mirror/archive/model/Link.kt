package com.lightningkite.mirror.archive.model

interface Link<A : HasUuid, B : HasUuid> {
    val a: Reference<A>
    val b: Reference<B>
}