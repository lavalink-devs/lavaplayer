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
    implementation(libs.slf4j)
    implementation(libs.commons.io)
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc()))
}
