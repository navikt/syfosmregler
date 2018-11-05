val commonsCSVVersion = "1.6"
val jacksonVersion = "2.9.6"

plugins {
    `kotlin-dsl`
}

repositories {
    jcenter()
}

dependencies {
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:$jacksonVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml:$jacksonVersion")
    implementation("org.apache.commons:commons-csv:$commonsCSVVersion")
}
