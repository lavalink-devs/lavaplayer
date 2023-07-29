rootProject.name = "lavaplayer-root"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(
        "common",
        "main",
        "testbot",
        "test-samples",
        ":extensions",
        ":extensions:youtube-rotator",
        ":extensions:format-xm",
        "natives",
        "natives-publish"
)

// https://github.com/gradle/gradle/issues/19254
project(":extensions").name = "extensions-project"

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            plugins()
        }
    }
}

fun VersionCatalogBuilder.plugins() {
    val mavenPublishPlugin = version("maven-publish-plugin", "0.25.3")

    plugin("maven-publish", "com.vanniktech.maven.publish").versionRef(mavenPublishPlugin)
    plugin("maven-publish-base", "com.vanniktech.maven.publish.base").versionRef(mavenPublishPlugin)
}
