plugins {
  `java-library`
  `maven-publish`
}

val moduleName = "lavaplayer-ext-format-xm"
version = "0.1.0"

dependencies {
  compileOnly(project(":main"))
  implementation(files("./lib/ibxm-a74.jar"))
}

val sourcesJar by tasks.registering(Jar::class) {
  archiveClassifier.set("sources")
  from(sourceSets["main"].allSource)
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      from(components["java"])
      artifactId = moduleName
      artifact(sourcesJar)
    }
  }
}
