package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.sql.Column
import com.lightningkite.mirror.archive.sql.PartialTable
import com.lightningkite.mirror.archive.sql.SQLSerializer
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.SerializationRegistry
import com.lightningkite.mirror.serialization.StringSerializer
import com.lightningkite.mirror.serialization.json.JsonSerializer

class PostgresSerializer(registry: SerializationRegistry, backup: StringSerializer = JsonSerializer(registry)) : SQLSerializer(registry, backup) {

    init {
        addDefinition(Boolean::class.type, PartialTable(listOf(Column("", "BOOLEAN"))))
        addEncoderDirect(Boolean::class.type)
        addDecoderDirect(Boolean::class.type)

        addDefinition(String::class.type, PartialTable(listOf(Column("", "TEXT"))))

        addDefinition(Id::class.type, PartialTable(listOf(Column("", "UUID"))))
        addEncoderDirect(Id::class.type)
        addDecoderDirect(Id::class.type)
    }

//    override fun convertToSQLCondition(condition: Condition<*>, baseType: Type<*>, fields: List<FieldInfo<*, *>>): String {
//        if(condition is Condition.RegexTextSearch) {
//            val fieldName = fields.joinToString("_"){ it.name }
//            val type = fields.last().type as Type<Any?>
//            val subFields = rawDefine(type)
//            val column = fieldName nameAppend subFields.columns.first().name
//            val encoded = buildString {
//                var escaped = false
//                var inBrackets = false
//                for(c in condition.query.pattern){
//                    when(c){
//                        '.' -> {
//                            if(escaped || inBrackets){
//                                append(c)
//                            } else {
//                                append("%")
//                            }
//                        }
//                        '%' -> append("\\%")
//                        '[' -> {
//                            inBrackets = true
//                            append(c)
//                        }
//                        ']' -> {
//                            inBrackets = false
//                            append(c)
//                        }
//                        '\\' -> {
//                            escaped = true
//                            append(c)
//                        }
//                        else -> append(c)
//                    }
//                    if(c != '\\')
//                        escaped = false
//                }
//            }
//            return "$column SIMILAR TO " + encodeSingle(encoded)
//        }
//        return super.convertToSQLCondition(condition, baseType, fields)
//    }

//    override fun upsert(table: Table, values: List<String>, condition: String?): SQLQuery? {
//        return SQLQuery(buildString {
//            append("INSERT INTO ")
//            append(table.fullName)
//            append(" ")
//            append(table.columns.joinToString(", ", "(", ")") { it.name })
//            append(" VALUES ")
//            append(values.joinToString(", ", "(", ")"))
//            append(" ON CONFLICT ON CONSTRAINT ")
//            append(table.constraints.find { it.type == Constraint.Type.PrimaryKey }!!.name)
//            append(" DO UPDATE SET ")
//            append(table.columns.zip(values).joinToString(", ") { it.first.name + " = " + it.second })
//            if (condition != null) {
//                append(" WHERE ")
//                //TODO: Mod condition to use 'excluded.'
//                append(condition)
//            }
//            append(" RETURNING ${table.columns.first().name}")
//        })
//    }
//
//    override fun update(table: Table, columns: List<Column>, values: List<String>, condition: String?): SQLQuery {
//        return SQLQuery(
//                super.update(table, columns, values, condition).string + " RETURNING ${table.columns.first().name}"
//        )
//    }

    //    override fun updateModifyReturning(
//            table: Table,
//            resultColumns: List<Column>,
//            modifications: List<String>,
//            condition: String?
//    ): SQLQuery? = SQLQuery(buildString {
//        append("UPDATE ")
//        append(table.fullName)
//        append(" SET ")
//        append(modifications.joinToString())
//        if (condition != null) {
//            append(" WHERE ")
//            append(condition)
//        }
//        append(" RETURNING ")
//        append(resultColumns.joinToString { it.name })
//    })
}