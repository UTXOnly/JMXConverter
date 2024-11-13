plugins {
    java
    application
    id("com.github.johnrengelman.shadow") version "8.1.1" // Add the Shadow plugin
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.eclipse.jetty:jetty-server:11.0.15")
    implementation("org.eclipse.jetty:jetty-servlet:11.0.15")
}

application {
    mainClass.set("org.example.SimpleJettyApp")
}
