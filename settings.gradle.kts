rootProject.name = "lavaplayer"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
    ":common",
    ":main",
    ":extensions",
    ":extensions:youtube-rotator",
    ":extensions:format-xm",
    ":natives",
    ":natives-publish"
)

// https://github.com/gradle/gradle/issues/19254
project(":extensions").name = "extensions-project"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            plugins()
            common()
            others()
            test()
        }
    }
}

fun VersionCatalogBuilder.plugins() {
    val mavenPublishPlugin = version("maven-publish-plugin", "0.25.3")

    plugin("maven-publish", "com.vanniktech.maven.publish").versionRef(mavenPublishPlugin)
    plugin("maven-publish-base", "com.vanniktech.maven.publish.base").versionRef(mavenPublishPlugin)
}

fun VersionCatalogBuilder.common() {
    library("slf4j", "org.slf4j", "slf4j-api").version("2.0.7")
    library("commons-io", "commons-io", "commons-io").version("2.13.0")

    version("jackson", "2.15.2")
    library("jackson-core", "com.fasterxml.jackson.core", "jackson-core").versionRef("jackson")
    library("jackson-databind", "com.fasterxml.jackson.core", "jackson-databind").versionRef("jackson")

    library("httpclient", "org.apache.httpcomponents", "httpclient").version("4.5.14")

    library("jsoup", "org.jsoup", "jsoup").version("1.16.1")
    library("base64", "net.iharder", "base64").version("2.3.9")
    library("json", "org.json", "json").version("20230618")
}

fun VersionCatalogBuilder.others() {
    library("ibxm-fork", "com.github.walkyst", "ibxm-fork").version("a75")
    library("jaadec-fork", "com.github.walkyst", "JAADec-fork").version("0.1.3")
    library("rhino-engine", "org.mozilla", "rhino-engine").version("1.7.14")
}

fun VersionCatalogBuilder.test() {
    library("groovy", "org.apache.groovy", "groovy").version("4.0.13")
    library("spock-core", "org.spockframework", "spock-core").version("2.4-M1-groovy-4.0")
    library("logback-classic", "ch.qos.logback", "logback-classic").version("1.4.8")
}
