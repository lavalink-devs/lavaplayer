import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `java-library`
    alias(libs.plugins.maven.publish.base)
}

base {
    archivesName = "lava-common"
}

dependencies {
    implementation("org.slf4j:slf4j-api:1.7.25")
    implementation("commons-io:commons-io:2.6")
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc()))
}
