plugins {
    kotlin("jvm") version "1.6.10"
    id("io.izzel.taboolib") version "1.34"
}

group = "ltd.lths.wireless.ghinf.ap"
version = "1.0.0"

taboolib {
    install(
        "common",
        "common-5",
        "module-configuration-shaded",
        "platform-application",
    )
    classifier = null
    version = "6.0.7-56"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.tabooproject.org/repository/releases")
}

dependencies {
    taboo(kotlin("stdlib"))

    taboo("commons-io:commons-io:2.11.0")
    taboo("org.apache.commons:commons-lang3:3.12.0")
    taboo("org.apache.httpcomponents:httpcore:4.4.15")
    taboo("org.apache.httpcomponents:httpclient:4.5.13")

    taboo("com.google.code.gson:gson:2.9.0")

    taboo("net.sf.jopt-simple:jopt-simple:5.0.4")
    taboo("org.jsoup:jsoup:1.14.3")
    compileOnly("com.electronwill.night-config:core:3.6.5")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<Jar> {
    manifest {
        attributes["Main-Class"] = "ltd.lths.wireless.ghinf.ap.Main"
    }
}