plugins {
    java
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

group = "com.nltarzan"
version = "0.1.0"

// The Minecraft version we compile against. The plugin.yml api-version is derived from it,
// so the plugin loads on this version and every newer one.
val minecraftVersion = "26.3"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.3.build.209-stable")
    // Optional at runtime (softdepend): the leaderboard placeholders are only registered when it's installed.
    compileOnly("me.clip:placeholderapi:2.12.3")
    // Downloaded by the server at runtime through `libraries:` in plugin.yml
    compileOnly("com.zaxxer:HikariCP:6.3.0")
    // The tests read YAML and parse MiniMessage, so they need the Paper API too (never packed in the plugin).
    testImplementation("io.papermc.paper:paper-api:26.3.build.209-stable")

    testImplementation(platform("org.junit:junit-bom:6.1.3"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

// Paper 26.x runs on (and its API is compiled for) Java 25.
tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 25
}

tasks.test {
    useJUnitPlatform()
}

tasks.processResources {
    val props = mapOf("version" to project.version, "apiVersion" to minecraftVersion)
    inputs.properties(props)
    filesMatching("plugin.yml") {
        expand(props)
    }
}

tasks.jar {
    archiveFileName.set("nlT-Races-${project.version}.jar")
}

// ./gradlew runServer -> builds the plugin and starts the local test server in run/ with it.
// run-paper adds the plugin jar and downloads LuckPerms (for nltraces.user / nltraces.admin) and
// PlaceholderAPI (for the leaderboard placeholders), so don't copy any of them into run/plugins.
tasks.runServer {
    minecraftVersion(minecraftVersion)
    runDirectory = layout.projectDirectory.dir("run")
    downloadPlugins {
        modrinth("luckperms", "v5.5.71-bukkit")
        hangar("PlaceholderAPI", "2.12.3")
    }
}
