plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    groovy
}

apply(from = "build-frontend.gradle.kts")

val secretConfigFile = projectDir.resolve("application-secret.yml")

tasks.register("checkSecretConfig") {
    description = "Fails the build if application-secret.yml is missing"
    doFirst {
        if (!secretConfigFile.exists()) {
            throw GradleException(
                "\napplication-secret.yml not found: ${secretConfigFile.absolutePath}" +
                "\nCopy application-secret.yml.template and fill in the values."
            )
        }
    }
}

tasks.named("bootJar") { dependsOn("checkSecretConfig") }
tasks.named("bootRun") { dependsOn("checkSecretConfig") }

dependencies {
    implementation(project(":backend-core"))
    implementation(project(":backend-infrastructure"))

    // API documentation
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.5.0")

    // Spring Boot
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.security:spring-security-oauth2-jose")
    implementation("org.liquibase:liquibase-core")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    runtimeOnly("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation(testFixtures(project(":backend-core")))
    testImplementation(testFixtures(project(":backend-infrastructure")))

    // Testing - Spock
    testImplementation("org.apache.groovy:groovy")
    testImplementation("org.spockframework:spock-core:2.4-M6-groovy-4.0")
    testImplementation("org.spockframework:spock-spring:2.4-M6-groovy-4.0")
}

// Groovy 4.0 does not support the JDK 25 runtime; pin Groovy compilation to JDK 21,
// which matches the project's bytecode target.
tasks.withType<GroovyCompile>().configureEach {
    javaLauncher.set(javaToolchains.launcherFor {
        languageVersion.set(JavaLanguageVersion.of(21))
    })
}

// Groovy test compilation must see Kotlin test classes (e.g. API client helpers).
tasks.named<GroovyCompile>("compileTestGroovy") {
    dependsOn("compileTestKotlin")
    classpath += files(layout.buildDirectory.dir("classes/kotlin/test"))
}
