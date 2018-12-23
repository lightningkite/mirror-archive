package com.lightningkite.kotlinx.persistence

import com.lightningkite.mirror.archive.ModificationOnItem
import com.lightningkite.mirror.archive.nitrite.NitriteDatabase
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
            assert(post1 in results.results)
            assert(post2 in results.results)
            assert(results.results.size == 2)

            //Modify and get
            val newTitle = "Post Title Updated"
            posts.modify(post1.id, listOf(ModificationOnItem.Set(PostClassInfo.fieldTitle, newTitle)))
            val result = posts.get(post1.id)
            assertEquals(newTitle, result!!.title)

        }
    }
}
