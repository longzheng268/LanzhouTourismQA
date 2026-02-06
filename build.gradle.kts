plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("org.jetbrains.compose") version "1.5.11"
}

group = "com.lanzhou.qa"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/kotlinx")
    maven("https://packages.jetbrains.io/maven")
    google()
}

dependencies {
    // Ktor - HTTP客户端
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-cio:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

    // Kotlinx Serialization - JSON序列化
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Coroutines - 协程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.7.3")

    // MariaDB JDBC驱动 - 数据库支持
    implementation("org.mariadb.jdbc:mariadb-java-client:3.4.1")

    // HikariCP - 数据库连接池（提高性能）
    implementation("com.zaxxer:HikariCP:5.1.0")

    // Logging
    implementation("io.github.oshai:kotlin-logging-jvm:6.0.9")

    // Compose Desktop - explicit dependencies for Compose 1.5.11
    implementation("org.jetbrains.compose.ui:ui:1.5.11")
    implementation("org.jetbrains.compose.ui:ui-tooling-preview:1.5.11")
    implementation("org.jetbrains.compose.material3:material3:1.5.11")
    implementation("org.jetbrains.compose.foundation:foundation:1.5.11")
    implementation("org.jetbrains.compose.ui:ui-graphics:1.5.11")
    implementation("org.jetbrains.compose.ui:ui-text:1.5.11")
    implementation("org.jetbrains.compose.ui:ui-unit:1.5.11")

    // Skiko - native rendering library (version matching Compose 1.5.11)
    implementation("org.jetbrains.skiko:skiko-awt:0.7.85.4")

    // Multi-platform Skiko runtime support
    // Windows
    if (System.getProperty("os.name").contains("Windows")) {
        runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-windows-x64:0.7.85.4")
    }
    // Linux
    else if (System.getProperty("os.name").contains("Linux")) {
        runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-linux-x64:0.7.85.4")
    }
    // macOS
    else if (System.getProperty("os.name").contains("Mac")) {
        val osArch = System.getProperty("os.arch")
        if (osArch.contains("aarch64") || osArch.contains("arm64")) {
            runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-macos-arm64:0.7.85.4")
        } else {
            runtimeOnly("org.jetbrains.skiko:skiko-awt-runtime-macos-x64:0.7.85.4")
        }
    }

    // Test
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
}

kotlin {
    jvmToolchain(20)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(20))
    }
}

tasks.test {
    useJUnitPlatform()
}

compose.desktop {
    application {
        mainClass = "com.lanzhou.qa.MainKt"
    }
}
