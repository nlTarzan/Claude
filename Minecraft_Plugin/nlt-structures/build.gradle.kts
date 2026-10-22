import xyz.jpenilla.runpaper.task.RunServer

plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.nltarzan"
version = "0.1.0"

// The Minecraft version we compile against. The plugin.yml api-version is derived from it,
// so the plugin loads on this version and every newer one (26.3 included).
val minecraftVersion = "26.2"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.132-stable")
    // The tests read YAML and use text colors, so they need the Paper API too (never packed in the plugin).
    testImplementation("io.papermc.paper:paper-api:26.2.build.132-stable")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.test {
    useJUnitPlatform()
}

tasks.jar {
    archiveFileName.set("nlT-Structures-${project.version}.jar")
}

tasks.processResources {
    val props = mapOf("version" to project.version, "apiVersion" to minecraftVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

// ./gradlew runServer      -> local 26.2 test server in run/26.2
// ./gradlew runServer26_3  -> local 26.3 test server in run/26.3
// Both test servers get LuckPerms, to hand out the nltstructures.admin permission.
tasks.withType<RunServer>().configureEach {
    downloadPlugins {
        modrinth("luckperms", "v5.5.71-bukkit")
    }
}

tasks.runServer {
    minecraftVersion(minecraftVersion)
    runDirectory = layout.projectDirectory.dir("run/$minecraftVersion")
}

tasks.register<RunServer>("runServer26_3") {
    group = "run paper"
    description = "Run a 26.3 test server with the plugin."
    minecraftVersion("26.3")
    runDirectory = layout.projectDirectory.dir("run/26.3")
    pluginJars(tasks.jar.flatMap { it.archiveFile })
}
