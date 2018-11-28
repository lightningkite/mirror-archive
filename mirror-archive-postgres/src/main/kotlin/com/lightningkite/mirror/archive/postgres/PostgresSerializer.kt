package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.Id
import com.lightningkite.mirror.info.*
import com.lightningkite.mirror.serialization.*
import io.reactiverse.pgclient.Row
import java.lang.IllegalArgumentException
import java.util.*
import kotlin.reflect.KClass

typealias ColumnGenerator = (FieldInfo<*, *>) -> List<Column>

class PostgresSerializer(val schema: String = "public", override val registry: SerializationRegistry) : Decoder<PostgresSerializer.RowReader>,
        Encoder<Appendable> {

    val <T : Any> KClass<T>.info get() = registry.classInfoRegistry[this]!!

    class RowReader(var row: Row, var columnIndex: Int = 0)

    fun escape(string: String): String {
        val escaped = string.replace(Regex("[^a-zA-Z 0-9,.+=-_/<>!@#$%^&*(){}\\[\\]`~]")) {
            it.value.toByteArray().joinToString("") {
                "\\x" + it.toString(16)
            }
        }
        return "E'$escaped'"
    }

    override val arbitraryDecoders: MutableList<Decoder.Generator<RowReader>> = ArrayList()
    override val decoders: MutableMap<Type<*>, TypeDecoder<RowReader, Any?>> = HashMap()
    override val kClassDecoders: MutableMap<KClass<*>, (Type<*>) -> TypeDecoder<RowReader, Any?>?> = HashMap()
    override val arbitraryEncoders: MutableList<Encoder.Generator<Appendable>> = ArrayList()
    override val encoders: MutableMap<Type<*>, TypeEncoder<Appendable, Any?>> = HashMap()
    override val kClassEncoders: MutableMap<KClass<*>, (Type<*>) -> TypeEncoder<Appendable, Any?>?> = HashMap()

    val makeColumnsGenerators: MutableList<Pair<Float, (KClass<*>) -> ColumnGenerator>> = ArrayList()
    val makeColumns = HashMap<KClass<*>, ColumnGenerator>()
    val columns = HashMap<FieldInfo<*, *>, List<Column>>()
    fun columns(variable: FieldInfo<*, *>): List<Column> {
        return columns.getOrPut(variable) {
            val type = variable.type.kClass
            makeColumns.getOrPut(type) {
                makeColumnsGenerators.asSequence().mapNotNull { it.second.invoke(type) }.firstOrNull()
                        ?: throw IllegalArgumentException("No columns or generator found for $type")
            }.invoke(variable)
        }
    }

    val constraints = HashMap<FieldInfo<*, *>, List<Constraint>>()
    fun constraints(variable: FieldInfo<*, *>): List<Constraint> {
        return constraints.getOrPut(variable) {
            val constraints = ArrayList<Constraint>()
            if (variable.annotations.any { it.name.endsWith("ForceUnique") }) {
                constraints += Constraint(Constraint.Type.Unique, columns(variable).map { it.name })
            }
            if (variable.annotations.any { it.name.endsWith("PrimaryKey") } || variable.name == "id") {
                constraints += Constraint(Constraint.Type.PrimaryKey, columns(variable).map { it.name })
            }
            variable.annotations.find { it.name.endsWith("ForeignKey") }?.let { annotation ->
                val otherTable = (annotation.arguments[0] as KClass<*>).info
                constraints += Constraint(
                        type = Constraint.Type.ForeignKey,
                        columns = columns(variable).map { it.name },
                        otherSchema = schema,
                        otherTable = otherTable.localName,
                        otherColumns = columns(otherTable.primaryKey()).map { it.name }
                )
            }
            constraints
        }
    }

    val tables = HashMap<ClassInfo<*>, Table>()
    fun table(type: ClassInfo<*>): Table = tables.getOrPut(type) {
        Table(
                name = type.localName,
                columns = type.fields.flatMap { columns(it) },
                constraints = type.fields.flatMap { constraints(it) },
                indexes = type.fields.asSequence()
                        .filter { it.annotations.any { it.name.endsWith("Indexed") } }
                        .map {
                            Index(
                                    name = type.localName + "_index_" + it.name,
                                    columns = columns(it).map { it.name }
                            )
                        }
                        .toList()
        )
    }

    fun <T : Any> readRow(type: KClass<T>, row: Row, withCached: RowReader = RowReader(row, 0)): T {

        val fields = type.info.fields
        val subCoders by lazy { fields.associate { it.name to rawDecoder(it.type as Type<*>) } }

//        println("Raw row: $row")
        withCached.row = row
        withCached.columnIndex = 0

        val subvalues = HashMap<String, Any?>()
        subCoders.forEach {
            //            println("Reading ${it.name} at ${withCached.columnIndex}")
            subvalues[it.key] = it.value.invoke(withCached)
        }
        return type.info.construct(subvalues)
    }

    fun <T : Any> writeRow(type: KClass<T>, value: T): String {
        val lazySubCoders by lazy { type.info.fields.associateWith { rawEncoder(it.type as Type<*>) } }
        return buildString {
            var first = true
            lazySubCoders.forEach { key, coder ->
                if (first) {
                    first = false
                } else {
                    append(", ")
                }
                val subvalue = key.get.untyped(value)
                if (key.name == "id" && subvalue == null)
                    append("DEFAULT")
                else
                    coder.invoke(this, subvalue)
            }
        }
    }

    init {
        makeColumns[Boolean::class] = { listOf(Column(it.name, "BOOLEAN")) }
        addEncoder(Boolean::class.type) { append(it.toString().toUpperCase()) }
        addDecoder(Boolean::class.type) { row.getBoolean(columnIndex++) }

        makeColumns[Char::class] = { listOf(Column(it.name, "CHAR(1)")) }
        addEncoder(Char::class.type) { value -> append(escape(value.toString())) }
        addDecoder(Char::class.type) { row.getString(columnIndex++).first() }

        makeColumns[String::class] = { listOf(Column(it.name, "TEXT")) }
        addEncoder(String::class.type) { value -> append(escape(value)) }
        addDecoder(String::class.type) { row.getString(columnIndex++) }

        makeColumns[Byte::class] = { listOf(Column(it.name, "SMALLINT")) }
        addEncoder(Byte::class.type) { value -> append(value.toString()) }
        addDecoder(Byte::class.type) { row.getShort(columnIndex++).toByte() }

        makeColumns[Short::class] = { listOf(Column(it.name, "SMALLINT")) }
        addEncoder(Short::class.type) { value -> append(value.toString()) }
        addDecoder(Short::class.type) { row.getShort(columnIndex++) }

        makeColumns[Int::class] = { listOf(Column(it.name, "INT")) }
        addEncoder(Int::class.type) { value -> append(value.toString()) }
        addDecoder(Int::class.type) { row.getInteger(columnIndex++) }

        makeColumns[Long::class] = { listOf(Column(it.name, "BIGINT")) }
        addEncoder(Long::class.type) { value -> append(value.toString()) }
        addDecoder(Long::class.type) { row.getLong(columnIndex++) }

        makeColumns[Float::class] = { listOf(Column(it.name, "REAL")) }
        addEncoder(Float::class.type) { value -> append(value.toString()) }
        addDecoder(Float::class.type) { row.getFloat(columnIndex++) }

        makeColumns[Double::class] = { listOf(Column(it.name, "DOUBLE PRECISION")) }
        addEncoder(Double::class.type) { value -> append(value.toString()) }
        addDecoder(Double::class.type) { row.getDouble(columnIndex++) }

        makeColumns[Id::class] = { listOf(Column(it.name, "UUID")) }
        addEncoder(Id::class.type) { value ->
            append('\'')
            append(value.toUUIDString())
            append('\'')
        }
        addDecoder(Id::class.type) {
            val uuid = row.getUUID(columnIndex++)
            Id.fromLongs(uuid.mostSignificantBits, uuid.leastSignificantBits)
        }

        initializeEncoders()
        initializeDecoders()

//        makeColumns[Date::class] = { listOf(Column(it.name, "DATE")) }
//        addEncoder(Date::class.type) { value -> append("'" + java.sql.Date(value.toJava().time.time).toString() + "'") }
//        addDecoder(Date::class.type) { row.getLocalDate(columnIndex++)?.toEpochDay()?.let { Date(it.toInt()) } }
//
//        makeColumns[Time::class] = { listOf(Column(it.name, "TIME")) }
//        addEncoder(Time::class.type) { value -> append("'" + java.sql.Time(value.toJava().time.time).toString() + "'") }
//        addDecoder(Time::class.type) { row.getLocalTime(columnIndex++)?.toNanoOfDay()?.div(1000000)?.let { Time(it.toInt()) } }
//
//        makeColumns[DateTime::class] = { listOf(Column(it.name, "TIMESTAMP")) }
//        addEncoder(DateTime::class.type) { value -> append("'" + Timestamp(value.toJava().time.time).toString() + "'") }
//        addDecoder(DateTime::class.type) {
//            row.getLocalDateTime(columnIndex++)?.let {
//                DateTime(
//                        it.toLocalDate().toEpochDay().let { Date(it.toInt()) },
//                        it.toLocalTime().toNanoOfDay().div(1000000).let { Time(it.toInt()) }
//                )
//            }
//        }
//
//        makeColumns[TimeStamp::class] = { listOf(Column(it.name, "TIMESTAMP")) }
//        addEncoder(TimeStamp::class.type) { value -> append("'" + Timestamp(value.millisecondsSinceEpoch).toString() + "'") }
//        addDecoder(TimeStamp::class.type) { row.getLocalDateTime(columnIndex++)?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.let { TimeStamp(it) } }

//        makeColumns[GPSCoordinate::class] = { listOf(Column(it.name, "GEOGRAPHY")) }
//        addEncoder(GPSCoordinate::class.type) { value -> append("'SRID=4326;POINT(${value.longitude} ${value.latitude})'") }
//        addDecoder(GPSCoordinate::class.type) {
//            row.getString(columnIndex++)?.let {
//                val longitude = it.substringAfter("POINT(").substringBefore(' ').toDouble()
//                val latitude = it.substringAfter("POINT(").substringAfter(' ').substringBefore(')').toDouble()
//                GPSCoordinate(latitude, longitude)
//            }
//        }

        //JSONify all other types
        makeColumnsGenerators += 1f to { type ->
            {
                listOf(Column(it.name, "TEXT"))
            }
        }

        addEncoder(NullableEncoderGenerator())
        addDecoder(NullableDecoderGenerator())
    }

    inner class NullableEncoderGenerator : Encoder.Generator<Appendable> {
        override val description: String get() = "null"
        override val priority: Float get() = 1f
        override fun generateEncoder(type: Type<*>): (Appendable.(value: Any?) -> Unit)? {
            if (!type.nullable) return null
            val underlying = rawEncoder(type.copy(nullable = false))
            return { value ->
                if (value == null) {
                    append("NULL")
                } else {
                    underlying.invoke(this, value)
                }
            }
        }
    }

    inner class NullableDecoderGenerator : Decoder.Generator<PostgresSerializer.RowReader> {
        override val description: String get() = "null"
        override val priority: Float get() = 1f

        override fun generateDecoder(type: Type<*>): (PostgresSerializer.RowReader.() -> Any?)? {
            if (!type.nullable) return null
            val underlying = rawDecoder(type.copy(nullable = false))
            return {
                if (this.row.getValue(this.columnIndex) == null) {
                    this.columnIndex++
                    null
                } else underlying.invoke(this)
            }
        }
    }
}