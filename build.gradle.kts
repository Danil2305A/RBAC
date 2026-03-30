plugins {
    id("java")
}

group = "com.example"

tasks.jar {
    manifest {
        attributes["Main-Class"] = "com.example.Main"
        archiveBaseName = "mcs"
    }
}

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}