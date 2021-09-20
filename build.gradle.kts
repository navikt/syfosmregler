import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "no.nav.syfo"
version = "1.0.0"

val coroutinesVersion = "1.4.2"
val jacksonVersion = "2.12.3"
val kluentVersion = "1.65"
val ktorVersion = "1.5.1"
val logbackVersion = "1.2.3"
val logstashEncoderVersion = "6.5"
val prometheusVersion = "0.9.0"
val smCommonVersion = "1.dff6489"
val spekVersion = "2.0.17"
val jfairyVersion = "0.6.2"
val mockkVersion = "1.9.3"


plugins {
    java
    kotlin("jvm") version "1.4.21"
    id("org.jmailen.kotlinter") version "3.3.0"
    id("com.diffplug.spotless") version "5.8.2"
    id("com.github.johnrengelman.shadow") version "6.1.0"
}

val githubUser: String by project
val githubPassword: String by project

repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/navikt/syfosm-common")
        credentials {
            username = githubUser
            password = githubPassword
        }
    }
}


dependencies {
    implementation(kotlin("stdlib"))

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation ("io.ktor:ktor-server-netty:$ktorVersion")
    implementation ("io.ktor:ktor-jackson:$ktorVersion")

    implementation("io.ktor:ktor-client-auth-basic:$ktorVersion")
    implementation("io.ktor:ktor-client-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("no.nav.helse:syfosm-common-models:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-networking:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-rest-sts:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-rules:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-diagnosis-codes:$smCommonVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation ("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation ("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion")
    testImplementation("com.devskiller:jfairy:$jfairyVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty") // conflicts with WireMock
    }

    testImplementation("org.spekframework.spek2:spek-dsl-jvm:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
    testRuntimeOnly ("org.spekframework.spek2:spek-runner-junit5:$spekVersion") {
        exclude(group = "org.jetbrains.kotlin")
    }
}

tasks {
    withType<Jar> {
        manifest.attributes["Main-Class"] = "no.nav.syfo.BootstrapKt"
    }

    create("printVersion") {

        doLast {
            println(project.version)
        }
    }
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "12"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }

    withType<Test> {
        useJUnitPlatform {
            includeEngines("spek2")
        }
        testLogging {
            showStandardStreams = true
        }
    }

    "check" {
        dependsOn("formatKotlin")
    }
}
