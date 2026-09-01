import org.jetbrains.kotlin.gradle.dsl.JvmTarget

group = "no.nav.syfo"
version = "1.0.0"


val javaVersion = JvmTarget.JVM_25

val caffeineVersion = "3.2.2"
val coroutinesVersion = "1.10.2"
val jacksonVersion = "3.2.2"
val kluentVersion = "1.73"
val ktorVersion = "3.5.2"
val logbackVersion = "1.6.3"
val logstashEncoderVersion = "9.0"
val prometheusVersion = "0.16.0"
val kotestVersion = "6.2.4"
val mockkVersion = "1.14.5"
val kotlinVersion = "2.4.10"
val ktfmtVersion = "0.56"
val diagnosekoderVersion = "1.2026.0"
val kafkaVersion = "4.3.1"
val regulaVersion = "56"
val kafkaClientVersion = "4.3.1"

plugins {
    id("application")
    kotlin("jvm") version "2.4.10"
    id("com.diffplug.spotless") version "8.10.1"
}

application {
    mainClass.set("no.nav.syfo.ApplicationKt")
    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}



repositories {
    mavenCentral()
    maven {
        url = uri("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
}


dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson3:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-apache5:$ktorVersion")
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    implementation("org.apache.kafka:kafka-clients:$kafkaClientVersion")

    implementation("no.nav.tsm.regulus:regula:$regulaVersion")

    implementation("no.nav.helse:diagnosekoder:$diagnosekoderVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("tools.jackson.module:jackson-module-kotlin:${jacksonVersion}")

    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
    }
}

kotlin {
    compilerOptions {
        jvmTarget = javaVersion
    }
}

tasks {
    
    test {
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    spotless {
        kotlin { ktfmt(ktfmtVersion).kotlinlangStyle() }
        check {
            dependsOn("spotlessApply")
        }
    }
}
