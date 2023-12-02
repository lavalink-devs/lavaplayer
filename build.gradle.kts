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
logger.lifecycle("Version: $gitVersion (release: $release)")

allprojects {
    group = "dev.arbjerg"
    version = gitVersion

    repositories {
        mavenLocal()
        mavenCentral()
        maven("https://jitpack.io")
    }
}

subprojects {
    if (project.name == "natives" || project.name == "extensions-project") {
        return@subprojects
    }

    apply<JavaPlugin>()
    apply<MavenPublishPlugin>()

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_1_8
    }

    configure<PublishingExtension> {
        if (findProperty("MAVEN_PASSWORD") != null && findProperty("MAVEN_USERNAME") != null) {
            repositories {
                val snapshots = "https://maven.lavalink.dev/snapshots"
                val releases = "https://maven.lavalink.dev/releases"

                maven(if (release) releases else snapshots) {
                    credentials {
                        password = findProperty("MAVEN_PASSWORD") as String?
                        username = findProperty("MAVEN_USERNAME") as String?
                    }
                }
            }
        } else {
            logger.lifecycle("Not publishing to maven.lavalink.dev because credentials are not set")
        }
    }

    afterEvaluate {
        plugins.withId(libs.plugins.maven.publish.base.get().pluginId) {
            configure<MavenPublishBaseExtension> {
                coordinates(group.toString(), project.the<BasePluginExtension>().archivesName.get(), version.toString())

                if (findProperty("mavenCentralUsername") != null && findProperty("mavenCentralPassword") != null) {
                    publishToMavenCentral(SonatypeHost.S01, false)
                    if (release) {
                        signAllPublications()
                    }
                } else {
                    logger.lifecycle("Not publishing to OSSRH due to missing credentials")
                }

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
            logger.lifecycle("Git state is dirty, version is a snapshot.")
        }

        return if (headTag != null && clean) headTag.name to true else "${git.head().id}-SNAPSHOT" to false
    }
}
