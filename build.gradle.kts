import com.palantir.gradle.gitversion.VersionDetails
import groovy.lang.Closure
import java.net.URI

plugins {
  java
  `maven-publish`
  id("com.palantir.git-version") version "3.0.0"
}

group = "dev.arbjerg"
val gitVersion: Closure<String> by extra
val versionDetails: Closure<VersionDetails> by extra
val details = versionDetails()
version = gitVersion()
println("Version: $version")

allprojects {
  group = rootProject.group
  version = rootProject.version

  repositories {
    mavenLocal()
    mavenCentral()
    maven(url = "https://m2.dv8tion.net/releases")
    maven(url = "https://jitpack.io")
  }

  apply(plugin = "java")
  apply(plugin = "maven-publish")
  apply(plugin = "com.palantir.git-version")

  java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  publishing {
    repositories {
      val snapshots = "https://maven.arbjerg.dev/snapshots"
      val releases = "https://maven.arbjerg.dev/releases"

      maven(if (details.isCleanTag) releases else snapshots) {
        credentials {
          password = findProperty("MAVEN_PASSWORD") as String?
          username = findProperty("MAVEN_USERNAME") as String?
        }
      }
    }
  }
}
