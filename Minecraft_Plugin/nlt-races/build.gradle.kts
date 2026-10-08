plugins {
    java
}

group = "com.nltarzan"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.132-stable")
    // Optional at runtime (softdepend): the leaderboard placeholders are only registered when it's installed.
    compileOnly("me.clip:placeholderapi:2.12.3")
    // Downloaded by the server at runtime through `libraries:` in plugin.yml
    compileOnly("com.zaxxer:HikariCP:6.3.0")
}

// Paper 26.x runs on (and its API is compiled for) Java 25.
tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("nlT-Races-${project.version}.jar")
}

// .\gradlew.bat deploy — builds the jar and copies it into the local test server (run/plugins).
tasks.register<Copy>("deploy") {
    group = "nlt-races"
    description = "Builds the plugin and copies it into run/plugins."
    from(tasks.jar)
    into(layout.projectDirectory.dir("run/plugins"))
}
