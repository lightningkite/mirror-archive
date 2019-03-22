package com.lightningkite.mirror.archive.postgres

import io.reactiverse.pgclient.Row

fun Row.asMap() = (0 until size()).associate { getColumnName(it) to getValue(it) }