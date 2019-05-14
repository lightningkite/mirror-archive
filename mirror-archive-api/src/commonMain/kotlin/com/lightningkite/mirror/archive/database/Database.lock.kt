package com.lightningkite.mirror.archive.database

import com.lightningkite.mirror.archive.model.*
import com.lightningkite.mirror.info.MirrorClass

suspend fun <T : Any, LOCK> Database<T>.withLock(
        lockField: MirrorClass.Field<T, LOCK>,
        lockDefault: LOCK,
        lockValue: LOCK,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        limit: Int = 100,
        action: suspend (List<T>)->Unit
): Int {
    this.limitedUpdate(
            condition = condition and (lockField equal lockDefault),
            operation = lockField setTo lockValue,
            sort = sort,
            limit = limit
    )
    val result = this.get(condition = lockField equal lockValue)
    try {
        action(result)
    } catch(t: Throwable){
        throw t
    } finally {
        this.update(
                condition = lockField equal lockValue,
                operation = lockField setTo lockDefault
        )
    }
    return result.size
}
suspend fun <T : Lockable<*>> Database<T>.withLock(
        mirror: MirrorClass<T>,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        limit: Int = 100,
        action: suspend (List<T>)->Unit
): Int {
    val lockField = mirror.fields.find { it.name == "lock" } as MirrorClass.Field<T, LockState>
    return withLock(
            lockField = lockField,
            lockDefault = LockState.UNLOCKED,
            lockValue = LockState.get(),
            condition = condition,
            sort = sort,
            limit = limit,
            action = action
    )
}


suspend fun <T : Any, LOCK> Database<T>.deleteAfterUsing(
        lockField: MirrorClass.Field<T, LOCK>,
        lockDefault: LOCK,
        lockValue: LOCK,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        limit: Int = 100,
        action: suspend (List<T>)->Unit
): Int {
    this.limitedUpdate(
            condition = condition and (lockField equal lockDefault),
            operation = lockField setTo lockValue,
            sort = sort,
            limit = limit
    )
    val result = this.get(condition = lockField equal lockValue)
    try {
        action(result)
        this.delete(lockField equal lockValue)
    } catch(t: Throwable){
        this.update(
                condition = lockField equal lockValue,
                operation = lockField setTo lockDefault
        )
        throw t
    }
    return result.size
}
suspend fun <T : Lockable<*>> Database<T>.deleteAfterUsing(
        mirror: MirrorClass<T>,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        limit: Int = 100,
        action: suspend (List<T>)->Unit
): Int {
    val lockField = mirror.fields.find { it.name == "lock" } as MirrorClass.Field<T, LockState>
    return deleteAfterUsing(
            lockField = lockField,
            lockDefault = LockState.UNLOCKED,
            lockValue = LockState.get(),
            condition = condition,
            sort = sort,
            limit = limit,
            action = action
    )
}


suspend fun <T : Any, LOCK> Database<T>.updateAfterUsing(
        lockField: MirrorClass.Field<T, LOCK>,
        lockDefault: LOCK,
        lockValue: LOCK,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        limit: Int = 100,
        completeOperation: Operation<T>,
        action: suspend (List<T>)->Unit
): Int {
    this.limitedUpdate(
            condition = condition and (lockField equal lockDefault),
            operation = lockField setTo lockValue,
            sort = sort,
            limit = limit
    )
    val result = this.get(condition = lockField equal lockValue)
    try {
        action(result)
        this.update(lockField equal lockValue, Operation.Multiple(listOf(completeOperation, lockField setTo lockDefault)))
    } catch(t: Throwable){
        this.update(
                condition = lockField equal lockValue,
                operation = lockField setTo lockDefault
        )
        throw t
    }
    return result.size
}
suspend fun <T : Lockable<*>> Database<T>.updateAfterUsing(
        mirror: MirrorClass<T>,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        limit: Int = 100,
        completeOperation: Operation<T>,
        action: suspend (List<T>)->Unit
): Int {
    val lockField = mirror.fields.find { it.name == "lock" } as MirrorClass.Field<T, LockState>
    return updateAfterUsing(
            lockField = lockField,
            lockDefault = LockState.UNLOCKED,
            lockValue = LockState.get(),
            condition = condition,
            sort = sort,
            limit = limit,
            completeOperation = completeOperation,
            action = action
    )
}


suspend fun <T : HasId<ID>, LOCK, ID> Database<T>.updateProgrammatic(
        idField: MirrorClass.Field<T, ID>,
        lockField: MirrorClass.Field<T, LOCK>,
        lockDefault: LOCK,
        lockValue: LOCK,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        getOperation: (T) -> Operation<T>,
        limit: Int = 100
): List<T> {
    this.limitedUpdate(
            condition = condition and (lockField equal lockDefault),
            operation = lockField setTo lockValue,
            sort = sort,
            limit = limit
    )
    val result = this.get(condition = lockField equal lockValue)
    result.groupBy { getOperation(it) }.forEach { (operation, targets) ->
        this.update(
                condition = idField equalToOne targets.map { idField.get(it) },
                operation = operation
        )
    }
    this.limitedUpdate(
            condition = condition and (lockField equal lockDefault),
            operation = lockField setTo lockValue,
            sort = sort,
            limit = limit
    )
    return result
}
suspend fun <T : Lockable<ID>, ID> Database<T>.updateProgrammatic(
        mirror: MirrorClass<T>,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        getOperation: (T) -> Operation<T>,
        limit: Int = 100
): List<T> {
    val lockField = mirror.fields.find { it.name == "lock" } as MirrorClass.Field<T, LockState>
    val idField = mirror.fields.find { it.name == "id" } as MirrorClass.Field<T, ID>
    return updateProgrammatic(
            idField = idField,
            lockField = lockField,
            lockDefault = LockState.UNLOCKED,
            lockValue = LockState.get(),
            condition = condition,
            sort = sort,
            getOperation = getOperation,
            limit = limit
    )
}


suspend fun <T : HasId<ID>, LOCK, ID> Database<T>.updateProgrammaticAfterUsing(
        idField: MirrorClass.Field<T, ID>,
        lockField: MirrorClass.Field<T, LOCK>,
        lockDefault: LOCK,
        lockValue: LOCK,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        getOperation: (T) -> Operation<T>,
        limit: Int = 100,
        action: suspend (List<T>)->Unit
): Int {
    this.limitedUpdate(
            condition = condition and (lockField equal lockDefault),
            operation = lockField setTo lockValue,
            sort = sort,
            limit = limit
    )
    val result = this.get(condition = lockField equal lockValue)
    try {
        action(result)
        result.groupBy { getOperation(it) }.forEach { (operation, targets) ->
            this.update(
                    condition = idField equalToOne targets.map { idField.get(it) },
                    operation = operation
            )
        }
        this.update(
                condition = lockField equal lockValue,
                operation = lockField setTo lockDefault
        )
    } catch(t: Throwable){
        this.update(
                condition = lockField equal lockValue,
                operation = lockField setTo lockDefault
        )
        throw t
    }
    return result.size
}
suspend fun <T : Lockable<ID>, ID> Database<T>.updateProgrammaticAfterUsing(
        mirror: MirrorClass<T>,
        condition: Condition<T>,
        sort: List<Sort<T, *>> = listOf(),
        getOperation: (T) -> Operation<T>,
        limit: Int = 100,
        action: suspend (List<T>)->Unit
): Int {
    val lockField = mirror.fields.find { it.name == "lock" } as MirrorClass.Field<T, LockState>
    val idField = mirror.fields.find { it.name == "id" } as MirrorClass.Field<T, ID>
    return updateProgrammaticAfterUsing(
            idField = idField,
            lockField = lockField,
            lockDefault = LockState.UNLOCKED,
            lockValue = LockState.get(),
            condition = condition,
            sort = sort,
            getOperation = getOperation,
            limit = limit,
            action = action
    )
}