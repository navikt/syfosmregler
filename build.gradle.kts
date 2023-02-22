import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import com.github.jengelman.gradle.plugins.shadow.transformers.ServiceFileTransformer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.io.ByteArrayOutputStream

group = "no.nav.syfo"
version = "1.0.0"

val caffeineVersion = "3.1.2"
val coroutinesVersion = "1.6.4"
val jacksonVersion = "2.14.1"
val kluentVersion = "1.72"
val ktorVersion = "2.2.3"
val logbackVersion = "1.4.5"
val logstashEncoderVersion = "7.2"
val prometheusVersion = "0.16.0"
val smCommonVersion = "1.fbf33a9"
val kotestVersion = "5.5.4"
val mockkVersion = "1.13.2"
val kotlinVersion = "1.8.10"
val nettyCodecVersion = "4.1.86.Final"


plugins {
    kotlin("jvm") version "1.8.10"
    id("org.jmailen.kotlinter") version "3.10.0"
    id("com.diffplug.spotless") version "6.5.0"
    id("com.github.johnrengelman.shadow") version "7.1.2"
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
    implementation("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("io.prometheus:simpleclient_hotspot:$prometheusVersion")
    implementation("io.prometheus:simpleclient_common:$prometheusVersion")

    implementation("io.ktor:ktor-server-core:$ktorVersion")
    implementation("io.ktor:ktor-server-netty:$ktorVersion")
    //This is to override version that is in io.ktor:ktor-server-netty
    //https://www.cve.org/CVERecord?id=CVE-2022-41915
    implementation("io.netty:netty-codec:$nettyCodecVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages:$ktorVersion")
    implementation("io.ktor:ktor-server-cors:$ktorVersion")
    implementation("io.ktor:ktor-server-auth:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt:$ktorVersion")
    implementation("io.ktor:ktor-serialization-jackson:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-client-apache:$ktorVersion")

    implementation("no.nav.helse:syfosm-common-models:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-networking:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-diagnosis-codes:$smCommonVersion")
    implementation("no.nav.helse:syfosm-common-kafka:$smCommonVersion")

    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("net.logstash.logback:logstash-logback-encoder:$logstashEncoderVersion")

    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("com.github.ben-manes.caffeine:caffeine:$caffeineVersion")

    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("org.amshove.kluent:kluent:$kluentVersion")
    testImplementation("io.kotest:kotest-runner-junit5:$kotestVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion") {
        exclude(group = "org.eclipse.jetty")
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
        kotlinOptions.jvmTarget = "17"
    }

    withType<ShadowJar> {
        transform(ServiceFileTransformer::class.java) {
            setPath("META-INF/cxf")
            include("bus-extensions.txt")
        }
    }
    withType<Test> {
        useJUnitPlatform {
        }
        testLogging {
            events("skipped", "failed")
            showStackTraces = true
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }

    register<JavaExec>("generateTilbakedateringRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.tilbakedatering.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of tilbakedatering rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- TILBAKEDATERING_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- TILBAKEDATERING_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                    listOf(
                        "<!-- TILBAKEDATERING_MARKER_START -->",
                        "```mermaid",
                    ) +
                    output.toString().split("\n") +
                    listOf(
                        "```",
                        "<!-- TILBAKEDATERING_MARKER_END -->",
                    ) +
                    lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }
    register<JavaExec>("generateHPRRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.hpr.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of hpr rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- HPR_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- HPR_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            "<!-- HPR_MARKER_START -->",
                            "```mermaid",
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            "```",
                            "<!-- HPR_MARKER_END -->",
                        ) +
                        lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }

    register<JavaExec>("generateLegesuspensjonRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.legesuspensjon.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of hpr rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- LEGESUSPENSJON_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- LEGESUSPENSJON_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            "<!-- LEGESUSPENSJON_MARKER_START -->",
                            "```mermaid",
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            "```",
                            "<!-- LEGESUSPENSJON_MARKER_END -->",
                        ) +
                        lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }
    register<JavaExec>("generatePeriodLogicRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.periodlogic.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of hpr rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- PERIODLOGIC_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- PERIODLOGIC_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            "<!-- PERIODLOGIC_MARKER_START -->",
                            "```mermaid",
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            "```",
                            "<!-- PERIODLOGIC_MARKER_END -->",
                        ) +
                        lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }

    register<JavaExec>("generateValidationRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.validation.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of hpr rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- VALIDATION_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- VALIDATION_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            "<!-- VALIDATION_MARKER_START -->",
                            "```mermaid",
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            "```",
                            "<!-- VALIDATION_MARKER_END -->",
                        ) +
                        lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }

    register<JavaExec>("generatepatientAgeOver70RuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.patientageover70.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of patient age over 70 rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- PATIENT_AGE_OVER_70_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- PATIENT_AGE_OVER_70_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            "<!-- PATIENT_AGE_OVER_70_MARKER_START -->",
                            "```mermaid",
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            "```",
                            "<!-- PATIENT_AGE_OVER_70_MARKER_END -->",
                        ) +
                        lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }

    register<JavaExec>("generateArbeidsuforhetRuleMermaid") {
        val output = ByteArrayOutputStream()
        mainClass.set("no.nav.syfo.rules.arbeidsuforhet.GenerateMermaidKt")
        classpath = sourceSets["main"].runtimeClasspath
        group = "documentation"
        description = "Generates mermaid diagram source of hpr rules"
        standardOutput = output
        doLast {
            val readme = File("README.md")
            val lines = readme.readLines()
            val start = lines.indexOfFirst { it.contains("<!-- ARBEIDSUFOREHET_MARKER_START -->") }
            val end = lines.indexOfFirst { it.contains("<!-- ARBEIDSUFOREHET_MARKER_END -->") }
            val newLines: List<String> =
                lines.subList(0, start) +
                        listOf(
                            "<!-- ARBEIDSUFOREHET_MARKER_START -->",
                            "```mermaid",
                        ) +
                        output.toString().split("\n") +
                        listOf(
                            "```",
                            "<!-- ARBEIDSUFOREHET_MARKER_END -->",
                        ) +
                        lines.subList(end + 1, lines.size)


            readme.writeText(newLines.joinToString("\n"))
        }
    }

    "check" {
        dependsOn("formatKotlin")
        dependsOn("generateTilbakedateringRuleMermaid")
        dependsOn("generateHPRRuleMermaid")
        dependsOn("generateLegesuspensjonRuleMermaid")
        dependsOn("generatePeriodLogicRuleMermaid")
        dependsOn("generateValidationRuleMermaid")
        dependsOn("generatepatientAgeOver70RuleMermaid")
        dependsOn("generateArbeidsuforhetRuleMermaid")
    }
}
