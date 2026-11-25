plugins {
    java
    id("net.fabricmc.fabric-loom-remap") version("1.17.+")
    id("ploceus") version("1.17.+")
    id("me.modmuss50.mod-publish-plugin") version "2.2.0"
}

group = "org.polyfrost"
version = providers.gradleProperty("mod_version").get()

java.toolchain {
    languageVersion = JavaLanguageVersion.of(25)
}

@Suppress("MayBeConstant")
object Versions {
    val minecraft = "1.8.9"
    val feather = "2"
    val fabric = "0.19.5"
    val netty = "4.2.18.Final"
    val argentum = "1.0.2+3609b11"
}

loom {
    accessWidenerPath = file("src/main/resources/polydium.accesswidener")
    runs.named("client") {
        jvmArguments.add("-Dmixin.debug.export=true")
    }
}

ploceus {
    setIntermediaryGeneration(2)
}

repositories {
    mavenCentral()
    maven("https://maven.taumc.org/releases")
}

dependencies {
    minecraft("com.mojang:minecraft:${Versions.minecraft}")
    mappings(ploceus.featherMappings(Versions.feather))

    modImplementation("net.fabricmc:fabric-loader:${Versions.fabric}")
    modCompileOnly("dev.rdh:argentum:${Versions.argentum}") { isTransitive = false }
    compileOnly("org.embeddedt.celeritas:celeritas-common:2.6.0-dev.2") { isTransitive = false }
    compileOnly("org.joml:joml:1.10.8")

    listOf("resolver", "resolver-dns", "codec-dns").forEach {
        compileOnly("io.netty:netty-$it:${Versions.netty}") { isTransitive = false }
    }
}

tasks.assemble {
    dependsOn("remapJar")
}

tasks.processResources {
    val v = project.version
    inputs.property("version", v)

    filesMatching("fabric.mod.json") {
        expand("version" to v)
    }
}

publishMods {
    val modrinthToken = providers.gradleProperty("publish.modrinth.token")
        .orElse(providers.gradleProperty("modrinth.token"))

    file = tasks.remapJar.flatMap { it.archiveFile }
    displayName = project.version.toString()
    version = "v${project.version}"
    changelog = rootProject.file("CHANGELOG.md").takeIf { it.exists() }?.readText() ?: "No changelog provided."
    type = if ("beta" in project.version.toString()) BETA else STABLE
    modLoaders.add("ornithe")
    dryRun = !modrinthToken.isPresent

    modrinth {
        projectId = "hbSfT032"
        accessToken = modrinthToken
        minecraftVersions.add(Versions.minecraft)
        environment = CLIENT_ONLY
        requires("argentum")
    }
}
