import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "1.6.10"
    id("com.github.johnrengelman.shadow") version "7.0.0"
    application
}

version = "1.0.0"

repositories {
    mavenLocal()
    mavenCentral()
}

dependencies {
    kotlin("stable")
    implementation("commons-io:commons-io:2.11.0")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("org.apache.httpcomponents:httpcore:4.4.15")
    implementation("org.apache.httpcomponents:httpclient:4.5.13")

    implementation("com.google.code.gson:gson:2.9.0")

    implementation("net.sf.jopt-simple:jopt-simple:5.0.4")


}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

application {
    mainClass.set("ltd.lths.wireless.ghinf.ap.Main")
}

tasks.withType<ShadowJar> {
    classifier = null
}