package com.lightningkite.mirror.archive.sql

sealed class SQLCondition {
    open fun simplify(): SQLCondition = this

    class Never : SQLCondition()
    class Always : SQLCondition()

    data class And(val conditions: List<SQLCondition>): SQLCondition(){
        
        override fun simplify(): SQLCondition {
            val result = ArrayList<SQLCondition>()
            for(condition in conditions){
                val innerSimp = condition.simplify()
                when(innerSimp){
                    is Never -> return Never()
                    is Always -> {}
                    is And -> result.addAll(innerSimp.conditions)
                    else -> result.add(innerSimp)
                }
            }
            if(result.size == 1) return result.first()
            return SQLCondition.And(result)
        }
    }
    data class Or(val conditions: List<SQLCondition>): SQLCondition(){
        override fun simplify(): SQLCondition {
            val result = ArrayList<SQLCondition>()
            for(condition in conditions){
                val innerSimp = condition.simplify()
                when(innerSimp){
                    is Always -> return Always()
                    is Never -> {}
                    is Or -> result.addAll(innerSimp.conditions)
                    else -> result.add(innerSimp)
                }
            }
            if(result.size == 1) return result.first()
            return SQLCondition.Or(result)
        }
    }
    data class Not(val condition: SQLCondition): SQLCondition()
    data class Equal(val column: String, val value: String) : SQLCondition()
    data class EqualToOne(val column: String, val values: Collection<String>) : SQLCondition()
    data class NotEqual(val column: String, val value: String) : SQLCondition()
    data class LessThan(val column: String, val value: String) : SQLCondition()
    data class GreaterThan(val column: String, val value: String) : SQLCondition()
    data class LessThanOrEqual(val column: String, val value: String) : SQLCondition()
    data class GreaterThanOrEqual(val column: String, val value: String) : SQLCondition()
    data class TextSearch(val column: String, val query: String) : SQLCondition()
    data class RegexTextSearch(val column: String, val query: Regex) : SQLCondition()
}

infix fun <T> SQLCondition.and(other:SQLCondition):SQLCondition = SQLCondition.And(listOf(this, other)).simplify()
infix fun <T> SQLCondition.or(other:SQLCondition):SQLCondition = SQLCondition.Or(listOf(this, other)).simplify()
operator fun <T> SQLCondition.not() = SQLCondition.Not(this)