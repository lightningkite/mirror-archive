//Generated by Lightning Kite's Mirror plugin
//AUTOMATICALLY GENERATED AND WILL BE OVERRIDDEN IF THIS MESSAGE IS PRESENT
package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.mirror.archive.HasId
import com.lightningkite.mirror.archive.Id
import com.lightningkite.mirror.info.*
import kotlin.reflect.KClass

@Suppress("RemoveExplicitTypeArguments", "UNCHECKED_CAST", "USELESS_CAST")
object PostClassInfo: ClassInfo<Post> {

   override val kClass: KClass<Post> = Post::class
   override val modifiers: List<ClassInfo.Modifier> = listOf(ClassInfo.Modifier.Data)

   override val implements: List<Type<*>> = listOf(Type<HasId>(HasId::class, listOf(), false))

   override val packageName: String = "com.lightningkite.kotlinx.db.postgres"
   override val owner: KClass<*>? = null
   override val ownerName: String? = null

   override val name: String = "Post"
   override val annotations: List<AnnotationInfo> = listOf()
   override val enumValues: List<Post>? = null

   object Fields {
       val id = FieldInfo<Post, Id>(PostClassInfo, "id", Type<Id>(Id::class, listOf(), false), true, { it.id as Id}, listOf())
        val userId = FieldInfo<Post, Long>(PostClassInfo, "userId", Type<Long>(Long::class, listOf(), false), true, { it.userId as Long}, listOf())
        val title = FieldInfo<Post, String>(PostClassInfo, "title", Type<String>(String::class, listOf(), false), true, { it.title as String}, listOf())
        val body = FieldInfo<Post, String>(PostClassInfo, "body", Type<String>(String::class, listOf(), false), true, { it.body as String}, listOf())
   }

   override val fields:List<FieldInfo<Post, *>> = listOf(Fields.id, Fields.userId, Fields.title, Fields.body)

   override fun construct(map: Map<String, Any?>): Post {
       //Gather variables
       
           //Handle the optionals
       val id:Id = map["id"] as? Id ?: Id.key()
        val userId:Long = map["userId"] as? Long ?: 0
        val title:String = map["title"] as? String ?: ""
        val body:String = map["body"] as? String ?: ""
       //Finally do the call
       return Post(
           id = id,
            userId = userId,
            title = title,
            body = body
       )
   }

}