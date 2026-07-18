plugins {
    kotlin("jvm") version "1.9.22"
    id("org.openjfx.javafxplugin") version "0.1.0"
    application
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.freegram"
version = "1.0.0"

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jdom:jdom2:2.0.6.1")
    implementation("org.apache.pdfbox:pdfbox:3.0.0")
    implementation("com.github.albfernandez:juniversalchardet:2.4.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

javafx {
    version = "17.0.6"
    modules("javafx.controls", "javafx.web", "javafx.swing")
}

application {
    mainClass.set("com.freegram.Main")
    applicationName = "freegram"
}

kotlin {
    jvmToolchain(17)
}

tasks.jar {
    manifest {
        attributes("Main-Class" to "com.freegram.Main")
    }
}


