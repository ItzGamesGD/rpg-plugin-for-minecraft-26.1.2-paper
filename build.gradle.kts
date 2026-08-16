plugins {
    java
}

group = "com.hyunseo"
version = "0.1.0-SNAPSHOT"

java {
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.72-stable")
    implementation("org.xerial:sqlite-jdbc:3.46.1.0")
    testImplementation("io.papermc.paper:paper-api:26.1.2.build.72-stable")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.jar {
    archiveBaseName.set("HyunseoRPG")
    from(configurations.runtimeClasspath.get().map { file -> if (file.isDirectory) file else zipTree(file) })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

tasks.test {
    useJUnitPlatform()
}
