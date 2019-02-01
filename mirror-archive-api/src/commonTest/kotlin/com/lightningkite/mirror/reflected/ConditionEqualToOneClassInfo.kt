//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.mirror.archive.model

import com.lightningkite.kommon.collection.treeWalkDepthSequence
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

@Suppress("RemoveExplicitTypeArguments", "UNCHECKED_CAST", "USELESS_CAST")
object ConditionEqualToOneClassInfo : ClassInfo<Condition.EqualToOne<*>> {

    override val kClass: KClass<Condition.EqualToOne<*>> = Condition.EqualToOne::class
    override val modifiers: List<ClassInfo.Modifier> = listOf(ClassInfo.Modifier.Data)
    override val companion: Any? get() = null

    override val implements: List<Type<*>> = listOf(Type<Condition<Any?>>(Condition::class, listOf(TypeProjection(Type<Any?>(Any::class, listOf(), false), TypeProjection.Variance.INVARIANT)), false))

    override val packageName: String = "com.lightningkite.mirror.archive.model"
    override val owner: KClass<*>? = Condition::class
    override val ownerName: String? = "Condition"

    override val name: String = "EqualToOne"
    override val annotations: List<AnnotationInfo> = listOf()
    override val enumValues: List<Condition.EqualToOne<*>>? = null

    val fieldValues = FieldInfo<Condition.EqualToOne<*>, List<Any?>>(this, "values", Type<List<Any?>>(List::class, listOf(TypeProjection(Type<Any?>(Any::class, listOf(), false), TypeProjection.Variance.INVARIANT)), false), false, { it.values as List<Any?> }, listOf())

    override val fields: List<FieldInfo<Condition.EqualToOne<*>, *>> = listOf(fieldValues)

    override fun construct(map: Map<String, Any?>): Condition.EqualToOne<Any?> {
        //Gather variables
        val values: List<Any?> = map["values"] as List<Any?>
        //Handle the optionals

        //Finally do the call
        return Condition.EqualToOne<Any?>(
                values = values
        )
    }

}
