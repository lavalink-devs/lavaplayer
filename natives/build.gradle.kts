import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import org.apache.tools.ant.taskdefs.condition.Os
import java.net.HttpURLConnection
import java.net.URI
import java.util.Properties

plugins {
    `java-library`
    alias(libs.plugins.maven.publish.base)
}

base {
    archivesName.set("lavaplayer-natives")
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc()))
}

val versionProps = Properties().apply {
    file("$projectDir/versions.properties").inputStream().use { load(it) }
}

val opusVersion = versionProps["opus"] as String
val mpg123Version = versionProps["mpg123"] as String
val oggVersion = versionProps["ogg"] as String
val vorbisVersion = versionProps["vorbis"] as String
val sampleRateVersion = versionProps["samplerate"] as String
val fdkAacVersion = versionProps["fdkaac"] as String

fun downloadFile(url: String, dest: String) {
    val destFile = file(dest)
    destFile.parentFile.mkdirs()
    val connection = URI(url).toURL().openConnection() as HttpURLConnection
    connection.instanceFollowRedirects = true
    connection.inputStream.use { input ->
        destFile.outputStream().use { output ->
            input.copyTo(output)
        }
    }
}

tasks.register("load") {
    doLast {
        if (!file("$projectDir/samplerate/src").exists()) {
            val downloadPath = "${layout.buildDirectory.get()}/tmp/libsamplerate.tar.xz"
            val unpackPath = "${layout.buildDirectory.get()}/tmp"

            downloadFile(
                "https://github.com/libsndfile/libsamplerate/releases/download/$sampleRateVersion/libsamplerate-$sampleRateVersion.tar.xz",
                downloadPath
            )

            val process = ProcessBuilder("tar", "xf", downloadPath, "-C", unpackPath)
                .inheritIO()
                .start()

            val exitCode = process.waitFor()
            if (exitCode != 0) {
                throw IllegalStateException("Failed to extract libsamplerate.")
            }

            copy {
                from("$unpackPath/libsamplerate-$sampleRateVersion/src")
                into("$projectDir/samplerate/src")
            }

            copy {
                from("$unpackPath/libsamplerate-$sampleRateVersion/include")
                into("$projectDir/samplerate/include")
            }
        }

        if (!file("$projectDir/fdk-aac/libAACdec").exists()) {
            val downloadPath = "${layout.buildDirectory.get()}/tmp/fdk-aac-v$fdkAacVersion.zip"
            val unpackPath = "${layout.buildDirectory.get()}"

            downloadFile(
                "https://github.com/mstorsjo/fdk-aac/archive/v$fdkAacVersion.zip",
                downloadPath
            )

            copy {
                from(zipTree(file(downloadPath)))
                into(unpackPath)
            }

            copy {
                from("$unpackPath/fdk-aac-$fdkAacVersion")
                into("$projectDir/fdk-aac")
                exclude("CMakeLists.txt")
            }
        }

        if (!file("$projectDir/vorbis/libogg").exists()) {
            val downloadPath = "${layout.buildDirectory.get()}/tmp/temp.zip"

            downloadFile(
                "https://downloads.xiph.org/releases/ogg/libogg-$oggVersion.zip",
                downloadPath
            )

            copy {
                from(zipTree(file(downloadPath)))
                into("${layout.buildDirectory.get()}/tmp")
            }

            file("$projectDir/vorbis").mkdirs()
            file("${layout.buildDirectory.get()}/tmp/libogg-$oggVersion")
                .renameTo(file("$projectDir/vorbis/libogg"))
        }

        if (!file("$projectDir/vorbis/libvorbis").exists()) {
            val downloadPath = "${layout.buildDirectory.get()}/tmp/temp.zip"

            downloadFile(
                "https://downloads.xiph.org/releases/vorbis/libvorbis-$vorbisVersion.zip",
                downloadPath
            )

            copy {
                from(zipTree(file(downloadPath)))
                into("${layout.buildDirectory.get()}/tmp")
            }

            file("$projectDir/vorbis").mkdirs()
            file("${layout.buildDirectory.get()}/tmp/libvorbis-$vorbisVersion")
                .renameTo(file("$projectDir/vorbis/libvorbis"))
        }

        if (!file("$projectDir/opus/opus").exists()) {
            val downloadPath = "${layout.buildDirectory.get()}/tmp/temp.tar.gz"

            downloadFile(
                "https://downloads.xiph.org/releases/opus/opus-$opusVersion.tar.gz",
                downloadPath
            )

            copy {
                from(tarTree(file(downloadPath)))
                into("${layout.buildDirectory.get()}/tmp")
            }

            file("$projectDir/opus").mkdirs()
            file("${layout.buildDirectory.get()}/tmp/opus-$opusVersion")
                .renameTo(file("$projectDir/opus/opus"))
        }

        if (!Os.isFamily(Os.FAMILY_WINDOWS) && !file("$projectDir/mp3/mpg123").exists()) {
            val downloadPath = "${layout.buildDirectory.get()}/tmp/temp.tar.bz2"

            downloadFile(
                "https://www.mpg123.de/download/mpg123-$mpg123Version.tar.bz2",
                downloadPath
            )

            copy {
                from(tarTree(file(downloadPath)))
                into("${layout.buildDirectory.get()}/tmp")
            }

            file("$projectDir/mp3").mkdirs()
            file("${layout.buildDirectory.get()}/tmp/mpg123-$mpg123Version")
                .renameTo(file("$projectDir/mp3/mpg123"))
        }
    }
}
