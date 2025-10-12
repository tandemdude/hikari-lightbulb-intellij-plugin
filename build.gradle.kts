plugins {
  id("java")
  id("org.jetbrains.kotlin.jvm") version "2.1.0"
  id("org.jetbrains.intellij.platform") version "2.9.0"
  id("com.diffplug.spotless") version "8.0.0"
}

group = "io.github.tandemdude"

version = "0.0.9"

repositories {
  mavenCentral()
  intellijPlatform { defaultRepositories() }
}

// Configure Gradle IntelliJ Plugin
// Read more: https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html
dependencies {
  intellijPlatform {
    create("PC", "2025.2.3")
    testFramework(org.jetbrains.intellij.platform.gradle.TestFrameworkType.Platform)

    // Add necessary plugin dependencies for compilation here, example:
    bundledPlugin("com.intellij.modules.python")
  }
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

intellijPlatform {
  pluginConfiguration {
    ideaVersion { sinceBuild = "251" }

    changeNotes =
      """
          Initial version
      """
        .trimIndent()
  }

  signing {
    certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
    privateKey = providers.environmentVariable("PRIVATE_KEY")
    password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
  }

  publishing { token = providers.environmentVariable("PUBLISH_TOKEN") }

  buildSearchableOptions = false
}

tasks {
  // Set the JVM compatibility versions
  withType<JavaCompile> {
    sourceCompatibility = "21"
    targetCompatibility = "21"
  }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21) } }
