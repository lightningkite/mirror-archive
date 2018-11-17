package com.lightningkite.mirror.archive.postgres

import com.lightningkite.kommunicate.ConnectionException
import com.lightningkite.kommunicate.HttpClient
import okhttp3.*
import io.reactiverse.pgclient.PgClient
import io.reactiverse.pgclient.PgConnectOptions
import io.reactiverse.pgclient.PgPool
import io.reactiverse.pgclient.PgPoolOptions
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.lang.IllegalStateException
import java.util.concurrent.TimeUnit
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object EmbeddedPG {

    //Download Layer

    fun downloadUrl(version: String, platform: Platform) = "https://get.enterprisedb.com/postgresql/postgresql-$version-${platform.stringName}-binaries.zip?ls=Crossover&type=Crossover"

    object Versions {
        const val VERSION_10 = "10.5-1"
        const val VERSION_9 = "9.6.10-1"
    }

    enum class Platform(val stringName: String) {
        WINDOWS_64("windows-x64"),
        WINDOWS_32("windows-x32"),
        LINUX_64("linux-x64"),
        LINUX_32("linux-x32"),
        OSX("osx"),
    }

    fun getPlatform(): Platform? {
        val osString = System.getProperty("os.name").toLowerCase()
        return when {
            osString.contains("win") -> {
                val arch = System.getProperty("os.arch").toLowerCase()
                when {
                    arch.contains("x86") -> Platform.WINDOWS_32
                    arch.contains("64") -> Platform.WINDOWS_64
                    else -> null
                }
            }
            osString.contains("linux") -> {
                val arch = System.getProperty("os.arch").toLowerCase()
                when {
                    arch.contains("x86") -> Platform.LINUX_32
                    arch.contains("64") -> Platform.LINUX_64
                    else -> null
                }
            }
            osString.contains("mac") -> Platform.OSX
            else -> null
        }
    }

    suspend fun download(version: String, toFolder: File) = suspendCoroutine<Unit> { callback ->
        val url = getPlatform()?.let { downloadUrl(version, it) } ?: throw IllegalStateException("Unknown platform")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        HttpClient.okClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback.resumeWithException(ConnectionException(e.message ?: "", e))
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.code() / 100 != 2) callback.resumeWithException(ConnectionException(""))
                response.body()!!.byteStream().use {
                    val buffer = ByteArray(1024)
                    val zipIs = ZipInputStream(it)
                    var entry: ZipEntry? = zipIs.nextEntry
                    while (entry != null) {
                        val newFile = File(toFolder, entry.name)
                        newFile.parentFile.mkdirs()
                        if (entry.isDirectory) {
                            if (newFile.exists() && !newFile.isDirectory) {
                                newFile.delete()
                            }
                            newFile.mkdir()
                        } else {
                            if (newFile.exists() && newFile.isDirectory) newFile.delete()
                            if (!newFile.exists()) newFile.createNewFile()
                            FileOutputStream(newFile, false).use { fileOut ->
                                var len = 0
                                while (true) {
                                    len = zipIs.read(buffer)
                                    if (len < 0) break
                                    fileOut.write(buffer, 0, len)
                                }
                            }
                        }
                        zipIs.closeEntry()
                        entry = zipIs.nextEntry
                    }
                    zipIs.closeEntry()
                    zipIs.close()
                }
                callback.resume(Unit)
            }

        })
    }

    //Basic Execution Layer

    fun pgCtlExecute(location: File, options: List<String>){
        val command = listOf(location.absolutePath) + options
        println("Executing ${command.joinToString(" ")}")
        ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
                .waitFor(20, TimeUnit.SECONDS)
    }

    fun start(base: File, storeFiles: File, port:Int = 5432){
        storeFiles.mkdirs()
        val executable = File(base, "pgsql/bin/pg_ctl")
        val options = listOf(
                listOf("-D", storeFiles.absolutePath, "-o", "-U postgres", "initdb"),
                listOf("-w", "-D", storeFiles.absolutePath, "-l", "logfile", "-o", "-p $port", "start")
        )
        for(set in options){
            pgCtlExecute(executable, set)
        }
    }

    fun stop(base: File, storeFiles: File){
        val executable = File(base, "pgsql/bin/pg_ctl")
        val options = listOf(
                listOf("-D", storeFiles.absolutePath, "stop")
        )
        for(set in options){
            pgCtlExecute(executable, set)
        }
    }

    //Pool layer
    class PoolProvider(val cache: File, val version: String, val storeFiles: File, val port:Int = 5432){
        val base = File(cache, version)
        fun start(): PgPool{
            println("Base: $base")
            if(!base.exists()){
                base.mkdirs()
                runBlocking {
                    download(version, base)
                }
            }
            println("Files found!")
            start(base, storeFiles, port)
            return PgClient.pool(PgPoolOptions(PgConnectOptions().also{
                it.host = "localhost"
                it.port = port
                it.user = "postgres"
                it.password = "postgres"
                it.database = "postgres"
            }))
        }
        fun stop(){
            EmbeddedPG.stop(base, storeFiles)
        }
    }
}