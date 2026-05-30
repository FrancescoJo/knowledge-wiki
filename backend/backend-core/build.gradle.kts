plugins {
    kotlin("jvm")
    `java-test-fixtures`
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.github.f4b6a3:uuid-creator:5.3.3")

    // Testing
    testFixturesImplementation("net.datafaker:datafaker:2.4.3")
    testFixturesImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("io.kotest:kotest-assertions-core:5.9.1")
}
