package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.request.Request

class RequestDatabaseHandler(val handler: Request.Handler) : Database.Handler {

    override suspend fun <T : Any> invoke(request: Database.Request<T>): Database<T> {
        return RequestDatabase(
                handler = handler,
                request = request
        )
    }
}

