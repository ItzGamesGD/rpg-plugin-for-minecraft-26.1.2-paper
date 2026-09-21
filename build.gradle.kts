import java.util.zip.ZipFile

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
    testImplementation("com.google.code.gson:gson:2.13.2")
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

val verifyBundledDatapack = tasks.register("verifyBundledDatapack") {
    group = "verification"
    description = "Verifies that the single plugin JAR contains its complete discoverable datapack."
    dependsOn(tasks.jar)
    doLast {
        val jarFile = tasks.jar.get().archiveFile.get().asFile
        val required = listOf(
            "datapack/pack.mcmeta",
            "datapack/data/minecraft/tags/enchantment/in_enchanting_table.json",
            "datapack/data/minecraft/tags/enchantment/tradeable.json",
            "datapack/data/minecraft/tags/enchantment/on_random_loot.json",
            "datapack/data/minecraft/tags/enchantment/treasure.json",
            "datapack/data/minecraft/tags/enchantment/non_treasure.json",
            "datapack/data/hyunseorpg/tags/enchantment/exclusive_set/bow_shift_left.json",
            "datapack/data/hyunseorpg/tags/item/swords.json",
            "datapack/data/hyunseorpg/tags/item/axes.json",
            "datapack/data/hyunseorpg/tags/item/pickaxes.json",
            "datapack/data/hyunseorpg/tags/item/hoes.json",
            "datapack/data/hyunseorpg/tags/item/excavation_tools.json",
            "datapack/data/hyunseorpg/tags/item/bows.json",
            "datapack/data/hyunseorpg/tags/item/crossbows.json",
            "datapack/data/hyunseorpg/tags/item/fishing_rods.json",
            "datapack/data/hyunseorpg/tags/item/elytra.json",
            "datapack/data/hyunseorpg/tags/item/maces.json"
        )
        ZipFile(jarFile).use { archive ->
            val present = archive.entries().asSequence().map { it.name }.toSet()
            val missing = required.filterNot(present::contains)
            check(missing.isEmpty()) { "Bundled datapack entries missing from ${jarFile.name}: $missing" }
        }
    }
}

tasks.check { dependsOn(verifyBundledDatapack) }
