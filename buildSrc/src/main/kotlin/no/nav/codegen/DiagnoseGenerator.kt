package no.nav.codegen

import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import java.io.BufferedReader
import java.io.InputStreamReader
import java.nio.file.Files
import java.nio.file.Path

import java.io.InputStream
import org.apache.poi.ss.usermodel.WorkbookFactory
import java.nio.file.Paths

data class Entry(
        val icpc2CodeValue: String,
        val icpc2FullText: String,
        val icd10CodeValue: String,
        val icd10Text: String
) {
    val icpc2EnumName = icpc2CodeValue.replace("-", "NEGATIVE_")
}

// Filen icd10 kan lastes ned fra https://ehelse.no/standarder-kodeverk-og-referansekatalog/helsefaglige-kodeverk/kodeverket-icd-10-og-icd-11
// Filen ipc2withicd10 kan lastes ned fra https://ehelse.no/standarder-kodeverk-og-referansekatalog/helsefaglige-kodeverk/icpc-2-den-internasjonale-klassifikasjonen-for-primerhelsetjenesten
fun generateDiagnoseCodes(outputDirectory: Path) {
    readAndWriteDiagnoses(
            outputDirectory,
            Files.newInputStream(Paths.get("src/main/resources/ICD-10_2019.xlsx")),
            Files.newInputStream(Paths.get("src/main/resources/ICPC2_til_ICD-10_CSV_1_1_2019.txt")))
}

fun readAndWriteDiagnoses(outputDirectory: Path, icd10Input: InputStream, ipc2withicd10Input: InputStream ) {
    val entries = BufferedReader(InputStreamReader(ipc2withicd10Input)).use { reader ->
        reader.readLines()
                .filter { !it.matches(Regex(".?--.+")) }
                .map { CSVParser.parse(it, CSVFormat.DEFAULT.withDelimiter(';')) }.flatMap { it.records }
                .map {
                    Entry (icpc2CodeValue = it[0], icpc2FullText = it[2], icd10CodeValue = it[3], icd10Text = it[4])
                }
    }

    Files.newBufferedWriter(outputDirectory.resolve("ICPC2.kt")).use { writer ->
        writer.write("package no.nav.syfo\n\n")
        writer.write("enum class ICPC2(override val codeValue: String, override val text: String, val icd10: List<ICD10>, override val oid: String = \"2.16.578.1.12.4.1.1.7170\") : Kodeverk {\n")
        entries
                .groupBy { it.icpc2CodeValue }
                .map {
                    (_, entries) ->
                    val firstEntry = entries.first()
                    "    ${firstEntry.icpc2EnumName}(\"${firstEntry.icpc2CodeValue}\", \"${firstEntry.icpc2FullText}\", ${entries.joinToString(", ", "listOf(", ")") { "ICD10.${it.icd10CodeValue}" }}),\n"
                }
                .forEach { writer.write(it) }
        writer.write("}\n")
    }

    val icd10Mapping = entries.groupBy { it.icd10CodeValue }
            .toMap()
    val icd10Codes = readICD10Codes(icd10Input)
    Files.newBufferedWriter(outputDirectory.resolve("ICD10.kt")).use { writer ->
        writer.write("package no.nav.syfo\n\n")
        writer.write("enum class ICD10(override val codeValue: String, override val text: String, val icpc2: List<ICPC2>, override val oid: String = \"2.16.578.1.12.4.1.1.7110\") : Kodeverk {\n")
        icd10Codes.map { (code, description) ->
            val icpc2Values = icd10Mapping[code] ?: listOf()
            "    $code(\"$code\", \"$description\", ${icpc2Values.joinToString(", ", "listOf(", ")") { "ICPC2.${it.icpc2EnumName}" }}),\n"
        }.forEach { writer.write(it) }

        writer.write("}\n")
        writer.close()
    }
}

fun readICD10Codes(inputStream: InputStream) = WorkbookFactory.create(inputStream).use { workbook ->
    val icd10Sheet = workbook.getSheetAt(1)

    icd10Sheet.map { row ->
        row.getCell(0).toString() to row.getCell(1).toString()
    }
}
