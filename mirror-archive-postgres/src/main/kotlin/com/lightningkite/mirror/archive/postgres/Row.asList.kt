package com.lightningkite.mirror.archive.postgres

import io.reactiverse.pgclient.Row

fun Row.asList() = List(size()){ i -> getValue(i)}