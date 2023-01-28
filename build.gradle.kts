plugins {
    val kotlinVersion = "1.7.10"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion

    id("net.mamoe.mirai-console") version "2.13.3"
}

group = "com.logs"
version = "0.1.0"

repositories {
    maven("https://maven.aliyun.com/repository/public")
    mavenCentral()
}
dependencies {
    implementation("com.google.code.gson:gson:2.9.1")
    implementation("org.json:json:20220924")
}