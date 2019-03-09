//package com.lightningkite.mirror.archive.cache
//
//import com.lightningkite.kommon.property.MutablePropertyDelegate
//
//interface Cache {
//    operator fun <T> get(identifier: String): MutablePropertyDelegate<T>
//}
//
//interface SuspendingProperty<T> {
//    suspend fun get(): T
//    suspend fun set(value: T)
//
//}

/*

PARTS

- Shared Database
    - get(id, type)
        - suspend query
        - suspend insert
        - suspend update
        - suspend delete

- Shared Atomics
    - get(id, type)
        - suspend get
        - suspend set
        - suspend compareAndSet
        - suspend addAndGet

- Shared List
    - get(id, type)
        - suspend pushLeft
        - suspend pushRight
        - suspend popLeft
        - suspend popRight
        - suspend get
        - suspend set
        - suspend size
        - suspend trim
        - suspend insertBefore / insertAfter
        - suspend getRange

- Shared Set
    - get(id, type)
        - suspend add(score, value)
        - suspend size
        - suspend countBetween
        - suspend removeWithScoreRange
        - suspend removeByRank

- Shared Pub/Sub
    - suspend publish(channel, type, value)
    - subscribe(channel, type): Event
    - patternSubscribe(pattern, type): Event

By blending Shared Pub/Sub with Shared Atomics, we can get shared observable properties!
By blending Shared Pub/Sub with Shared List, we can get shared observable lists!

Declaration should end up like this:

ktor.servePubSubs(RedisPubSub(namespace = "public"))
ktor.serveAtomics(RedisAtomics(namespace = "public"))
ktor.serveLists(RedisLists(namespace = "public"))
ktor.serveSets(RedisSets(namespace = "public")

NO UGLY UGLY IDENTIFIERS.  CODE IS CODE

data class MyRequest(...): Request<Unit>
//Use RequestHandler

object MyDatabase: Database<Post>
//Use DatabaseHandler?  KtorDatabaseHandler(asdfasdf).query(MyDatabase)

object GlobalLock: SharedAtomic<Boolean>
//Use SharedAtomicHandler?

Separation of request and handler works, but fundamentally poses the problem of where the hell is this defined

Sometimes, some things will need to be just server side and sometimes global.  You'll need *two* separate handlers *and*
know when to use them, which just reintroduces the vague code use problem that string requests have.

Perhaps this?

data class MyClientRequest(...): Request<Unit>, ClientScope
val clientRequestHandler = KtorRequestHandler<ClientScope>(HttpClient())

Needed Code Fragments:

- RequestHandler
    - KtorRequestHandler
    - serveRequestH
- SharedDatabase
    - RedisDatabase
    - serveSharedDatabase(SharedDatabase)
    - KtorDatabase(HttpClient)
- SharedAtomics
    - RedisAtomics
    - serveSharedAtomics(SharedAtomics)
    - KtorAtomics(HttpClient)
- SharedSets
    - RedisSets
    - serveSharedSets(SharedSets)
    - KtorSets(HttpClient)
- SharedLists
    - RedisLists
    - serveSharedLists(SharedLists)
    - KtorLists(HttpClient)
- SharedPubSub
    - RedisPubSub
    - serveSharedPubSub(SharedPubSub)
    - KtorPubSub(HttpClient)




TYPE B


data class Database.Query<T>(database: Database, condition: Condition<T>, count: Int): List<T>
GENERICS DON'T CARRY OVER IN SERIALIZATION, run inefficiently due to addition of typing information everywhere
Polymorphism+ would take care of it?

This would make tons of sense from the client perspective, though.  In fact, this would allow for *every* other request to be
passed through as necessary.  The only hole is websockets...

Still doesn't answer questions about how this stuff works on the server side - still maybe makes sense to have an interface for realizing the object.

data EventForUser(val user: Int): PubSubRequest<String>
PubSubHandler().event(EventForUser(32)).add { string -> }

object PostsDatabase : DatabaseRequest<Post> { }

DOING THIS WOULD LOCK OFF THIS SECTION OF THE SYSTEM TO MIRROR.







Request
    - Request
    - RequestHandler
        - KtorRequestHandler
        - ServerRequestHandler
    - <Result>
DatabaseRequest
    - DatabaseRequest
    - DatabaseRequestHandler
        - RHDatabaseRequestHandler
        - PostgresDatabaseRequestHandler
        - Other databases...
    - Database
AtomicRequest
    - AtomicRequest
    - AtomicRequestHandler
        - RHAtomicRequestHandler
        - RedisHandler
    - Atomic
SetRequest
    - SetRequest
    - SetRequestHandler
        - RedisHandler
    - Set
ListRequest
    - ListRequest
    - ListRequestHandler
        - RedisHandler
    - List
PubSubRequest
    - PubSubRequest
    - PubSubRequestHandler
        - RedisHandler
    - PubSub




COMMON

data class User(...){
    inline class Token(val raw: String)
}

data class Post(...){
    class Database(val token: User.Token? = null) : DatabaseRequest<Post>
}


SERVER

object PostBackingDatabase: DatabaseRequest<Post>

val postgres = Postgres()
val DatabaseRequest<T>.backing get() = postgres.handle(this)

val localRequestHandler = LocalRequestHandler()
val localDatabaseHandler = LocalDatabaseHandler(localRequestHandler)

localDatabaseHandler.get<Post.Database> {
    PostBackingDatabase.backing.secure(
        ...
    )
}


object PostTasks: SharedListRequest<Post.Task>
val redis = Redis()
val <T> SharedListRequest<T>.backing = redis.invoke(this)



CLIENT

val requestHandler = RemoteRequestHandler("http://localhost:8080")
val dbHandler = requestHandler.db
suspend fun <T> Request<T>.invoke(): T = requestHandler.invoke(this)
val <T> DatabaseRequest<T>.remote get() = dbHandler.invoke(this)

val posts = RemoteDatabase.QueryRequest(
    database = Post.Database(myToken),
    count = 20
).invoke()





interface SocketRequest<T> {
    interface Handler {
        fun <T> invoke(socketRequest: SocketRequest<T>): Event<T>
    }
}





Overall Organization

- Mirror
    - Meta
        - Request
        - DatabaseRequest / Handler
        - AtomicReferenceRequest / Handler
        - SharedListRequest / Handler
        - EventRequestRequest / Handler
    - HttpClient Implementations
    - Serving the implementations

- KotlinX Serialization Databases
    - Migrations?????
    - Interfaces
        - Database
        - AtomicReference
    - FlatMapFormat
    - Redis
    - Postgres
    - Android SQLite
    - JVM SQLite
    - IndexedDB (JS)





Bite-size Tasks

- PostgresDatabaseHandler

- RedisAtomicReference
- SharedPreferencesAtomicReference
- LocalStorageAtomicReference

- SharedList
- SharedList.Request
- SharedList.Handler
- LocalSharedList
- RedisSharedList

- EventRequest
- EventRequest.Handler
- HttpClientEventRequestHandler
- LocalEventRequestHandler
- RedisEventRequestHandler



EVENT won't quite cut it - the stream could always get ended prematurely by the server side.
Event will only work when the socket is recovering, which should be done at another layer
Needs: connected, item, terminated
Connected can be done through an initial suspend on the function
Terminated though...

PartialObservableList through events and databases combined - intensely useful



*/