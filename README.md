[![Build status](https://github.com/navikt/syfosmregler/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)](https://github.com/navikt/syfosmregler/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)

# SYFO sykemelding regler
This project contains just the rules for validating whether a user is supposed to get paid sick leave


# Technologies used
* Kotlin
* Ktor
* Gradle
* Kotest
* Jackson

### :scroll: Prerequisites
* JDK 21
  Make sure you have the Java JDK 21 installed
  You can check which version you have installed using this command:
``` shell
java -version
```

* Docker
  Make sure you have the Docker installed
  You can check which version you have installed using this command:
``` shell
docker -version
```

## FlowChart
This the high level flow for the application
```mermaid
  graph LR;
      syfosmmottak --- syfosmregler;
      syfosmregler --- syfohelsenettproxy;
      syfosmregler --- syfosmregister;
      syfosmregler --- PDL;
      syfosmregler --- flex-syketilfelle;
      syfosmregler --- btsys;
```

## Getting started
#### Running locally
``` bash
./gradlew run
```

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run
``` bash
./gradlew shadowJar
```
or on windows 
`gradlew.bat shadowJar`

#### Creating a docker image
Creating a docker image should be as simple as 
``` bash
docker build -t syfosmregler .
```

#### Running a docker image
Remember to change the environment variables, to match your local services
``` bash
docker run --rm -it -p 8080:8080 -e "LEGE_SUSPENSJON_PROXY_ENDPOINT_URL=https://localhost:8081" -e "LEGE_SUSPENSJON_PROXY_SCOPE=localhost" -e "SYKETILLFELLE_SCOPE=localhost" -e "HELSENETT_ENDPOINT_URL=https://localhost:8081" -e "AZURE_OPENID_CONFIG_TOKEN_ENDPOINT=https://localhost:8081" -e "AZURE_APP_CLIENT_ID=localhost" -e "AZURE_APP_CLIENT_SECRET=localhost" -e "HELSENETT_SCOPE=localhost" -e "SMREGISTER_AUDIENCE=localhost" -e "PDL_SCOPE=localhost" -e "PDL_GRAPHQL_PATH=https://localhost:8081/graphql" -e "AZURE_OPENID_CONFIG_ISSUER=localhost" -e "AZURE_OPENID_CONFIG_JWKS_URI=https://localhost:8081" -e "KAFKA_BROKERS=https://localhost:8081" -e "KAFKA_TRUSTSTORE_PATH=super/secret/test.cert" -e "KAFKA_KEYSTORE_PATH=super/secret/private/test.cert" -e "KAFKA_CREDSTORE_PASSWORD=password" syfosmregler 
```


### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

``` bash
./gradlew wrapper --gradle-version $gradleVersjon
```

### Contact

This project is maintained by navikt/teamsykmelding

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syfosmregler/issues)

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997)


# Rules tree

<!-- RULE_MARKER_START -->
## 0. Lege suspensjon

---


---

```mermaid
graph TD
    root(BEHANDLER_SUSPENDERT) -->|Yes| root_BEHANDLER_SUSPENDERT_INVALID(INVALID):::invalid
    root(BEHANDLER_SUSPENDERT) -->|No| root_BEHANDLER_SUSPENDERT_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 1. Validation

---


---

```mermaid
graph TD
    root(UGYLDIG_REGELSETTVERSJON) -->|Yes| root_UGYLDIG_REGELSETTVERSJON_INVALID(INVALID):::invalid
    root(UGYLDIG_REGELSETTVERSJON) -->|No| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39)
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) -->|Yes| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_INVALID(INVALID):::invalid
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) -->|No| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE)
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE) -->|Yes| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_INVALID(INVALID):::invalid
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE) -->|No| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR)
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|Yes| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_INVALID(INVALID):::invalid
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|No| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR)
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|Yes| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR_INVALID(INVALID):::invalid
    root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|No| root_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 2. Periode validering

---


---

```mermaid
graph TD
    root(PERIODER_MANGLER) -->|Yes| root_PERIODER_MANGLER_INVALID(INVALID):::invalid
    root(PERIODER_MANGLER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO(FRADATO_ETTER_TILDATO)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO(FRADATO_ETTER_TILDATO) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO(FRADATO_ETTER_TILDATO) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER(OVERLAPPENDE_PERIODER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER(OVERLAPPENDE_PERIODER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER(OVERLAPPENDE_PERIODER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER(OPPHOLD_MELLOM_PERIODER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER(OPPHOLD_MELLOM_PERIODER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER(OPPHOLD_MELLOM_PERIODER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE(IKKE_DEFINERT_PERIODE)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE(IKKE_DEFINERT_PERIODE) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE(IKKE_DEFINERT_PERIODE) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO(BEHANDLINGSDATO_ETTER_MOTTATTDATO)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO(BEHANDLINGSDATO_ETTER_MOTTATTDATO) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO(BEHANDLINGSDATO_ETTER_MOTTATTDATO) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT(AVVENTENDE_SYKMELDING_KOMBINERT)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT(AVVENTENDE_SYKMELDING_KOMBINERT) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT(AVVENTENDE_SYKMELDING_KOMBINERT) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER(AVVENTENDE_SYKMELDING_OVER_16_DAGER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER(AVVENTENDE_SYKMELDING_OVER_16_DAGER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER(AVVENTENDE_SYKMELDING_OVER_16_DAGER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(FOR_MANGE_BEHANDLINGSDAGER_PER_UKE)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(FOR_MANGE_BEHANDLINGSDAGER_PER_UKE) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(FOR_MANGE_BEHANDLINGSDAGER_PER_UKE) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT(GRADERT_SYKMELDING_OVER_99_PROSENT)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT(GRADERT_SYKMELDING_OVER_99_PROSENT) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT(GRADERT_SYKMELDING_OVER_99_PROSENT) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER(SYKMELDING_MED_BEHANDLINGSDAGER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER(SYKMELDING_MED_BEHANDLINGSDAGER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER(SYKMELDING_MED_BEHANDLINGSDAGER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 3. HPR

---

- ### Juridisk Henvisning:
  - **Lovverk**: HELSEPERSONELLOVEN
  - **Paragraf**: 3

---

```mermaid
graph TD
    root(BEHANDLER_GYLIDG_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR(BEHANDLER_HAR_AUTORISASJON_I_HPR)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR(BEHANDLER_HAR_AUTORISASJON_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR(BEHANDLER_ER_LEGE_I_HPR)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR(BEHANDLER_ER_LEGE_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_OK(OK):::ok
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR(BEHANDLER_ER_LEGE_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR(BEHANDLER_ER_TANNLEGE_I_HPR)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR(BEHANDLER_ER_TANNLEGE_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_OK(OK):::ok
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR(BEHANDLER_ER_TANNLEGE_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR(BEHANDLER_ER_MANUELLTERAPEUT_I_HPR)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR(BEHANDLER_ER_MANUELLTERAPEUT_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_SYKEFRAVAR_OVER_12_UKER_INVALID(INVALID):::invalid
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_SYKEFRAVAR_OVER_12_UKER_OK(OK):::ok
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR(BEHANDLER_ER_MANUELLTERAPEUT_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR(BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR(BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER_INVALID(INVALID):::invalid
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER_OK(OK):::ok
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR(BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR(BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR(BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER)
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER) -->|Yes| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER_INVALID(INVALID):::invalid
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER(SYKEFRAVAR_OVER_12_UKER) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR_SYKEFRAVAR_OVER_12_UKER_OK(OK):::ok
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR(BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_BEHANDLER_ER_LEGE_I_HPR_BEHANDLER_ER_TANNLEGE_I_HPR_BEHANDLER_ER_MANUELLTERAPEUT_I_HPR_BEHANDLER_ER_FT_MED_TILLEGSKOMPETANSE_I_HPR_BEHANDLER_ER_KI_MED_TILLEGSKOMPETANSE_I_HPR_INVALID(INVALID):::invalid
    root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR(BEHANDLER_HAR_AUTORISASJON_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_BEHANDLER_HAR_AUTORISASJON_I_HPR_INVALID(INVALID):::invalid
    root(BEHANDLER_GYLIDG_I_HPR) -->|No| root_BEHANDLER_GYLIDG_I_HPR_INVALID(INVALID):::invalid
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 4. Arbeidsuforhet

---

- ### Juridisk Henvisning:
  - **Lovverk**: FOLKETRYGDLOVEN
  - **Paragraf**: 8-4
  - **Ledd**: 1

---

```mermaid
graph TD
    root(UKJENT_DIAGNOSEKODETYPE) -->|Yes| root_UKJENT_DIAGNOSEKODETYPE_INVALID(INVALID):::invalid
    root(UKJENT_DIAGNOSEKODETYPE) -->|No| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE(ICPC_2_Z_DIAGNOSE)
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE(ICPC_2_Z_DIAGNOSE) -->|Yes| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_INVALID(INVALID):::invalid
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE(ICPC_2_Z_DIAGNOSE) -->|No| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER)
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER) -->|Yes| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_INVALID(INVALID):::invalid
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER(HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER) -->|No| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE)
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE) -->|Yes| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE_INVALID(INVALID):::invalid
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE(UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE) -->|No| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE_UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(UGYLDIG_KODEVERK_FOR_BIDIAGNOSE)
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE_UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(UGYLDIG_KODEVERK_FOR_BIDIAGNOSE) -->|Yes| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE_UGYLDIG_KODEVERK_FOR_BIDIAGNOSE_INVALID(INVALID):::invalid
    root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE_UGYLDIG_KODEVERK_FOR_BIDIAGNOSE(UGYLDIG_KODEVERK_FOR_BIDIAGNOSE) -->|No| root_UKJENT_DIAGNOSEKODETYPE_ICPC_2_Z_DIAGNOSE_HOVEDDIAGNOSE_ELLER_FRAVAERSGRUNN_MANGLER_UGYLDIG_KODEVERK_FOR_HOVEDDIAGNOSE_UGYLDIG_KODEVERK_FOR_BIDIAGNOSE_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 5. Pasient under 13

---

- ### Juridisk Henvisning:
  - **Lovverk**: FOLKETRYGDLOVEN
  - **Paragraf**: 8-3
  - **Ledd**: 1

---

```mermaid
graph TD
    root(PASIENT_YNGRE_ENN_13) -->|Yes| root_PASIENT_YNGRE_ENN_13_INVALID(INVALID):::invalid
    root(PASIENT_YNGRE_ENN_13) -->|No| root_PASIENT_YNGRE_ENN_13_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 6. Periode

---


---

```mermaid
graph TD
    root(FREMDATERT) -->|Yes| root_FREMDATERT_INVALID(INVALID):::invalid
    root(FREMDATERT) -->|No| root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR(TILBAKEDATERT_MER_ENN_3_AR)
    root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR(TILBAKEDATERT_MER_ENN_3_AR) -->|Yes| root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR_INVALID(INVALID):::invalid
    root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR(TILBAKEDATERT_MER_ENN_3_AR) -->|No| root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR_TOTAL_VARIGHET_OVER_ETT_AAR(TOTAL_VARIGHET_OVER_ETT_AAR)
    root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR_TOTAL_VARIGHET_OVER_ETT_AAR(TOTAL_VARIGHET_OVER_ETT_AAR) -->|Yes| root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR_TOTAL_VARIGHET_OVER_ETT_AAR_INVALID(INVALID):::invalid
    root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR_TOTAL_VARIGHET_OVER_ETT_AAR(TOTAL_VARIGHET_OVER_ETT_AAR) -->|No| root_FREMDATERT_TILBAKEDATERT_MER_ENN_3_AR_TOTAL_VARIGHET_OVER_ETT_AAR_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```


## 7. Tilbakedatering

---

- ### Juridisk Henvisning:
  - **Lovverk**: FOLKETRYGDLOVEN
  - **Paragraf**: 8-7
  - **Ledd**: 2

---

```mermaid
graph TD
    root(TILBAKEDATERING) -->|Yes| root_TILBAKEDATERING_ETTERSENDING(ETTERSENDING)
    root_TILBAKEDATERING_ETTERSENDING(ETTERSENDING) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING(ETTERSENDING) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER(TILBAKEDATERING_OVER_4_DAGER)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER(TILBAKEDATERING_OVER_4_DAGER) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER(TILBAKEDATERT_INNTIL_8_DAGER)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER(TILBAKEDATERT_INNTIL_8_DAGER) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN_INVALID(INVALID):::invalid
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER(TILBAKEDATERT_INNTIL_8_DAGER) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER(TILBAKEDATERT_INNTIL_30_DAGER)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER(TILBAKEDATERT_INNTIL_30_DAGER) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE(ARBEIDSGIVERPERIODE)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE(ARBEIDSGIVERPERIODE) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE(ARBEIDSGIVERPERIODE) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN_INVALID(INVALID):::invalid
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER(TILBAKEDATERT_INNTIL_30_DAGER) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD(BEGRUNNELSE_MIN_3_ORD)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD(BEGRUNNELSE_MIN_3_ORD) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD(BEGRUNNELSE_MIN_3_ORD) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN_INVALID(INVALID):::invalid
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER(TILBAKEDATERING_OVER_4_DAGER) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERING_OVER_4_DAGER_OK(OK):::ok
    root(TILBAKEDATERING) -->|No| root_TILBAKEDATERING_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;
```



<!-- RULE_MARKER_END -->