package com.lightningkite.mirror.archive.server

import com.lightningkite.kommon.string.Uri
import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.archive.database.LocalDatabaseHandler
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.setTo
import com.lightningkite.mirror.info.MirrorClass
import com.lightningkite.mirror.request.LocalRequestHandler
import com.lightningkite.mirror.request.Request
import kotlin.reflect.KClass

data class UploadForUriField<OWNER: Any>(
        val database: Database.Request<OWNER>,
        val field: MirrorClass.Field<OWNER, Uri?>,
        val on: Condition<OWNER>,
        val contents: ByteArray
): Request<Unit>

fun LocalRequestHandler.setupUploadForUriField(dbHandler: Database.Handler, root: FileObject){
    invocation(UploadForUriField::class){
        val saved =
        dbHandler.invoke(database).update(on, field setTo )
    }
}