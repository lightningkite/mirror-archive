package com.lightningkite.mirror.archive.sql

data class SQLQuery(
        val sql: String = "",
        val arguments: List<Any?> = listOf()
) {
    data class Builder(
            val sql: StringBuilder = StringBuilder(),
            val arguments: ArrayList<Any?> = ArrayList(),
            val fields: ArrayList<String> = ArrayList(),
            var prepend: String = "",
            val argumentText: (Int)->String = { "\$" + arguments.size.toString() }
    ) {
        fun addValue(value: Any?) {
            sql.append(argumentText)
            arguments.add(value)
        }
        inline fun withConditionFieldName(name: String, action:()->Unit){
            fields.add(name)
            action()
            fields.removeAt(fields.lastIndex)
        }
        inline fun withPrepend(newPrepend: String, action:()->Unit){
            val old = prepend
            prepend = newPrepend
            action()
            prepend = old
        }
        val field: String get() = prepend + if(fields.isEmpty()) "value" else fields.joinToString("_")
        val rawField: String get() = prepend + fields.joinToString("_")
        fun build(): SQLQuery = SQLQuery(sql = sql.toString(), arguments = arguments)
    }

    companion object {
        inline fun build(setup:SQLQuery.Builder.()->Unit): SQLQuery {
            return Builder().apply(setup).build()
        }
    }
}