package com.lightningkite.kotlinx.db.mongo

import com.lightningkite.kotlinx.persistence.*
import com.lightningkite.kotlinx.reflection.KxClass
import com.lightningkite.kotlinx.reflection.kxType
import com.lightningkite.kotlinx.serialization.CommonSerialization
import org.bson.*
import com.mongodb.BasicDBObject
import com.mongodb.async.client.MongoCollection
import com.mongodb.async.client.MongoDatabase
import org.bson.types.ObjectId
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine


suspend fun MongoDatabase.get(name: String): MongoCollection<BsonDocument> = suspendCoroutine{ cont ->
    try {
        cont.resume(this.getCollection(name, BsonDocument::class.java))
    } catch (e: IllegalAccessException) {
        //not created yet
        this.createCollection(name) { _, throwable ->
            if (throwable == null) {
                cont.resume(this.getCollection(name, BsonDocument::class.java))
            } else {
                cont.resumeWithException(throwable)
            }
        }
    }
}

suspend fun <T : Model<String>> MongoCollection<BsonDocument>.insertOne(type: KxClass<T>, value: T): T = suspendCoroutine{ cont ->
    val serialized = BsonValueSerializer.write(type.kxType, value, Unit) as BsonDocument
    this.insertOne(serialized) { _, throwable ->
        if (throwable == null) {
            val id = serialized["_id"]!!.asObjectId().value.toHexString()
            value.id = id
            cont.resume(value)
        } else {
            cont.resumeWithException(throwable)
        }
    }
}

suspend fun <T : Model<String>> MongoCollection<BsonDocument>.insertMany(type: KxClass<T>, values: Collection<T>): Collection<T> = suspendCoroutine{ cont ->
    val serialized = values.map { BsonValueSerializer.write(type.kxType, it, Unit) as BsonDocument }
    this.insertMany(serialized) { _, throwable ->
        if (throwable == null) {
            values.forEachIndexed { index, it ->
                it.id = serialized[index]["_id"]!!.asObjectId().value.toHexString()
            }
            cont.resume(values)
        } else {
            cont.resumeWithException(throwable)
        }
    }
}

//fun <reified T: Any> MongoCollection<BsonDocument>.get(id: String)
//        = this.findOne()
//
//fun <reified T: Any> MongoCollection<BsonDocument>.insertOne(value: T)
//        = this.insertOne(BsonValueSerializer.write(T::class.kxType, value, Unit) as BsonDocument)
//
//fun <reified T: Any> MongoCollection<BsonDocument>.insertMany(values: List<T>)
//        = this.insertMany(values.map { BsonValueSerializer.write(T::class.kxType, it, Unit) as BsonDocument })
//
suspend fun <T : Model<String>> MongoCollection<BsonDocument>.replace(type: KxClass<T>, id: String, value: T) = suspendCoroutine<Unit>{ cont ->
    this.updateOne(BasicDBObject("_id", ObjectId(id)), BsonValueSerializer.write(type.kxType, value, Unit) as BsonDocument) { _, throwable ->
        if (throwable == null) {
            cont.resume(Unit)
        } else {
            cont.resumeWithException(throwable)
        }
    }
}

suspend fun <T : Model<String>> MongoCollection<BsonDocument>.updateOne(type: KxClass<T>, id: String, modifications: List<ModificationOnItem<T, *>>) = suspendCoroutine<Unit>{ cont ->
    this.updateOne(BasicDBObject("_id", ObjectId(id)), modifications.toMongo(BsonValueSerializer)) { _, throwable ->
        if (throwable == null) {
            cont.resume(Unit)
        } else {
            cont.resumeWithException(throwable)
        }
    }
}

suspend fun <T : Model<String>> MongoCollection<BsonDocument>.updateMany(type: KxClass<T>, filter: ConditionOnItem<T>, modifications: List<ModificationOnItem<T, *>>) = suspendCoroutine<Unit>{ cont ->
    this.updateMany(filter.toMongo(BsonValueSerializer), modifications.toMongo(BsonValueSerializer)) { _, throwable ->
        if (throwable == null) {
            cont.resume(Unit)
        } else {
            cont.resumeWithException(throwable)
        }
    }
}

suspend fun <T : Model<String>> MongoCollection<BsonDocument>.query(
        type: KxClass<T>,
        condition: ConditionOnItem<T>,
        sortedBy: List<SortOnItem<T, *>>,
        continuationToken: String?,
        count: Int
):QueryResult<T> = suspendCoroutine { cont ->
    //TODO min for continuationToken
    //TODO sort for sortedBy
    val output = ArrayList<T>()
    this.find(condition.toMongo(BsonValueSerializer))
            .limit(count)
            .forEach({
                output.add(BsonValueSerializer.read(type.kxType, it) as T)
            }){ _, throwable ->
                if (throwable == null) {
                    cont.resume(QueryResult(output))
                } else {
                    cont.resumeWithException(throwable)
                }
            }
}

suspend fun <T: Model<String>> MongoCollection<BsonDocument>.getOne(
        type: KxClass<T>,
        id: String
): T = suspendCoroutine { cont ->
    this.find(
            BasicDBObject("_id", ObjectId(id))
    ).first { result, throwable ->
        if (throwable == null) {
            cont.resume(BsonValueSerializer.read(type.kxType, result) as T)
        } else {
            cont.resumeWithException(throwable)
        }
    }
}
