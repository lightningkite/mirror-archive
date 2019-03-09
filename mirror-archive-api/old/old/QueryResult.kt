package com.lightningkite.mirror.archive.model

data class QueryResult<T>(
        var results: List<T>,
        var continuationToken: String? = null
)
