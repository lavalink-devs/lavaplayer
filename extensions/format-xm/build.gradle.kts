import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar

plugins {
    `java-library`
    alias(libs.plugins.maven.publish.base)
}
base {
    archivesName = "lavaplayer-ext-format-xm"
}

dependencies {
    compileOnly(projects.main)
    implementation("com.github.walkyst:ibxm-fork:a75")
    implementation("org.slf4j:slf4j-api:1.7.25")
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc()))
}
