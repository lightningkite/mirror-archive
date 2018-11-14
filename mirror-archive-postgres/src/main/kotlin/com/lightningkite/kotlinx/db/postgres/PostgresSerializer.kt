package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.geo.GPSCoordinate
import com.lightningkite.kotlinx.locale.*
import com.lightningkite.kotlinx.locale.Date
import com.lightningkite.kotlinx.reflection.KxClass
import com.lightningkite.kotlinx.reflection.KxType
import com.lightningkite.kotlinx.reflection.KxVariable
import com.lightningkite.kotlinx.reflection.kxReflect
import com.lightningkite.kotlinx.serialization.*
import com.lightningkite.kotlinx.serialization.json.JsonSerializer
import io.reactiverse.pgclient.Row
import java.lang.IllegalArgumentException
import java.sql.Timestamp
import java.time.ZoneOffset
import java.util.*
import kotlin.reflect.KClass

typealias ColumnGenerator = (KxVariable<*, *>) -> List<Column>

class PostgresSerializer(val schema: String = "public"): StandardReaderRepository<PostgresSerializer.RowReader>,
        StandardWriterRepository<Unit, String> {

    class RowReader(var row: Row, var columnIndex: Int = 0)

    fun escape(string: String): String {
        val escaped = string.replace(Regex("[^a-zA-Z 0-9,.+=-_/<>!@#$%^&*(){}\\[\\]`~]")) {
            it.value.toByteArray().joinToString("") {
                "\\x" + it.toString(16)
            }
        }
        return "E'$escaped'"
    }

    override val readerGenerators: MutableList<Pair<Float, TypeReaderGenerator<RowReader>>> = ArrayList()
    override val readers: MutableMap<KClass<*>, TypeReader<RowReader>> = HashMap()
    override val writerGenerators: MutableList<Pair<Float, TypeWriterGenerator<Unit, String>>> = ArrayList()
    override val writers: MutableMap<KClass<*>, TypeWriter<Unit, String>> = HashMap()
    val makeColumnsGenerators: MutableList<Pair<Float, (KClass<*>) -> ColumnGenerator>> = ArrayList()
    val makeColumns = HashMap<KClass<*>, ColumnGenerator>()
    val columns = HashMap<KxVariable<*, *>, List<Column>>()
    fun columns(variable: KxVariable<*, *>): List<Column> {
        return columns.getOrPut(variable) {
            val type = variable.type.base.kclass
            makeColumns.getOrPut(type) {
                makeColumnsGenerators.asSequence().mapNotNull { it.second.invoke(type) }.firstOrNull() ?: throw IllegalArgumentException("No columns or generator found for $type")
            }.invoke(variable)
        }
    }

    val constraints = HashMap<KxVariable<*, *>, List<Constraint>>()
    fun constraints(variable: KxVariable<*, *>): List<Constraint> {
        return constraints.getOrPut(variable) {
            val constraints = ArrayList<Constraint>()
            if (variable.annotations.any { it.name.endsWith("ForceUnique") }) {
                constraints += Constraint(Constraint.Type.Unique, columns(variable).map { it.name })
            }
            if (variable.annotations.any { it.name.endsWith("PrimaryKey") } || variable.name == "id") {
                constraints += Constraint(Constraint.Type.PrimaryKey, columns(variable).map { it.name })
            }
            variable.annotations.find { it.name.endsWith("ForeignKey") }?.let { annotation ->
                val otherTable = (annotation.arguments[0] as KClass<*>).kxReflect
                constraints += Constraint(
                        type = Constraint.Type.ForeignKey,
                        columns = columns(variable).map { it.name },
                        otherSchema = schema,
                        otherTable = otherTable.simpleName,
                        otherColumns = columns(otherTable.primaryKey()).map { it.name }
                )
            }
            constraints
        }
    }

    val tables = HashMap<KxClass<*>, Table>()
    fun table(type: KxClass<*>): Table = tables.getOrPut(type){
        Table(
                name = type.simpleName,
                columns = type.orderedAuthenticVariables.flatMap { columns(it) },
                constraints = type.orderedAuthenticVariables.flatMap { constraints(it) },
                indexes = type.orderedAuthenticVariables.asSequence()
                        .filter { it.annotations.any { it.name.endsWith("Indexed") } }
                        .map {
                            Index(
                                    name = type.simpleName + "_index_" + it.name,
                                    columns = columns(it).map { it.name }
                            )
                        }
                        .toList()
        )
    }

    inline fun <T : Any> setNullableWriter(typeKClass: KClass<T>, crossinline write: (T, KxType) -> String) {
        setWriter(typeKClass) { it, type ->
            if (it == null) "NULL"
            else write(it, type)
        }
    }

    private val helperCache = WeakHashMap<KClass<*>, ReflectiveReaderHelper<RowReader>>()
    fun <T : Any> readRow(type: KClass<T>, row: Row, withCached: RowReader = RowReader(row, 0)): T {
//        println("Raw row: $row")
        withCached.row = row
        withCached.columnIndex = 0

        val helper = helperCache.getOrPut(type) { ReflectiveReaderHelper.tryInit(type, this)!! }
        val builder = helper.instanceBuilder()
        type.kxReflect.orderedAuthenticVariables.forEach {
//            println("Reading ${it.name} at ${withCached.columnIndex}")
            builder.place(it.name, withCached) { withCached.columnIndex += columns(it).size }
        }
        return builder.build() as T
    }

    private val reflectiveWriterDataCache = WeakHashMap<KClass<*>, List<ReflectiveWriterPropertyInfo<Unit, String>>>()
    fun <T : Any> writeRow(type: KClass<T>, value: T): String {
        val vars = reflectiveWriterDataCache.getOrPut(type) { type.reflectiveWriterData(this, type.kxReflect.orderedAuthenticVariables) }
        return vars.joinToString(", ") {
            if (it.key == "id" && it.getter.invoke(value) == null) {
                "DEFAULT"
            } else it.writeValue(Unit, value)

        }
    }

    init {
        makeColumns[Boolean::class] = { listOf(Column(it.name, "BOOLEAN")) }
        setNullableWriter(Boolean::class) { value, type -> value.toString().toUpperCase() }
        setReader(Boolean::class) { row.getBoolean(columnIndex++) }

        makeColumns[Char::class] = { listOf(Column(it.name, "CHAR(1)")) }
        setNullableWriter(Char::class) { value, type -> value.let { escape(it.toString()) } }
        setReader(Char::class) { row.getString(columnIndex++)?.firstOrNull() }

        makeColumns[String::class] = { listOf(Column(it.name, "TEXT")) }
        setNullableWriter(String::class) { value, type -> value.let { escape(it) } }
        setReader(String::class) { row.getString(columnIndex++) }

        makeColumns[Byte::class] = { listOf(Column(it.name, "SMALLINT")) }
        setNullableWriter(Byte::class) { value, type -> value.toString() }
        setReader(Byte::class) { row.getShort(columnIndex++).toByte() }

        makeColumns[Short::class] = { listOf(Column(it.name, "SMALLINT")) }
        setNullableWriter(Short::class) { value, type -> value.toString() }
        setReader(Short::class) { row.getShort(columnIndex++) }

        makeColumns[Int::class] = { listOf(Column(it.name, if(it.name == "id") "SERIAL" else "INT")) }
        setNullableWriter(Int::class) { value, type -> value.toString() }
        setReader(Int::class) { row.getInteger(columnIndex++) }

        makeColumns[Long::class] = { listOf(Column(it.name, if(it.name == "id") "BIGSERIAL" else "BIGINT")) }
        setNullableWriter(Long::class) { value, type -> value.toString() }
        setReader(Long::class) { row.getLong(columnIndex++) }

        makeColumns[Float::class] = { listOf(Column(it.name, "REAL")) }
        setNullableWriter(Float::class) { value, type -> value.toString() }
        setReader(Float::class) { row.getFloat(columnIndex++) }

        makeColumns[Double::class] = { listOf(Column(it.name, "DOUBLE PRECISION")) }
        setNullableWriter(Double::class) { value, type -> value.toString() }
        setReader(Double::class) { row.getDouble(columnIndex++) }

        makeColumns[Date::class] = { listOf(Column(it.name, "DATE")) }
        setNullableWriter(Date::class) { value, type -> "'" + java.sql.Date(value.toJava().time.time).toString() + "'" }
        setReader(Date::class) { row.getLocalDate(columnIndex++)?.toEpochDay()?.let { Date(it.toInt()) } }

        makeColumns[Time::class] = { listOf(Column(it.name, "TIME")) }
        setNullableWriter(Time::class) { value, type -> "'" + java.sql.Time(value.toJava().time.time).toString() + "'" }
        setReader(Time::class) { row.getLocalTime(columnIndex++)?.toNanoOfDay()?.div(1000000)?.let { Time(it.toInt()) } }

        makeColumns[DateTime::class] = { listOf(Column(it.name, "TIMESTAMP")) }
        setNullableWriter(DateTime::class) { value, type -> "'" + Timestamp(value.toJava().time.time).toString() + "'" }
        setReader(DateTime::class) {
            row.getLocalDateTime(columnIndex++)?.let {
                DateTime(
                        it.toLocalDate().toEpochDay().let { Date(it.toInt()) },
                        it.toLocalTime().toNanoOfDay().div(1000000).let { Time(it.toInt()) }
                )
            }
        }

        makeColumns[TimeStamp::class] = { listOf(Column(it.name, "TIMESTAMP")) }
        setNullableWriter(TimeStamp::class) { value, type -> "'" + Timestamp(value.millisecondsSinceEpoch).toString() + "'" }
        setReader(TimeStamp::class) { row.getLocalDateTime(columnIndex++)?.atZone(ZoneOffset.UTC)?.toInstant()?.toEpochMilli()?.let { TimeStamp(it) } }

        makeColumns[GPSCoordinate::class] = { listOf(Column(it.name, "GEOGRAPHY")) }
        setNullableWriter(GPSCoordinate::class) { value, type -> "'SRID=4326;POINT(${value.longitude} ${value.latitude})'" }
        setReader(GPSCoordinate::class) {
            row.getString(columnIndex++)?.let {
                val longitude = it.substringAfter("POINT(").substringBefore(' ').toDouble()
                val latitude = it.substringAfter("POINT(").substringAfter(' ').substringBefore(')').toDouble()
                GPSCoordinate(latitude, longitude)
            }
        }

        makeColumnsGenerators += 1f to { type ->
            {
                listOf(Column(it.name, "TEXT"))
            }
        }
        addReaderGenerator(1f, EnumGenerators.readerGenerator(this))
        addWriterGenerator(1f, EnumGenerators.writerGenerator(this))

        //JSONify all other types
        makeColumnsGenerators += 1f to { type ->
            {
                listOf(Column(it.name, "TEXT"))
            }
        }
        addReaderGenerator(0f) gen@{ type: KClass<*> ->
            return@gen { kxType ->
                JsonSerializer.read(row.getString(columnIndex++), kxType)
            }
        }
        addWriterGenerator(0f) { type: KClass<*> ->
            return@addWriterGenerator { value, typeInfo ->
                escape(JsonSerializer.write(value, typeInfo))
            }
        }
    }
}