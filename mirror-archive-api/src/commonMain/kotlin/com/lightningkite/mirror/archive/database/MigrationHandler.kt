package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.info.MirrorClass

/**
 * Handles additional things when migrating.
 * Gives you what fields are being removed, what fields are being added, and lets you run updates and deletes as necessary.
 */
typealias MigrationHandler<T> = suspend (
        database: Database<T>,
        removingFields: Collection<MirrorClass.Field<*, *>>,
        addingFields: Collection<MirrorClass.Field<*, *>>
) -> Unit