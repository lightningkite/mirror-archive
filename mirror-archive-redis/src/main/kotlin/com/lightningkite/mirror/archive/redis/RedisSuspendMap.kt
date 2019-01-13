package com.lightningkite.mirror.archive.redis

import com.lightningkite.mirror.archive.database.SuspendMap
import com.lightningkite.mirror.archive.database.SuspendMapProvider
import com.lightningkite.mirror.archive.model.Condition
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.model.Sort
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.Type
import com.lightningkite.mirror.serialization.StringSerializer
import io.lettuce.core.ScriptOutputType
import io.lettuce.core.api.StatefulRedisConnection
import kotlinx.coroutines.future.await
import kotlin.coroutines.suspendCoroutine

class RedisSuspendMap<K, V : Any>(
        val name: String,
        val valueType: Type<V>,
        val connection: StatefulRedisConnection<String, String>,
        val serializer: RedisSerializer,
        val getKey: () -> K
) : SuspendMap<K, V> {
    
    class Provider(
            val connection: StatefulRedisConnection<String, String>,
            val serializer: RedisSerializer
    ): SuspendMapProvider {
        override fun <K, V : Any> suspendMap(key: Type<K>, value: Type<V>, name: String?): SuspendMap<K, V> {
            @Suppress("UNCHECKED_CAST") val getKey:()->K = when(key.kClass){
                String::class -> {{Id.randomUUID4().toUUIDString() as K}}
                Id::class -> {{Id.randomUUID4() as K}}
                else -> {{throw UnsupportedOperationException()}}
            }
            return RedisSuspendMap(
                    name = name ?: serializer.registry.kClassToExternalNameRegistry[key.kClass] + "_to_" + serializer.registry.kClassToExternalNameRegistry[value.kClass],
                    valueType = value,
                    connection = connection,
                    serializer = serializer,
                    getKey = getKey
            )
        }
    }
    
    fun K.string(): String = name + ":" + this.toString()
    
    override suspend fun getNewKey(): K = getKey()

    override suspend fun get(key: K): V? {
        val raw = connection.async().hgetall(key.string()).await()
        return if(raw.isEmpty()) null else serializer.read(raw, valueType)
    }

    override suspend fun put(key: K, value: V, conditionIfExists: Condition<V>, create: Boolean): Boolean {
        return if (conditionIfExists is Condition.Never) {
            if (create) {
                val keyString = valueToLua(key.string())
                val script = """
                    $luaHGetAll
                    local c = redis.call('EXISTS', $keyString)
                    if c == 0 then
                      redis.call('HMSET', $keyString, ${serializer.write(value, valueType).entries.joinToString { it.key + " '" + it.value + "'" }})
                      return 1
                    else
                      return 0
                    end
                """.trimIndent()
                return connection.async().eval<Int>(script, ScriptOutputType.INTEGER).await() == 1
            } else {
                false
            }
        } else if (conditionIfExists is Condition.Always) {
            if (create) {
                connection.async().hmset(
                        key.string(),
                        serializer.write(value, valueType)
                ).await() == "OK"
            } else {
                val keyString = valueToLua(key.string())
                val script = """
                    $luaHGetAll
                    local c = redis.call('EXISTS', $keyString)
                    if c == 1 then
                      redis.call('HMSET', $keyString, ${serializer.write(value, valueType).entries.joinToString { it.key + " '" + it.value + "'" }})
                      return 1
                    else
                      return 0
                    end
                """.trimIndent()
                return connection.async().eval<Int>(script, ScriptOutputType.INTEGER).await() == 1
            }
        } else {
            val keyString = valueToLua(key.string())
            val script = """
                $luaHGetAll
                local c = hgetall($keyString)
                if c == nil or ${condition(conditionIfExists)} then
                  redis.call('HMSET', $keyString, ${serializer.write(value, valueType).entries.joinToString { it.key + " '" + it.value + "'" }})
                  return 1
                else
                  return 0
                end
            """.trimIndent()
            return connection.async().eval<Int>(script, ScriptOutputType.INTEGER).await() == 1
        }
    }

    override suspend fun modify(key: K, operation: Operation<V>, condition: Condition<V>): V? {
        val keyString = valueToLua(key.string())
        val script = """
                $luaHGetAll
                local c = hgetall($keyString)
                if c ~= nil and ${condition(condition)} then
                  redis.call('HMSET', $keyString, ${lua(operation)})
                  return redis.call('HGETALL', $keyString)
                else
                  return 0
                end
            """.trimIndent()
        val raw = connection.async().eval<List<Any>>(script, ScriptOutputType.MULTI).await()
                .asSequence()
                .zipWithNext()
                .associate { it.first as String to it.second as String }
        return serializer.read(raw, valueType)
    }

    override suspend fun remove(key: K, condition: Condition<V>): Boolean {
        return if (condition is Condition.Always) {
            connection.async().del(key.string()).await() != 0L
        } else {
            val keyString = valueToLua(key.string())
            val script = """
                $luaHGetAll
                local c = hgetall($keyString)
                if ${condition(condition)} then
                  redis.call('DEL', $keyString)
                  return 1
                else
                  return 0
                end
            """.trimIndent()
            connection.async().eval<Int>(script, ScriptOutputType.INTEGER).await() == 1
        }
    }

    //add key to sorted set for key condition?
    override suspend fun query(
            condition: Condition<V>,
            keyCondition: Condition<K>,
            sortedBy: Sort<V>?,
            after: SuspendMap.Entry<K, V>?,
            count: Int
    ): List<SuspendMap.Entry<K, V>> {
        throw UnsupportedOperationException()
    }


    val luaHGetAll = """local hgetall = function (key)
  local bulk = redis.call('HGETALL', key)
	local result = {}
	local nextkey
	for i, v in ipairs(bulk) do
		if i % 2 == 1 then
			nextkey = v
		else
			result[nextkey] = v
		end
	end
	return result
end"""

    fun lua(operation: Operation<*>, fields: List<FieldInfo<*, *>> = listOf()): String {
        val columnName = if (fields.isEmpty()) "value" else fields.joinToString("_") { it.name }
        return when (operation) {
            is Operation.Set -> "'$columnName', ${valueToLua(operation.value)}"
            is Operation.AddNumeric -> "'$columnName', c['$columnName'] + ${valueToLua(operation.amount)}"
            is Operation.Append -> "'$columnName', c['$columnName'] .. ${valueToLua(operation.string)}"
            is Operation.Fields -> operation.changes.entries.joinToString(",") {
                lua(it.value, fields + it.key)
            }
            else -> throw UnsupportedOperationException()
        }
    }

    fun condition(condition: Condition<*>, fields: List<FieldInfo<*, *>> = listOf()): String {
        val columnName = if (fields.isEmpty()) "value" else fields.joinToString("_") { it.name }
        return when (condition) {
            is Condition.Never -> "false"
            is Condition.Always -> "true"
            is Condition.And -> condition.conditions.joinToString(" and ", "(", ")") { condition(it, fields) }
            is Condition.Or -> condition.conditions.joinToString(" or ", "(", ")") { condition(it, fields) }
            is Condition.Not -> "(not ${condition(condition.condition, fields)})"
            is Condition.Field<*, *> -> condition(condition.condition, fields + condition.field)
            is Condition.Equal -> {
                val column = luaConvertFromString("c['$columnName']", condition.value)
                val value = valueToLua(condition.value)
                "$column == $value"
            }
            is Condition.EqualToOne -> condition(Condition.Or(conditions = condition.values.map { Condition.Equal(it) }))
            is Condition.NotEqual -> {
                val column = luaConvertFromString("c['$columnName']", condition.value)
                val value = valueToLua(condition.value)
                "$column ~= $value"
            }
            is Condition.LessThan -> {
                val column = luaConvertFromString("c['$columnName']", condition.value)
                val value = valueToLua(condition.value)
                "$column < $value"
            }
            is Condition.GreaterThan -> {
                val column = luaConvertFromString("c['$columnName']", condition.value)
                val value = valueToLua(condition.value)
                "$column > $value"
            }
            is Condition.LessThanOrEqual -> {
                val column = luaConvertFromString("c['$columnName']", condition.value)
                val value = valueToLua(condition.value)
                "$column <= $value"
            }
            is Condition.GreaterThanOrEqual -> {
                val column = luaConvertFromString("c['$columnName']", condition.value)
                val value = valueToLua(condition.value)
                "$column >= $value"
            }
            is Condition.TextSearch -> {
                val column = luaConvertFromString("c['$columnName']", condition.query)
                val value = valueToLua(condition.query)
                "string.match($column, $value)"
            }
            is Condition.RegexTextSearch -> TODO()
        }
    }

    fun luaConvertFromString(expression: String, value: Any?): String {
        return when (value) {
            is Number -> "tonumber($expression)"
            is Boolean -> "($expression == true)"
            else -> expression
        }
    }

    fun valueToLua(value: Any?): String {
        return when (value) {
            null -> "nil"
            true -> "true"
            false -> "false"
            is Number -> value.toString()
            is String -> buildString {
                append('\'')
                for (c in value) {
                    when (c) {
                        '[', ']', '\\', '\'', '"' -> {
                            append('\\')
                            append(c)
                        }
                        '\n' -> append("\\n")
                        '\b' -> append("\\b")
                        '\r' -> append("\\r")
                        '\t' -> append("\\t")
                        in ' '..'~' -> append(c)
                        else -> {
                            append("\\")
                            append(c.toInt().toString().padStart(3, '0'))
                        }
                    }
                }
                append('\'')
            }
            else -> throw UnsupportedOperationException()
        }
    }

    // if redis.call('HGET',KEYS[1],'id') == ARGV[1] then return redis.call('HSET',KEYS[1],'title','Updated Title') else return nil end
//
//    override suspend fun getNewKey(): String = Id.randomUUID4().toUUIDString()
//
//    override suspend fun get(key: String): String? = connection.async().get(key).await()
//
//    override suspend fun put(key: String, value: String, conditionIfExists: Condition<String>, create: Boolean): Boolean {
//        return if(conditionIfExists is Condition.Never) {
//            if(create){
//                connection.async().setnx(key, value).await()
//            } else {
//                false
//            }
//        } else if(conditionIfExists is Condition.Always) {
//            if(create){
//                connection.async().set(key, value).await() == "OK"
//            } else {
//                connection.async().eval<String>(setIfExists, ScriptOutputType.VALUE, arrayOf(key), value).await() == "OK"
//            }
//        } else {
//            throw UnsupportedOperationException()
//        }
//    }
//
//    override suspend fun modify(key: String, operation: Operation<String>, condition: Condition<String>): String? {
//        return if(condition is Condition.Never) {
//            when(con)
//            connection.async().setnx(key, value).await()
//        } else if(condition is Condition.Always) {
//            connection.async().eval<String>(setIfExists, ScriptOutputType.VALUE, arrayOf(key), value).await() == "OK"
//        } else {
//            throw UnsupportedOperationException()
//        }
//    }
//
//    override suspend fun remove(key: String, condition: Condition<String>): Boolean {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    override suspend fun find(condition: Condition<String>, sortedBy: Sort<String>?): Pair<String, String>? {
//        return super.find(condition, sortedBy)
//    }
//
//    override suspend fun getMany(keys: Collection<String>): Map<String, String?> {
//        return super.getMany(keys)
//    }
//
//    override suspend fun putMany(map: Map<String, String>) {
//        super.putMany(map)
//    }
//
//    override suspend fun removeMany(keys: Iterable<String>) {
//        super.removeMany(keys)
//    }
//
//    override suspend fun query(condition: Condition<String>, keyCondition: Condition<String>, sortedBy: Sort<String>?, after: Pair<String, String>?, count: Int): List<Pair<String, String>> {
//        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
//    }
//
//    fun conditionToLue
}