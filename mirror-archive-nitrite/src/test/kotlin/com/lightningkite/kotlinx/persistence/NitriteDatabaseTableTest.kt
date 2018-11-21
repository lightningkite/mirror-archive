package com.lightningkite.kotlinx.persistence

import com.lightningkite.mirror.archive.ModificationOnItem
import com.lightningkite.mirror.archive.Transaction
import com.lightningkite.mirror.archive.nitrite.NitriteDatabase
import com.lightningkite.mirror.archive.use
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
        val db = NitriteDatabase(Nitrite.builder()
                .compressed()
                .filePath("/tmp/test.db")
                .openOrCreate(), registry)
        val posts = db.table(Post::class)

        val post1 = Post(id = 0, title = "Post Title", body = "Here is a test post's content.")
        val post2 = Post(id = 1, title = "Another Post", body = "Here is more content!")
        val post3 = Post(id = 2, title = "A third post", body = "I'm not adding this one at first!")

        runBlocking {

            //Insert
            Transaction().use {
                posts.insert(it, post1)
                posts.insert(it, post2)
            }

            //Query, ID order expected by default
            Transaction().use {
                val results = posts.query(it)
                println(results)
                assert(results.results[0] == post1)
                assert(results.results[1] == post2)
                assert(results.results.size == 2)
            }

            //Modify and get
            Transaction().use {
                val newTitle = "Post Title Updated"
                posts.modify(it, 0, listOf(ModificationOnItem.Set(PostClassInfo.Fields.title, newTitle)))
                val result = posts.get(it, 0)
                assertEquals(newTitle, result.title)
            }

        }
    }
}
