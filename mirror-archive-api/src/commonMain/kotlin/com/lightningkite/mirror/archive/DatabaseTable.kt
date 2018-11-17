package com.lightningkite.mirror.archive

@Deprecated("Use Database.Table instead.", ReplaceWith("Database.Table<T, ID>"))
typealias DatabaseTable<T, ID> = Database.Table<T, ID>