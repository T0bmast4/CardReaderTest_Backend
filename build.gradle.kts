plugins {
    kotlin("jvm") version "2.1.0"
}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val vertxVersion = "4.5.12"

dependencies {
    implementation(platform("io.vertx:vertx-stack-depchain:$vertxVersion"))
    implementation("io.vertx:vertx-lang-kotlin")
    implementation("io.vertx:vertx-web-client")
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.vertx:vertx-lang-kotlin-coroutines")
    implementation("io.vertx:vertx-web")
    implementation("io.vertx:vertx-mysql-client:4.4.0")
    testImplementation(kotlin("test"))
    implementation("com.google.auth:google-auth-library-oauth2-http:1.13.0")
}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(22)
}