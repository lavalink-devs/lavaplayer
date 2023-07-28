plugins {
  `java-library`
  `maven-publish`
}

val moduleName = "lavaplayer-ext-youtube-rotator"

dependencies {
  compileOnly(project(":main"))
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
