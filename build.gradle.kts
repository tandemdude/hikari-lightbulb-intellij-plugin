plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "1.9.23"
  id("org.jetbrains.intellij") version "1.17.3"
  id("com.diffplug.spotless") version "6.25.0"
}

group = "io.github.tandemdude"

version = "0.0.5"

repositories { mavenCentral() }

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
intellij {
  version.set("2023.2.6")
  type.set("PC") // Target IDE Platform

  plugins.set(listOf("python-ce"))
}

spotless {
  java {
    target("src/main/java/**/*.java")
    toggleOffOn()

    importOrder()
    removeUnusedImports()
    palantirJavaFormat("2.40.0")
    formatAnnotations()
    endWithNewline()
  }
  kotlin {
    target("src/main/kotlin/**/*.kt")
    ktfmt().googleStyle()
  }
  kotlinGradle {
    target("**/*.gradle.kts")
    ktfmt().googleStyle()
  }
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "17"
    targetCompatibility = "17"
  }
  withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> { kotlinOptions.jvmTarget = "17" }

  buildSearchableOptions {
    // Remove if you add plugin settings
    enabled = false
  }

  patchPluginXml {
    sinceBuild.set("232")
    untilBuild.set("242.*")
  }

  signPlugin {
    certificateChain.set(System.getenv("CERTIFICATE_CHAIN"))
    privateKey.set(System.getenv("PRIVATE_KEY"))
    password.set(System.getenv("PRIVATE_KEY_PASSWORD"))
  }

  publishPlugin { token.set(System.getenv("PUBLISH_TOKEN")) }
}
