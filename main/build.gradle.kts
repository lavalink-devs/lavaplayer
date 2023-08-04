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
    implementation("com.github.walkyst.JAADec-fork:jaadec-ext-aac:0.1.3")
    implementation("org.mozilla:rhino-engine:1.7.14")
    api("org.slf4j:slf4j-api:1.7.25")

    api("org.apache.httpcomponents:httpclient:4.5.10")
    implementation("commons-io:commons-io:2.6")

    api("com.fasterxml.jackson.core:jackson-core:2.10.0")
    api("com.fasterxml.jackson.core:jackson-databind:2.10.0")

    implementation("org.jsoup:jsoup:1.12.1")
    implementation("net.iharder:base64:2.3.9")
    implementation("org.json:json:20220924")

    testImplementation("org.codehaus.groovy:groovy:2.5.5")
    testImplementation("org.spockframework:spock-core:1.2-groovy-2.5")
    testImplementation("ch.qos.logback:logback-classic:1.2.3")
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
