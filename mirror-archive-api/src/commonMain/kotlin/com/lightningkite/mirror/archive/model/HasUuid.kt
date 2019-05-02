package com.lightningkite.mirror.archive.model


interface HasUuid: HasId<Uuid> {
    override val id: Uuid
}

