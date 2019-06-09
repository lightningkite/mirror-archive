package com.lightningkite.mirror.archive.postgres

import com.lightningkite.mirror.archive.database.Database
import com.lightningkite.mirror.test.Post
import com.lightningkite.mirror.test.PostMirror
import com.lightningkite.mirror.test.Zoo
import com.lightningkite.mirror.test.ZooMirror
import kotlinx.coroutines.runBlocking
import org.junit.AfterClass
import org.junit.BeforeClass
import org.junit.Test
import java.io.File

class MigrationTest {

    companion object {

        lateinit var database: PostgresDatabase<Post>
        var provider: EmbeddedPG.PoolProvider? = null

        @JvmStatic
        @BeforeClass
        fun before() {
            provider = EmbeddedPG.PoolProvider(
                    cache = File("/pgcache"),
                    version = EmbeddedPG.Versions.VERSION_10,
                    clearBeforeStarting = true,
                    storeFiles = File("build/pg")
            )
            database = PostgresDatabase<Post>(
                    mirror = PostMirror,
                    default = Post(),
                    client = provider!!.start()
            )
        }

        @JvmStatic
        @AfterClass
        fun after() {
            provider?.stop()
            provider = null
        }

    }

    @Test
    fun testAddColumn(){
        runBlocking {
            database.setupState.value = 0
            database.client.suspendQuery("DROP TABLE IF EXISTS mySchema.Post;")
            database.client.suspendQuery("CREATE SCHEMA IF NOT EXISTS mySchema;")
            database.client.suspendQuery("CREATE TABLE mySchema.Post (field_id int8, title text, body text);")
            database.client.suspendQuery("INSERT INTO mySchema.Post (field_id, title, body) VALUES (123, 'TITLE', 'BODY');")
            database.setup()
        }
    }

    @Test
    fun testAddRemoveColumn(){
        runBlocking {
            database.setupState.value = 0
            database.client.suspendQuery("DROP TABLE IF EXISTS mySchema.Post;")
            database.client.suspendQuery("CREATE SCHEMA IF NOT EXISTS mySchema;")
            database.client.suspendQuery("CREATE TABLE mySchema.Post (field_id int8, title text, body text, dumb_value text);")
            database.client.suspendQuery("INSERT INTO mySchema.Post (field_id, title, body, dumb_value) VALUES (123, 'TITLE', 'BODY', 'dumb');")
            database.setup()
        }
    }
}