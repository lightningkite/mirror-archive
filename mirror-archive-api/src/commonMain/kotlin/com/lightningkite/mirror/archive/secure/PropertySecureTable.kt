package com.lightningkite.mirror.archive.secure

import com.lightningkite.kommon.exception.ForbiddenException
import com.lightningkite.mirror.archive.*
import com.lightningkite.mirror.info.ClassInfo
import com.lightningkite.mirror.info.FieldInfo

abstract class PropertySecureTable<T : Model<ID>, ID>(
        val classInfo: ClassInfo<T>,
        val underlying: Database.Table<T, ID>
) : Database.Table<T, ID> {
    interface PropertyRules<T : Any, V> {
        val variable: FieldInfo<T, V>
        suspend fun query(untypedUser: Any?)
        suspend fun read(untypedUser: Any?, justInserted: Boolean, currentState: T): V
        suspend fun write(untypedUser: Any?, currentState: T?, newState: V): V
    }

    abstract val propertyRules: Map<FieldInfo<T, *>, PropertyRules<T, *>>
    abstract suspend fun wholeQuery(untypedUser: Any?)
    abstract suspend fun wholeRead(untypedUser: Any?, justInserted: Boolean, currentState: T): Boolean
    abstract suspend fun wholeWrite(untypedUser: Any?, isDelete: Boolean, currentState: T?)

    private suspend fun handleWholeItemRead(transaction: Transaction, item: T, isJustInserted: Boolean): T? {
        return if(!wholeRead(transaction.untypedUser, isJustInserted, item)) null
        else {
            val newValues = classInfo.fields.associate {
                val rules = propertyRules[it] as? PropertyRules<T, Any?>
                it.name to (if(rules == null) it.get(item) else rules.read(transaction.untypedUser, isJustInserted, item))
            }
            classInfo.construct(newValues)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun handleWholeItemUpdate(transaction: Transaction, old: T?, item: T?): T? {
        wholeWrite(transaction.untypedUser, item == null, old)
        if (item != null) {
            val newValues = classInfo.fields.associate {
                val suggestedValue = it.get(item)
                val rules = propertyRules[it] as? PropertyRules<T, Any?>
                it.name to (if(rules == null) suggestedValue else rules.write(transaction.untypedUser, old, suggestedValue))
            }
            return classInfo.construct(newValues)
        } else return null
    }


    override suspend fun get(transaction: Transaction, id: ID): T = underlying.get(transaction, id)
            .let { item ->
                handleWholeItemRead(transaction, item, false) ?: throw ForbiddenException("You are not permitted to read this item.")
            }

    override suspend fun getMany(transaction: Transaction, ids: Iterable<ID>): List<T> = underlying.getMany(transaction, ids)
            .mapNotNull { handleWholeItemRead(transaction, it, false) }

    override suspend fun insert(transaction: Transaction, model: T): T = underlying.insert(
            transaction,
            model.let {
                handleWholeItemUpdate(transaction, null, model)!!
            }
    ).let { item ->
        handleWholeItemRead(transaction, item, true) ?: throw ForbiddenException("You are not permitted to read this item.")
    }

    override suspend fun insertMany(transaction: Transaction, models: Collection<T>): Collection<T> = underlying.insertMany(
            transaction,
            models.also {
                it.mapNotNull {
                    handleWholeItemUpdate(transaction, null, it)
                }
            }
    ).mapNotNull { handleWholeItemRead(transaction, it, true) }

    override suspend fun update(transaction: Transaction, model: T): T = underlying.update(transaction, model.let {
        handleWholeItemUpdate(transaction, underlying.get(transaction, model.id!!), it)!!
    }).let { item ->
        handleWholeItemRead(transaction, item, false) ?: throw ForbiddenException("You are not permitted to read this item.")
    }

    override suspend fun modify(transaction: Transaction, id: ID, modifications: List<ModificationOnItem<T, *>>): T {
        val old = underlying.get(transaction, id)
        wholeWrite(transaction.untypedUser, false, old)
        val newModifications = modifications.map { typedMod ->
            val untypedRules = propertyRules[typedMod.field] as? PropertyRules<T, Any?> ?: return@map typedMod
            @Suppress("UNCHECKED_CAST") val untypedMod = typedMod as ModificationOnItem<T, Any?>
            val originalSet = untypedMod.invokeOnSub(untypedRules.variable.get(old))
            val newSet = untypedRules.write(
                    untypedUser = transaction.untypedUser,
                    currentState = old,
                    newState = originalSet
            )
            if (originalSet != newSet) {
                if (untypedMod is ModificationOnItem.Set) {
                    untypedMod.value = newSet
                    typedMod
                } else {
                    ModificationOnItem.Set(untypedRules.variable as FieldInfo<T, Any?>, newSet)
                }
            } else typedMod
        }
        return underlying.modify(transaction, id, newModifications).let { item ->
            handleWholeItemRead(transaction, item, false) ?: throw ForbiddenException("You are not permitted to read this item.")
        }
    }

    override suspend fun query(
            transaction: Transaction,
            condition: ConditionOnItem<T>,
            sortedBy: List<SortOnItem<T, *>>,
            continuationToken: String?,
            count: Int
    ): QueryResult<T> {
        wholeQuery(transaction.untypedUser)
        return underlying.query(
                transaction = transaction,
                condition = condition.also {
                    it.recursing().forEach {
                        if (it is ConditionOnItem.OnField<*, *>) {
                            propertyRules[it.field]?.query(transaction.untypedUser)
                        }
                    }
                },
                sortedBy = sortedBy.also {
                    it.forEach {
                        propertyRules[it.field]?.query(transaction.untypedUser)
                    }
                },
                continuationToken = continuationToken,
                count = count
        ).also {
            it.results = it.results.mapNotNull {
                handleWholeItemRead(transaction, it, false)
            }
        }
    }

    override suspend fun delete(transaction: Transaction, id: ID) {
        val old = underlying.get(transaction, id)
        handleWholeItemUpdate(transaction, old, null)
        return underlying.delete(transaction, id)
    }
}