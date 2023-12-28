plugins {
    java
    application
}

dependencies {
    implementation(projects.main)
    implementation(libs.base64)
    implementation(libs.slf4j)
    runtimeOnly(libs.logback.classic)
}

application {
    mainClass.set("com.sedmelluq.discord.lavaplayer.demo.LocalPlayerDemo")
}
