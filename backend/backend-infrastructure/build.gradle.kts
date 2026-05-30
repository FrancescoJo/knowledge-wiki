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
    implementation("org.springframework.security:spring-security-crypto")
    implementation("com.nimbusds:nimbus-jose-jwt:9.37.3")
    implementation("io.github.java-diff-utils:java-diff-utils:4.15")
    runtimeOnly("org.postgresql:postgresql")

    testFixturesApi("org.testcontainers:testcontainers")
    testFixturesApi("org.testcontainers:postgresql")
    testFixturesImplementation(project(":backend-core"))
    testFixturesImplementation("org.springframework:spring-jdbc")

    testImplementation(testFixtures(project(":backend-core")))

    testImplementation("org.liquibase:liquibase-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
    testImplementation("org.testcontainers:junit-jupiter")
}
