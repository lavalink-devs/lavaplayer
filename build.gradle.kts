@file:Suppress("UnstableApiUsage")

import com.vanniktech.maven.publish.MavenPublishBaseExtension
import com.vanniktech.maven.publish.SonatypeHost
import org.ajoberstar.grgit.Grgit

plugins {
  id("org.ajoberstar.grgit") version "5.2.0"
  id("de.undercouch.download") version "5.4.0"
  alias(libs.plugins.maven.publish.base) apply false
}

val (gitVersion, release) = versionFromGit()
println("Version: $gitVersion (release: $release)")

allprojects {
  group = "dev.arbjerg"
  version = gitVersion

  repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
  }
}

subprojects {
  apply<JavaPlugin>()
  apply<MavenPublishPlugin>()

  configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
  }

  configure<PublishingExtension> {
    repositories {
      val snapshots = "https://maven.arbjerg.dev/snapshots"
      val releases = "https://maven.arbjerg.dev/releases"

      maven(if (release) releases else snapshots) {
        credentials {
          password = findProperty("MAVEN_PASSWORD") as String?
          username = findProperty("MAVEN_USERNAME") as String?
        }
      }
    }
  }

  afterEvaluate {
    plugins.withId(libs.plugins.maven.publish.base.get().pluginId) {
        configure<MavenPublishBaseExtension> {
          coordinates(group.toString(), project.the<BasePluginExtension>().archivesName.get(), version.toString())

          publishToMavenCentral(SonatypeHost.S01, true)
          //signAllPublications()

          pom {
            name = "lavaplayer"
            description = "A Lavaplayer fork maintained by Lavalink"
            url = "https://github.com/lavalink-devs/lavaplayer"

            licenses {
              license {
                name = "The Apache License, Version 2.0"
                url = "https://github.com/lavalink-devs/lavaplayer/blob/main/LICENSE"
              }
            }

            developers {
              developer {
                id = "freyacodes"
                name = "Freya Arbjerg"
                url = "https://www.arbjerg.dev"
              }
            }

            scm {
              url = "https://github.com/lavalink-devs/lavaplayer/"
              connection = "scm:git:git://github.com/lavalink-devs/lavaplayer.git"
              developerConnection = "scm:git:ssh://git@github.com/lavalink-devs/lavaplayer.git"
            }
          }
        }
      }
  }
}

@SuppressWarnings("GrMethodMayBeStatic")
fun versionFromGit(): Pair<String, Boolean> {
  Grgit.open(mapOf("currentDir" to project.rootDir)).use { git ->
    val headTag = git.tag
      .list()
      .find { it.commit.id == git.head().id }

    val clean = git.status().isClean || System.getenv("CI") != null
    if (!clean) {
      println("Git state is dirty, version is a snapshot.")
    }

    return if (headTag != null && clean) Pair(headTag.name, true) else Pair(git.head().abbreviatedId, false)
  }
}
