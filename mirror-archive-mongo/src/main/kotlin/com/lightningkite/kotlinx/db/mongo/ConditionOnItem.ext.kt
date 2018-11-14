package com.lightningkite.kotlinx.db.mongo

import com.lightningkite.kotlinx.persistence.*
import org.bson.BsonArray
import org.bson.BsonDocument
import org.bson.BsonString
import org.bson.BsonValue

fun ConditionOnItem<*>.toMongo(serializer: BsonValueSerializer):BsonDocument = when(this){
    is ConditionOnItem.Never -> TODO()
    is ConditionOnItem.Always -> BsonDocument()
    is ConditionOnItem.And -> BsonDocument("\$and", BsonArray(conditions.map { it.toMongo(serializer) }))
    is ConditionOnItem.Or -> BsonDocument("\$or", BsonArray(conditions.map { it.toMongo(serializer) }))
    is ConditionOnItem.Not -> BsonDocument("\$not", condition.toMongo(serializer))
    is ConditionOnItem.Equal<*, *> -> BsonDocument(field.name, serializer.write(field.type, value, Unit))
    is ConditionOnItem.EqualToOne<*, *> -> BsonDocument(field.name, BsonDocument("\$in", BsonArray( values.map { serializer.write(field.type, it, Unit) } )))
    is ConditionOnItem.NotEqual<*, *> -> BsonDocument(field.name, BsonDocument("\$ne", serializer.write(field.type, value, Unit)))
    is ConditionOnItem.LessThan<*, *> -> BsonDocument(field.name, BsonDocument("\$lt", serializer.write(field.type, value, Unit)))
    is ConditionOnItem.GreaterThan<*, *> -> BsonDocument(field.name, BsonDocument("\$gt", serializer.write(field.type, value, Unit)))
    is ConditionOnItem.LessThanOrEqual<*, *> -> BsonDocument(field.name, BsonDocument("\$lte", serializer.write(field.type, value, Unit)))
    is ConditionOnItem.GreaterThanOrEqual<*, *> -> BsonDocument(field.name, BsonDocument("\$gte", serializer.write(field.type, value, Unit)))
    is ConditionOnItem.TextSearch<*, *> -> BsonDocument("\$text", BsonDocument("\$search", BsonString(query)))
    is ConditionOnItem.RegexTextSearch<*, *> -> BsonDocument("\$regex", BsonString(query.pattern))
}

fun ModificationOnItem<*, *>.toMongo(serializer: BsonValueSerializer):Pair<String, BsonValue> = when(this){
    is ModificationOnItem.Add<*, *> -> "\$inc" to serializer.write(field.type, this.amount, Unit)
    is ModificationOnItem.Multiply<*, *> -> "\$mul" to serializer.write(field.type, this.amount, Unit)
    is ModificationOnItem.Set<*, *> -> "\$set" to serializer.write(field.type, this.value, Unit)
    else -> throw IllegalArgumentException("This type of modification ($this) is not yet supported.")
}

fun Collection<ModificationOnItem<*, *>>.toMongo(serializer: BsonValueSerializer):BsonDocument = BsonDocument().also {
    for(item in this){
        val pair = item.toMongo(serializer)
        it[pair.first] = pair.second
    }
}
