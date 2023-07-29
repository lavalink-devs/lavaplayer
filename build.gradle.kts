import org.ajoberstar.grgit.Grgit

plugins {
  java
  `maven-publish`
  id("org.ajoberstar.grgit") version "5.2.0"
  id("de.undercouch.download") version "5.4.0"
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
  apply(plugin = "java")
  apply(plugin = "maven-publish")

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  publishing {
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