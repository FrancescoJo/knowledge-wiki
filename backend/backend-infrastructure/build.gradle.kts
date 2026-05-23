plugins {
    id("io.spring.dependency-management")
    kotlin("jvm")
    kotlin("plugin.spring")
    `java-test-fixtures`
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.boot:spring-boot-dependencies:3.3.0")
    }
}

dependencies {
    implementation(project(":backend-core"))
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    testFixturesApi("org.testcontainers:testcontainers")
    testFixturesApi("org.testcontainers:postgresql")

    testImplementation(testFixtures(project(":backend-core")))

    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.testcontainers:junit-jupiter")
}
