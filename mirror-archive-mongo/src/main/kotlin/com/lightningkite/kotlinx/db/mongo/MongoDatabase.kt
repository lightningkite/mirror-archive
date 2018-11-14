package com.lightningkite.kotlinx.db.mongo

import com.lightningkite.kotlinx.observable.list.ObservableList
import com.lightningkite.kotlinx.observable.property.ObservableProperty
import com.lightningkite.kotlinx.persistence.*
import com.lightningkite.kotlinx.reflection.KxClass
import com.lightningkite.kotlinx.reflection.StringReflection
import com.mongodb.BasicDBObject
import com.mongodb.async.client.MongoCollection
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.async
import org.bson.BsonDocument
import org.bson.types.ObjectId
import java.lang.IllegalArgumentException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class MongoDatabase(
        val mongo: com.mongodb.async.client.MongoDatabase
) : Database {
    @Suppress("UNCHECKED_CAST")
    override fun <T : Model<ID>, ID> table(type: KxClass<T>, name: String): DatabaseTable<T, ID> {
        if (type.variables["id"]!!.type.base != StringReflection) throw IllegalArgumentException()
        return Table(
                type = type as KxClass<out Model<String>>,
                mongoCollectionDeferred = GlobalScope.async { mongo.get(name) }
        ) as DatabaseTable<T, ID>
    }

    class Table<T : Model<String>>(
            val type: KxClass<T>,
            val mongoCollectionDeferred: Deferred<MongoCollection<BsonDocument>>
    ) : DatabaseTable<T, String> {

        var currentMongoCollection: MongoCollection<BsonDocument>? = null
        suspend fun mongoCollection(): MongoCollection<BsonDocument> = currentMongoCollection ?: run {
            mongoCollectionDeferred.await()
        }

        override suspend fun get(transaction: Transaction, id: String): T = mongoCollection().getOne(type, id)

        override suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> {
            return mongoCollection().insertMany(type, models)
        }

        override suspend fun insert(transaction: Transaction, model: T): T {
            return mongoCollection().insertOne(type, model)
        }

        override suspend fun update(transaction: Transaction, model: T): T {
            mongoCollection().replace<T>(type, model.id!!, model)
            return model
        }

        override suspend fun modify(transaction: Transaction, id: String, modifications: List<ModificationOnItem<T, *>>): T {
            mongoCollection().updateOne(type, id, modifications)
            return mongoCollection().getOne(type, id)
        }

        override suspend fun query(transaction: Transaction, condition: ConditionOnItem<T>, sortedBy: List<SortOnItem<T, *>>, continuationToken: String?, count: Int): QueryResult<T> {
            return mongoCollection().query(type, condition, sortedBy, continuationToken, count)
        }

        override suspend fun delete(transaction: Transaction, id: String) {
            return with(mongoCollection()) {
                suspendCoroutine { cont ->
                    deleteOne(BasicDBObject("_id", ObjectId(id))) { result, throwable ->
                        if (throwable == null) {
                            cont.resume(Unit)
                        } else {
                            cont.resumeWithException(throwable)
                        }
                    }
                }
            }
        }
    }
}
