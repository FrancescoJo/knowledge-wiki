import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.3.0" apply false
    id("io.spring.dependency-management") version "1.1.5" apply false
    kotlin("jvm") version "1.9.24" apply false
    kotlin("plugin.spring") version "1.9.24" apply false
}

allprojects {
    group = "com.fj.omnimemo"
    version = "0.1.1"

    repositories {
        mavenCentral()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(25))
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            freeCompilerArgs = listOf("-Xjsr305=strict")
            jvmTarget = "21"
        }
    }

    tasks.withType<JavaCompile> {
        options.release.set(21)
    }

    tasks.withType<Test> {
        useJUnitPlatform()
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
