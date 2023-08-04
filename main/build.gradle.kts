import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import kotlin.io.path.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeText

plugins {
    `java-library`
    alias(libs.plugins.maven.publish.base)
}

base {
    archivesName = "lavaplayer"
}

dependencies {
    api(projects.common)
    implementation(projects.nativesPublish)
    implementation(libs.jaadec.fork)
    implementation(libs.rhino.engine)
    implementation(libs.slf4j)

    api(libs.httpclient)
    implementation(libs.commons.io)

    api(libs.jackson.core)
    api(libs.jackson.databind)

    implementation(libs.jsoup)
    implementation(libs.base64)
    implementation(libs.json)

    testImplementation(libs.groovy)
    testImplementation(libs.spock.core)
    testImplementation(libs.logback.classic)
}

tasks {
    val updateVersion by registering {
        val output = "$buildDir/resources/main/com/sedmelluq/discord/lavaplayer/tools/version.txt"
        inputs.property("version", version)
        outputs.file(output)

        doLast {
            Path(output).let {
                it.parent.createDirectories()
                it.writeText(version.toString())
            }
        }
    }

    classes {
        dependsOn(updateVersion)
    }
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc()))
}
