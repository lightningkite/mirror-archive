package com.lightningkite.mirror.archive

data class QueryResult<T>(
        var results: List<T>,
        var continuationToken: String? = null
)
