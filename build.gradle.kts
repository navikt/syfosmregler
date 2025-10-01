import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.ByteArrayOutputStream

group = "no.nav.syfo"
version = "1.0.0"


val javaVersion = JvmTarget.JVM_21

val caffeineVersion = "3.2.2"
val coroutinesVersion = "1.10.2"
val jacksonVersion = "2.20.0"
val kluentVersion = "1.73"
val ktorVersion = "3.3.0"
val logbackVersion = "1.5.19"
val logstashEncoderVersion = "8.1"
val prometheusVersion = "0.16.0"
val kotestVersion = "6.0.3"
val mockkVersion = "1.14.6"
val kotlinVersion = "2.2.20"
val ktfmtVersion = "0.44"
val diagnosekoderVersion = "1.2025.0"
val kafkaVersion = "3.9.1"

///Due to vulnerabilities
val nettyCommonVersion = "4.2.6.Final"
val snappyJavaVersion = "1.1.10.8"
val commonsCodecVersion = "1.19.0"

plugins {
    id("application")
    kotlin("jvm") version "2.2.20"
    id("com.diffplug.spotless") version "8.0.0"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    constraints {
        implementation("io.netty:netty-common:$nettyCommonVersion") {
            because("override transient from io.ktor:ktor-server-netty")
        }
    }
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")
    constraints {
        implementation("commons-codec:commons-codec:$commonsCodecVersion") {
            because("override transient version 1.13 from io.ktor:ktor-client-apache")
        }
    }
    implementation("io.ktor:ktor-server-swagger:$ktorVersion")

    implementation("no.nav.tsm.regulus:regula:43")

    implementation("no.nav.helse:diagnosekoder:$diagnosekoderVersion")
    implementation("org.apache.kafka:kafka_2.12:$kafkaVersion")
    constraints {
        implementation("org.xerial.snappy:snappy-java:$snappyJavaVersion") {
            because("override transient from org.apache.kafka:kafka_2.12")
        }
    }

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

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
