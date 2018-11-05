package no.nav.codegen

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files

open class CodegenTask : DefaultTask() {
    @OutputDirectory
    lateinit var outputDirectory: File

    @TaskAction
    fun generateTask() {
        val sourcePackage = outputDirectory.resolve("no").resolve("nav").resolve("syfo").toPath()
        Files.createDirectories(sourcePackage)
        generateDiagnoseCodes(sourcePackage)
    }
}
