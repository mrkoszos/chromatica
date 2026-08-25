plugins {
    id("java-library")
    id("com.gradleup.shadow") version "9.6.1"
    id("xyz.jpenilla.run-paper") version "3.1.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://mvn.wesjd.net/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.2.build.+")
    implementation("net.wesjd:anvilgui:1.10.13-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(25)
}

tasks {

    shadowJar {
        relocate(
            "net.wesjd.anvilgui",
            "com.mrkoszos.chromify.libs.anvilgui"
        )
    }

    build {
        dependsOn(shadowJar)
    }

    runServer {
        minecraftVersion("26.2")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf(
            "version" to version,
            "description" to project.description
        )

        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}
