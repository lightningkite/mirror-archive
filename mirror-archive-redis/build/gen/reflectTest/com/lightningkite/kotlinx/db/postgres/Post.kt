
package com.lightningkite.kotlinx.db.postgres

import com.lightningkite.kotlinx.reflection.*
import com.lightningkite.kotlinx.persistence.*
import com.lightningkite.kotlinx.reflection.*
import com.lightningkite.kotlinx.locale.TimeStamp
import com.lightningkite.kotlinx.locale.now
import com.lightningkite.kotlinx.server.ServerFunction

object PostGetReflection: KxClass<Post.Get> {
	object Fields {
		val id by lazy { 
			KxValue<Post.Get, Reference<Post, Long>>(
				owner = PostGetReflection,
				name = "id",
				type = 
					KxType(
						base = Reference::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							),
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.id as Reference<Post, Long> },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post.Get::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = ServerFunction::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
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
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post.Get"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post.Get"
	override val values: Map<String, KxValue<Post.Get, *>> by lazy {
		mapOf<String, KxValue<Post.Get, *>>("id" to Fields.id)
	}
	override val variables: Map<String, KxVariable<Post.Get, *>> by lazy {
		mapOf<String, KxVariable<Post.Get, *>>()
	}
	override val functions: List<KxFunction<*>> by lazy {
		listOf<KxFunction<*>>(
		)
	}
	override val constructors: List<KxFunction<Post.Get>> by lazy {
		listOf<KxFunction<Post.Get>>(
			KxFunction<Post.Get>(
				name = "Post.Get",
				type = 
					KxType(
						base = Post.Get::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				arguments = listOf(
					KxArgument(
						name = "id",
						type = 
							KxType(
								base = Reference::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									),
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = null
					)
				),
				call = { Post.Get(it[0] as Reference<Post, Long>) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post.Get>? = null
}

object PostInsertReflection: KxClass<Post.Insert> {
	object Fields {
		val value by lazy { 
			KxValue<Post.Insert, Post>(
				owner = PostInsertReflection,
				name = "value",
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
				get = { owner -> owner.value as Post },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post.Insert::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = ServerFunction::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
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
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post.Insert"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post.Insert"
	override val values: Map<String, KxValue<Post.Insert, *>> by lazy {
		mapOf<String, KxValue<Post.Insert, *>>("value" to Fields.value)
	}
	override val variables: Map<String, KxVariable<Post.Insert, *>> by lazy {
		mapOf<String, KxVariable<Post.Insert, *>>()
	}
	override val functions: List<KxFunction<*>> by lazy {
		listOf<KxFunction<*>>(
		)
	}
	override val constructors: List<KxFunction<Post.Insert>> by lazy {
		listOf<KxFunction<Post.Insert>>(
			KxFunction<Post.Insert>(
				name = "Post.Insert",
				type = 
					KxType(
						base = Post.Insert::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				arguments = listOf(
					KxArgument(
						name = "value",
						type = 
							KxType(
								base = Post::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = null
					)
				),
				call = { Post.Insert(it[0] as Post) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post.Insert>? = null
}

object PostUpdateReflection: KxClass<Post.Update> {
	object Fields {
		val value by lazy { 
			KxValue<Post.Update, Post>(
				owner = PostUpdateReflection,
				name = "value",
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
				get = { owner -> owner.value as Post },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post.Update::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = ServerFunction::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
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
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post.Update"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post.Update"
	override val values: Map<String, KxValue<Post.Update, *>> by lazy {
		mapOf<String, KxValue<Post.Update, *>>("value" to Fields.value)
	}
	override val variables: Map<String, KxVariable<Post.Update, *>> by lazy {
		mapOf<String, KxVariable<Post.Update, *>>()
	}
	override val functions: List<KxFunction<*>> by lazy {
		listOf<KxFunction<*>>(
		)
	}
	override val constructors: List<KxFunction<Post.Update>> by lazy {
		listOf<KxFunction<Post.Update>>(
			KxFunction<Post.Update>(
				name = "Post.Update",
				type = 
					KxType(
						base = Post.Update::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				arguments = listOf(
					KxArgument(
						name = "value",
						type = 
							KxType(
								base = Post::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = null
					)
				),
				call = { Post.Update(it[0] as Post) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post.Update>? = null
}

object PostModifyReflection: KxClass<Post.Modify> {
	object Fields {
		val id by lazy { 
			KxValue<Post.Modify, Reference<Post, Long>>(
				owner = PostModifyReflection,
				name = "id",
				type = 
					KxType(
						base = Reference::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							),
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.id as Reference<Post, Long> },
				artificial = false,
				annotations = listOf(
				)
			)
		}
		val modifications by lazy { 
			KxValue<Post.Modify, List<ModificationOnItem<Post, *>>>(
				owner = PostModifyReflection,
				name = "modifications",
				type = 
					KxType(
						base = List::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
							KxTypeProjection(
								type = 
									KxType(
										base = ModificationOnItem::class.kxReflect,
										nullable = false,
										typeParameters = listOf(
											KxTypeProjection(
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
												variance = KxVariance.INVARIANT
											),
											KxTypeProjection.STAR
										),
										annotations = listOf(
										)
									)
								,
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.modifications as List<ModificationOnItem<Post, *>> },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post.Modify::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = ServerFunction::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
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
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post.Modify"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post.Modify"
	override val values: Map<String, KxValue<Post.Modify, *>> by lazy {
		mapOf<String, KxValue<Post.Modify, *>>("id" to Fields.id, "modifications" to Fields.modifications)
	}
	override val variables: Map<String, KxVariable<Post.Modify, *>> by lazy {
		mapOf<String, KxVariable<Post.Modify, *>>()
	}
	override val functions: List<KxFunction<*>> by lazy {
		listOf<KxFunction<*>>(
		)
	}
	override val constructors: List<KxFunction<Post.Modify>> by lazy {
		listOf<KxFunction<Post.Modify>>(
			KxFunction<Post.Modify>(
				name = "Post.Modify",
				type = 
					KxType(
						base = Post.Modify::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				arguments = listOf(
					KxArgument(
						name = "id",
						type = 
							KxType(
								base = Reference::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									),
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = null
					),
					KxArgument(
						name = "modifications",
						type = 
							KxType(
								base = List::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
										type = 
											KxType(
												base = ModificationOnItem::class.kxReflect,
												nullable = false,
												typeParameters = listOf(
													KxTypeProjection(
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
														variance = KxVariance.INVARIANT
													),
													KxTypeProjection.STAR
												),
												annotations = listOf(
												)
											)
										,
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = null
					)
				),
				call = { Post.Modify(it[0] as Reference<Post, Long>, it[1] as List<ModificationOnItem<Post, *>>) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post.Modify>? = null
}

object PostQueryReflection: KxClass<Post.Query> {
	object Fields {
		val condition by lazy { 
			KxValue<Post.Query, ConditionOnItem<Post>>(
				owner = PostQueryReflection,
				name = "condition",
				type = 
					KxType(
						base = ConditionOnItem::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.condition as ConditionOnItem<Post> },
				artificial = false,
				annotations = listOf(
				)
			)
		}
		val sortedBy by lazy { 
			KxValue<Post.Query, List<SortOnItem<Post, *>>>(
				owner = PostQueryReflection,
				name = "sortedBy",
				type = 
					KxType(
						base = List::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
							KxTypeProjection(
								type = 
									KxType(
										base = SortOnItem::class.kxReflect,
										nullable = false,
										typeParameters = listOf(
											KxTypeProjection(
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
												variance = KxVariance.INVARIANT
											),
											KxTypeProjection.STAR
										),
										annotations = listOf(
										)
									)
								,
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.sortedBy as List<SortOnItem<Post, *>> },
				artificial = false,
				annotations = listOf(
				)
			)
		}
		val continuationToken by lazy { 
			KxValue<Post.Query, String?>(
				owner = PostQueryReflection,
				name = "continuationToken",
				type = 
					KxType(
						base = String::class.kxReflect,
						nullable = true,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.continuationToken as String? },
				artificial = false,
				annotations = listOf(
				)
			)
		}
		val count by lazy { 
			KxValue<Post.Query, Int>(
				owner = PostQueryReflection,
				name = "count",
				type = 
					KxType(
						base = Int::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.count as Int },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post.Query::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = ServerFunction::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
						type = 
							KxType(
								base = QueryResult::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							)
						,
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post.Query"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post.Query"
	override val values: Map<String, KxValue<Post.Query, *>> by lazy {
		mapOf<String, KxValue<Post.Query, *>>("condition" to Fields.condition, "sortedBy" to Fields.sortedBy, "continuationToken" to Fields.continuationToken, "count" to Fields.count)
	}
	override val variables: Map<String, KxVariable<Post.Query, *>> by lazy {
		mapOf<String, KxVariable<Post.Query, *>>()
	}
	override val functions: List<KxFunction<*>> by lazy {
		listOf<KxFunction<*>>(
		)
	}
	override val constructors: List<KxFunction<Post.Query>> by lazy {
		listOf<KxFunction<Post.Query>>(
			KxFunction<Post.Query>(
				name = "Post.Query",
				type = 
					KxType(
						base = Post.Query::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				arguments = listOf(
					KxArgument(
						name = "condition",
						type = 
							KxType(
								base = ConditionOnItem::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): ConditionOnItem<Post> { return ConditionOnItem.Always() }
					),
					KxArgument(
						name = "sortedBy",
						type = 
							KxType(
								base = List::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
										type = 
											KxType(
												base = SortOnItem::class.kxReflect,
												nullable = false,
												typeParameters = listOf(
													KxTypeProjection(
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
														variance = KxVariance.INVARIANT
													),
													KxTypeProjection.STAR
												),
												annotations = listOf(
												)
											)
										,
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): List<SortOnItem<Post, *>> { return listOf() }
					),
					KxArgument(
						name = "continuationToken",
						type = 
							KxType(
								base = String::class.kxReflect,
								nullable = true,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): String? { return null }
					),
					KxArgument(
						name = "count",
						type = 
							KxType(
								base = Int::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): Int { return 100 }
					)
				),
				call = { Post.Query(it[0] as ConditionOnItem<Post>, it[1] as List<SortOnItem<Post, *>>, it[2] as String?, it[3] as Int) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post.Query>? = null
}

object PostDeleteReflection: KxClass<Post.Delete> {
	object Fields {
		val id by lazy { 
			KxValue<Post.Delete, Reference<Post, Long>>(
				owner = PostDeleteReflection,
				name = "id",
				type = 
					KxType(
						base = Reference::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							),
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.id as Reference<Post, Long> },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post.Delete::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = ServerFunction::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
						type = 
							KxType(
								base = Unit::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							)
						,
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post.Delete"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post.Delete"
	override val values: Map<String, KxValue<Post.Delete, *>> by lazy {
		mapOf<String, KxValue<Post.Delete, *>>("id" to Fields.id)
	}
	override val variables: Map<String, KxVariable<Post.Delete, *>> by lazy {
		mapOf<String, KxVariable<Post.Delete, *>>()
	}
	override val functions: List<KxFunction<*>> by lazy {
		listOf<KxFunction<*>>(
		)
	}
	override val constructors: List<KxFunction<Post.Delete>> by lazy {
		listOf<KxFunction<Post.Delete>>(
			KxFunction<Post.Delete>(
				name = "Post.Delete",
				type = 
					KxType(
						base = Post.Delete::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				arguments = listOf(
					KxArgument(
						name = "id",
						type = 
							KxType(
								base = Reference::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									),
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = null
					)
				),
				call = { Post.Delete(it[0] as Reference<Post, Long>) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post.Delete>? = null
}

object PostReflection: KxClass<Post> {
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
				artificial = false,
				annotations = listOf(
					KxAnnotation(
						name = "Indexed",
						arguments = listOf()
					)
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
				artificial = false,
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
				artificial = false,
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
				artificial = false,
				annotations = listOf(
				)
			)
		}
		val parent by lazy { 
			KxVariable<Post, Reference<Post, Long>?>(
				owner = PostReflection,
				name = "parent",
				type = 
					KxType(
						base = Reference::class.kxReflect,
						nullable = true,
						typeParameters = listOf(
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							),
							KxTypeProjection(
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
								variance = KxVariance.INVARIANT
							)
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.parent as Reference<Post, Long>? },
				set = { owner, value -> owner.parent = value },
				artificial = false,
				annotations = listOf(
				)
			)
		}
		val time by lazy { 
			KxVariable<Post, TimeStamp>(
				owner = PostReflection,
				name = "time",
				type = 
					KxType(
						base = TimeStamp::class.kxReflect,
						nullable = false,
						typeParameters = listOf(
						),
						annotations = listOf(
						)
					)
				,
				get = { owner -> owner.time as TimeStamp },
				set = { owner, value -> owner.time = value },
				artificial = false,
				annotations = listOf(
				)
			)
		}
	}
	override val kclass get() = Post::class
	override val implements: List<KxType> by lazy {
		listOf<KxType>(
			KxType(
				base = Model::class.kxReflect,
				nullable = false,
				typeParameters = listOf(
					KxTypeProjection(
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
						variance = KxVariance.INVARIANT
					)
				),
				annotations = listOf(
				)
			)
		)
	}
	override val simpleName: String = "Post"
	override val qualifiedName: String = "com.lightningkite.kotlinx.db.postgres.Post"
	override val values: Map<String, KxValue<Post, *>> by lazy {
		mapOf<String, KxValue<Post, *>>()
	}
	override val variables: Map<String, KxVariable<Post, *>> by lazy {
		mapOf<String, KxVariable<Post, *>>("userId" to Fields.userId, "id" to Fields.id, "title" to Fields.title, "body" to Fields.body, "parent" to Fields.parent, "time" to Fields.time)
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
						default = fun(previousArguments: List<Any?>): Long { return 0 }
					),
					KxArgument(
						name = "id",
						type = 
							KxType(
								base = Long::class.kxReflect,
								nullable = true,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): Long? { return null }
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
						default = fun(previousArguments: List<Any?>): String { return "" }
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
						default = fun(previousArguments: List<Any?>): String { return "" }
					),
					KxArgument(
						name = "parent",
						type = 
							KxType(
								base = Reference::class.kxReflect,
								nullable = true,
								typeParameters = listOf(
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									),
									KxTypeProjection(
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
										variance = KxVariance.INVARIANT
									)
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): Reference<Post, Long>? { return null }
					),
					KxArgument(
						name = "time",
						type = 
							KxType(
								base = TimeStamp::class.kxReflect,
								nullable = false,
								typeParameters = listOf(
								),
								annotations = listOf(
								)
							),
						annotations = listOf(
						),
						default = fun(previousArguments: List<Any?>): TimeStamp { return TimeStamp.now() }
					)
				),
				call = { Post(it[0] as Long, it[1] as Long?, it[2] as String, it[3] as String, it[4] as Reference<Post, Long>?, it[5] as TimeStamp) },
				annotations = listOf(
				)
			)
		)
	}
	override val annotations: List<KxAnnotation> = listOf<KxAnnotation>(
		KxAnnotation(
			name = "ExternalReflection",
			arguments = listOf()
		)
	)
	override val modifiers: List<KxClassModifier> = listOf<KxClassModifier>(KxClassModifier.Data)
	override val enumValues: List<Post>? = null
}

