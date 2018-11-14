package com.lightningkite.kotlinx.persistence

import com.lightningkite.kotlinx.serialization.CommonSerialization
import kotlinx.coroutines.runBlocking
import org.dizitart.no2.Nitrite
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class NitriteDatabaseTableTest {

    @Test
    fun test() {
        CommonSerialization.ExternalNames.register(PostReflection)

        File("/tmp/test.db").let {
            it.parentFile.mkdirs()
            if (it.exists()) it.delete()
        }
        val db = NitriteDatabase(Nitrite.builder()
                .compressed()
                .filePath("/tmp/test.db")
                .openOrCreate())
        val posts = db.table(PostReflection)

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
                posts.modify(it, 0, listOf(ModificationOnItem.Set(PostReflection.Fields.title, newTitle)))
                val result = posts.get(it, 0)
                assertEquals(newTitle, result.title)
            }

        }
    }
}
