package com.lightningkite.kotlinx.persistence

import com.lightningkite.mirror.archive.database.insert
import com.lightningkite.mirror.archive.model.Id
import com.lightningkite.mirror.archive.model.Operation
import com.lightningkite.mirror.archive.nitrite.NitriteSuspendMap
import com.lightningkite.mirror.info.FieldInfo
import com.lightningkite.mirror.info.type
import com.lightningkite.mirror.serialization.DefaultRegistry
import kotlinx.coroutines.runBlocking
import org.dizitart.no2.Nitrite
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NitriteDatabaseTableTest {

    @Test
    fun test() {
        val registry = DefaultRegistry + TestRegistry

        File("/tmp/test.db").let {
            it.parentFile.mkdirs()
            if (it.exists()) it.delete()
        }
        val db = NitriteSuspendMap.Provider(Nitrite.builder()
                .compressed()
                .filePath("/tmp/test.db")
                .openOrCreate(), registry)
        val posts = db.suspendMap(Id::class.type, Post::class.type)

        val post1 = Post(title = "Post Title", body = "Here is a test post's content.")
        val post2 = Post(title = "Another Post", body = "Here is more content!")
        val post3 = Post(title = "A third post", body = "I'm not adding this one at first!")

        runBlocking {

            //Insert
            posts.insert(post1)
            posts.insert(post2)

            //Query, ID order expected by default
            val results = posts.query()
            println(results)
            assert(results.any { it.second == post1 })
            assert(results.any { it.second == post2 })
            assert(results.size == 2)

            //Modify and get
            val newTitle = "Post Title Updated"
            val modifiedPost1 = post1.copy(title = newTitle)
            posts.insert(modifiedPost1)
            val result = posts.get(post1.id)
            assertEquals(newTitle, result!!.title)

        }
    }
}
