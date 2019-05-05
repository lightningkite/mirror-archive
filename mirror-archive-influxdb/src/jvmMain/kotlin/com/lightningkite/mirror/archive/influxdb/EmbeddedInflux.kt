package com.lightningkite.mirror.archive.influxdb

import kotlinx.coroutines.runBlocking
import okhttp3.*
import org.influxdb.InfluxDB
import org.influxdb.InfluxDBFactory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.IllegalStateException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object EmbeddedInflux {

    //Download Layer

    fun downloadUrl(version: String, platform: Platform) = "https://dl.influxdata.com/influxdb/releases/influxdb-${version}_${platform.stringName}.zip"

    object Versions {
        const val VERSION_1_7_2 = "1.7.2"
    }

    enum class Platform(val stringName: String) {
        WINDOWS_64("windows_amd64"),
        WINDOWS_32("windows_i386"),
        LINUX_64("linux_amd64"),
        LINUX_32("linux_i386"),
        OSX("darwin_amd64"),
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
        println("Downloading Influx from $url")
        val request = Request.Builder()
                .url(url)
                .get()
                .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                callback.resumeWithException(IllegalStateException(e.message ?: "", e))
            }

            override fun onResponse(call: Call, response: Response) {
                if(response.code() / 100 != 2) callback.resumeWithException(IllegalStateException(""))
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

    var process: Process? = null

    fun start(
            cache: File = File("/influx-cache"),
            version: String = Versions.VERSION_1_7_2,
            storeFiles: File = File("build/working/influx"),
            clearFiles: Boolean = false,
            port:Int = 5433
    ): InfluxDB {
        val downloadLocation = File(cache, version)
        if(!downloadLocation.exists() || downloadLocation.list().isEmpty()){
            downloadLocation.mkdirs()
            runBlocking {
                download(version, downloadLocation)
            }
        }
        val base = downloadLocation.listFiles().filter { it.isDirectory }.first()

        stop()

        if(clearFiles && storeFiles.exists()){
            storeFiles.deleteRecursively()
        }
        storeFiles.mkdirs()

        val executable = File(base, "influxd")
        val defaultConfig = File(base, "influxdb.conf")
        val myConfig = File(storeFiles, "config.conf")
        val confText = defaultConfig.readText()
                .replace("/var/lib/influxdb", storeFiles.absolutePath.replace('\\', '/'))
                .replace("""# bind-address = ":8086"""", """bind-address = ":$port" """)
        myConfig.writeText(confText)

        val command = listOf(executable.absolutePath, "-config", myConfig.absolutePath)
        println("Executing ${command.joinToString(" ")}")
        process = ProcessBuilder(command)
                .redirectOutput(ProcessBuilder.Redirect.INHERIT)
                .redirectError(ProcessBuilder.Redirect.INHERIT)
                .start()
        Thread.sleep(1_000L) //Wait for it to start up
        val url = "http://localhost:$port"
        println("Connecting to $url")
        return InfluxDBFactory.connect(url)
    }

    fun stop(){
        process?.destroy()
        process = null
    }
}