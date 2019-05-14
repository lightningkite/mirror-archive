package com.lightningkite.mirror.archive.model


interface Lockable<ID> : HasId<ID> {
    val lock: LockState
}
