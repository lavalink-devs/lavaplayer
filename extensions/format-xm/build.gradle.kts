plugins {
  `java-library`
  `maven-publish`
}
val moduleName = "lavaplayer-ext-format-xm"

dependencies {
  compileOnly(project(":main"))
  implementation("com.github.walkyst:ibxm-fork:a75")
  implementation("org.slf4j:slf4j-api:1.7.25")
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