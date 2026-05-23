plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    groovy
}

val texteditDir = rootProject.projectDir.parentFile.resolve("frontend/common-libs/textedit")

val texteditVersion = run {
    val raw = texteditDir.resolve("package.json").readText()
    Regex(""""version"\s*:\s*"([^"]+)"""").find(raw)!!.groupValues[1]
}
val texteditVersionTag = "v${texteditVersion.replace(".", "_")}"
val texteditFileName = "textedit-${texteditVersionTag}.js"

val buildTextedit = tasks.register<Exec>("buildTextedit") {
    workingDir(texteditDir)
    val shell = System.getenv("SHELL") ?: "/bin/bash"
    commandLine(shell, "-l", "-c", "npm run build:bundle")
    inputs.dir(texteditDir.resolve("src"))
    inputs.files(
        texteditDir.resolve("package.json"),
        texteditDir.resolve("vite.bundle.config.ts")
    )
    outputs.dir(texteditDir.resolve("dist-bundle"))
}

tasks.named<ProcessResources>("processResources") {
    dependsOn(buildTextedit)
    from(texteditDir.resolve("dist-bundle")) {
        include(texteditFileName)
        into("static/lib")
    }
    filesMatching("templates/fragments/head.html") {
        filter { line -> line.replace("@textedit.script@", "lib/$texteditFileName") }
    }
}

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
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
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
