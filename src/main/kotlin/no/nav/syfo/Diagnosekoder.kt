package no.nav.syfo

import no.nav.syfo.model.Diagnose
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.reflect.KClass

object Diagnosekoder {
    const val ICPC2_CODE = "2.16.578.1.12.4.1.1.7170"
    const val ICD10_CODE = "2.16.578.1.12.4.1.1.7110"

    interface Diagnosekode {
        val code: String
        val text: String
        val mapsTo: List<String>
        val oid: String
    }

    data class ICPC2(
        override val code: String,
        override val text: String,
        override val mapsTo: List<String>,
        override val oid: String = ICPC2_CODE
    ) : Diagnosekode {
        fun toICD10(): List<ICD10> = mapsTo.mapNotNull { icd10[it] }
    }

    data class ICD10(
        override val code: String,
        override val text: String,
        override val mapsTo: List<String>,
        override val oid: String = ICPC2_CODE
    ) : Diagnosekode {
        fun toICPC2(): List<ICPC2> = mapsTo.mapNotNull { icpc2[it] }
    }

    val icd10 = loadCodes(Paths.get("icd10.json"), Array<ICD10>::class)
    val icpc2 = loadCodes(Paths.get("icpc2.json"), Array<ICPC2>::class)

    fun <T : Diagnosekode> loadCodes(jsonFile: Path, type: KClass<Array<T>>) =
            objectMapper.readValue(jsonFile.toFile(), type.java)
                    .map { it.code to it }
                    .toMap()
}

fun Diagnose.isICPC2(): Boolean = system == Diagnosekoder.ICPC2_CODE

fun Diagnose.toICPC2() = if (isICPC2()) {
    listOf(Diagnosekoder.icpc2[kode])
} else {
    Diagnosekoder.icd10[kode]?.toICPC2() ?: listOf()
}
