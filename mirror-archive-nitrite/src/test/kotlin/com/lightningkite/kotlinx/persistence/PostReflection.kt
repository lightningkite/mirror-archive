package com.lightningkite.kotlinx.persistence

import com.lightningkite.kotlinx.persistence.Model
import com.lightningkite.kotlinx.reflection.*

@ExternalReflection
data class Post(
        var userId: Long = 0,
        override var id: Long? = null,
        var title: String = "",
        var body: String = ""
): Model<Long>

object PostReflection : KxClass<Post> {
    object Fields {
        val userId by lazy {
            KxVariable<Post, Long>(
                    owner = PostReflection,
                    name = "userId",
                    type =
                    KxType(
                            base = Long::class.kxReflect,
                            nullable = false,
                            typeParameters = listOf(
                            ),
                            annotations = listOf(
                            )
                    )
                    ,
                    get = { owner -> owner.userId as Long },
                    set = { owner, value -> owner.userId = value },
                    annotations = listOf(
                    )
            )
        }
        val id by lazy {
            KxVariable<Post, Long?>(
                    owner = PostReflection,
                    name = "id",
                    type =
                    KxType(
                            base = Long::class.kxReflect,
                            nullable = true,
                            typeParameters = listOf(
                            ),
                            annotations = listOf(
                            )
                    )
                    ,
                    get = { owner -> owner.id as Long? },
                    set = { owner, value -> owner.id = value },
                    annotations = listOf(
                    )
            )
        }
        val title by lazy {
            KxVariable<Post, String>(
                    owner = PostReflection,
                    name = "title",
                    type =
                    KxType(
                            base = String::class.kxReflect,
                            nullable = false,
                            typeParameters = listOf(
                            ),
                            annotations = listOf(
                            )
                    )
                    ,
                    get = { owner -> owner.title as String },
                    set = { owner, value -> owner.title = value },
                    annotations = listOf(
                    )
            )
        }
        val body by lazy {
            KxVariable<Post, String>(
                    owner = PostReflection,
                    name = "body",
                    type =
                    KxType(
                            base = String::class.kxReflect,
                            nullable = false,
                            typeParameters = listOf(
                            ),
                            annotations = listOf(
                            )
                    )
                    ,
                    get = { owner -> owner.body as String },
                    set = { owner, value -> owner.body = value },
                    annotations = listOf(
                    )
            )
        }
    }
    override val kclass get() = Post::class
    override val implements: List<KxType> by lazy {
        listOf<KxType>(
        )
    }
    override val simpleName: String = "Post"
    override val qualifiedName: String = "com.lightningkite.kotlinx.reflection.plugin.test.Post"
    override val values: Map<String, KxValue<Post, *>> by lazy {
        mapOf<String, KxValue<Post, *>>()
    }
    override val variables: Map<String, KxVariable<Post, *>> by lazy {
        mapOf<String, KxVariable<Post, *>>("userId" to Fields.userId, "id" to Fields.id, "title" to Fields.title, "body" to Fields.body)
    }
    override val functions: List<KxFunction<*>> by lazy {
        listOf<KxFunction<*>>(
        )
    }
    override val constructors: List<KxFunction<Post>> by lazy {
        listOf<KxFunction<Post>>(
                KxFunction<Post>(
                        name = "Post",
                        type =
                        KxType(
                                base = Post::class.kxReflect,
                                nullable = false,
                                typeParameters = listOf(
                                ),
                                annotations = listOf(
                                )
                        )
                        ,
                        arguments = listOf(
                                KxArgument(
                                        name = "userId",
                                        type =
                                        KxType(
                                                base = Long::class.kxReflect,
                                                nullable = false,
                                                typeParameters = listOf(
                                                ),
                                                annotations = listOf(
                                                )
                                        ),
                                        annotations = listOf(
                                        ),
                                        default = { previousArguments -> 0 }
                                ),
                                KxArgument(
                                        name = "id",
                                        type =
                                        KxType(
                                                base = Long::class.kxReflect,
                                                nullable = false,
                                                typeParameters = listOf(
                                                ),
                                                annotations = listOf(
                                                )
                                        ),
                                        annotations = listOf(
                                        ),
                                        default = { previousArguments -> 0 }
                                ),
                                KxArgument(
                                        name = "title",
                                        type =
                                        KxType(
                                                base = String::class.kxReflect,
                                                nullable = false,
                                                typeParameters = listOf(
                                                ),
                                                annotations = listOf(
                                                )
                                        ),
                                        annotations = listOf(
                                        ),
                                        default = { previousArguments -> "" }
                                ),
                                KxArgument(
                                        name = "body",
                                        type =
                                        KxType(
                                                base = String::class.kxReflect,
                                                nullable = false,
                                                typeParameters = listOf(
                                                ),
                                                annotations = listOf(
                                                )
                                        ),
                                        annotations = listOf(
                                        ),
                                        default = { previousArguments -> "" }
                                )
                        ),
                        call = { Post(it[0] as Long, it[1] as Long, it[2] as String, it[3] as String) },
                        annotations = listOf(
                        )
                )
        )
    }
    override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
    )
    override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
    override val enumValues: List<Post>? = null
}

