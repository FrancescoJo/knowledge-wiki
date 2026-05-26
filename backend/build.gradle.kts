import io.gitlab.arturbosch.detekt.Detekt
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    // Pinned to 2.0.21 to match detekt 1.23.8's embedded Kotlin version.
    // Detekt reads KotlinVersion.CURRENT from the KGP, not from kotlinCompilerClasspath,
    // so the KGP version must align. Upgrade both together when detekt supports a newer version.
    kotlin("jvm") version "2.0.21" apply false
    kotlin("plugin.spring") version "2.0.21" apply false
    id("io.gitlab.arturbosch.detekt") version "1.23.8" apply false
}

allprojects {
    group = "com.fj.omnimemo"
    version = "0.1.1"

    // Ensures Spring Boot BOM resolves Kotlin artefacts at 2.0.21 rather than its
    // default managed version, keeping all Kotlin artefacts on a single patch version.
    extra["kotlin.version"] = "2.0.21"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<Detekt>().configureEach {
        // Project toolchain targets JDK 25; detekt only supports up to JVM 21.
        jvmTarget = "21"
        config.setFrom("${rootProject.projectDir}/config/detekt/detekt.yml")
        buildUponDefaultConfig = true
        reports {
            html.required.set(false)
            xml.required.set(false)
            sarif.required.set(false)
        }
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            freeCompilerArgs.add("-Xjsr305=strict")
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }

    tasks.named("check") {
        dependsOn(tasks.withType<Detekt>())
    }

    tasks.withType<JavaCompile> {
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        // Byte Buddy (bundled with mockito-core) officially supports up to Java 23.
        // Experimental mode extends support to newer JVM versions such as Java 25.
        jvmArgs("-Dnet.bytebuddy.experimental=true")
    }

    val testSources = extensions.getByType<SourceSetContainer>()["test"]
    listOf("small", "medium", "large").forEach { tag ->
        tasks.register<Test>("test${tag.replaceFirstChar { it.uppercase() }}") {
            description = "Runs $tag tests."
            group = "verification"
            testClassesDirs = testSources.output.classesDirs
            classpath = testSources.runtimeClasspath
            useJUnitPlatform { includeTags(tag) }
        }
    }
}

tasks.register("test-backend-small") {
    group = "verification"
    description = "Runs all backend small tests."
    dependsOn(subprojects.map { ":${it.name}:testSmall" })
}

tasks.register("test-backend-medium") {
    group = "verification"
    description = "Runs all backend medium tests."
    dependsOn(subprojects.map { ":${it.name}:testMedium" })
}

tasks.register("test-backend-large") {
    group = "verification"
    description = "Runs all backend large tests."
    dependsOn(subprojects.map { ":${it.name}:testLarge" })
}

tasks.register("test-backend-all") {
    group = "verification"
    description = "Runs all backend tests."
    dependsOn(subprojects.map { ":${it.name}:test" })
}

tasks.register("test-all") {
    group = "verification"
    description = "Runs all tests."
    dependsOn("test-backend-all")
}

tasks.register("test") {
    group = "verification"
    description = "Alias for test-all."
    dependsOn("test-all")
}
