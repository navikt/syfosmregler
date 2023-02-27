[![Build status](https://github.com/navikt/syfosmregler/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)](https://github.com/navikt/syfosmregler/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)

# SYFO sykemelding regler
This project contains just the rules for validating whether a user is supposed to get paid sick leave


# Technologies used
* Kotlin
* Ktor
* Gradle
* Kotest
* Jackson

#### Requirements

* JDK 17
* Docker

## FlowChart
This the high level flow for the application
```mermaid
  graph LR;
      syfosmmottak --- syfosmregler;
      syfosmregler --- syfohelsenettproxy;
      syfosmregler --- syfosmregister;
      syfosmregler --- PDL;
      syfosmregler --- flex-syketilfelle;
      syfosmregler --- smgcp-proxy;
      smgcp-proxy --- btsys;
```

## Getting started
### Getting github-package-registry packages NAV-IT
Some packages used in this repo is uploaded to the GitHub Package Registry which requires authentication. It can, for example, be solved like this in Gradle:
```
val githubUser: String by project
val githubPassword: String by project
repositories {
    maven {
        credentials {
            username = githubUser
            password = githubPassword
        }
        setUrl("https://maven.pkg.github.com/navikt/syfosm")
    }
}
```

`githubUser` and `githubPassword` can be put into a separate file `~/.gradle/gradle.properties` with the following content:

```                                                     
githubUser=x-access-token
githubPassword=[token]
```

Replace `[token]` with a personal access token with scope `read:packages`.
See githubs guide [creating-a-personal-access-token](https://docs.github.com/en/authentication/keeping-your-account-and-data-secure/creating-a-personal-access-token) on
how to create a personal access token.

Alternatively, the variables can be configured via environment variables:

* `ORG_GRADLE_PROJECT_githubUser`
* `ORG_GRADLE_PROJECT_githubPassword`

or the command line:

```
./gradlew -PgithubUser=x-access-token -PgithubPassword=[token]
```
#### Running locally
`./gradlew run`

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run `./gradlew shadowJar` or on windows 
`gradlew.bat shadowJar`

#### Creating a docker image
Creating a docker image should be as simple as `docker build -t syfosmregler .`

#### Running a docker image
`docker run --rm -it -p 8080:8080 syfosmregler`


### Upgrading the gradle wrapper
Find the newest version of gradle here: https://gradle.org/releases/ Then run this command:

```./gradlew wrapper --gradle-version $gradleVersjon```

### Contact

This project is maintained by navikt/teamsykmelding

Questions and/or feature requests? Please create an [issue](https://github.com/navikt/syfosmregler/issues).

If you work in [@navikt](https://github.com/navikt) you can reach us at the Slack
channel [#team-sykmelding](https://nav-it.slack.com/archives/CMA3XV997).

# Tilbakedatering rule flow

<!-- TILBAKEDATERING_MARKER_START -->
```mermaid
graph TD
    root(TILBAKEDATERING) -->|Yes| root_TILBAKEDATERING_ETTERSENDING(ETTERSENDING)
    root_TILBAKEDATERING_ETTERSENDING(ETTERSENDING) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING(ETTERSENDING) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER(TILBAKEDATERT_INNTIL_8_DAGER)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER(TILBAKEDATERT_INNTIL_8_DAGER) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_SPESIALISTHELSETJENESTEN_INVALID(INVALID):::invalid
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER(TILBAKEDATERT_INNTIL_8_DAGER) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER(TILBAKEDATERT_INNTIL_30_DAGER)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER(TILBAKEDATERT_INNTIL_30_DAGER) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE(FORLENGELSE) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE(ARBEIDSGIVERPERIODE)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE(ARBEIDSGIVERPERIODE) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE(ARBEIDSGIVERPERIODE) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_FORLENGELSE_ARBEIDSGIVERPERIODE_SPESIALISTHELSETJENESTEN_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD(BEGRUNNELSE_MIN_1_ORD) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN_OK(OK):::ok
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_1_ORD_SPESIALISTHELSETJENESTEN_INVALID(INVALID):::invalid
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER(TILBAKEDATERT_INNTIL_30_DAGER) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD(BEGRUNNELSE_MIN_3_ORD)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD(BEGRUNNELSE_MIN_3_ORD) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD(BEGRUNNELSE_MIN_3_ORD) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN)
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|Yes| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN(SPESIALISTHELSETJENESTEN) -->|No| root_TILBAKEDATERING_ETTERSENDING_TILBAKEDATERT_INNTIL_8_DAGER_TILBAKEDATERT_INNTIL_30_DAGER_BEGRUNNELSE_MIN_3_ORD_SPESIALISTHELSETJENESTEN_INVALID(INVALID):::invalid
    root(TILBAKEDATERING) -->|No| root_TILBAKEDATERING_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;


```
<!-- TILBAKEDATERING_MARKER_END -->






# HPR rule flow
<!-- HPR_MARKER_START -->
```mermaid
graph TD
    root(BEHANDLER_IKKE_GYLDIG_I_HPR) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_INVALID(INVALID):::invalid
    root(BEHANDLER_IKKE_GYLDIG_I_HPR) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR(BEHANDLER_MANGLER_AUTORISASJON_I_HPR)
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR(BEHANDLER_MANGLER_AUTORISASJON_I_HPR) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_INVALID(INVALID):::invalid
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR(BEHANDLER_MANGLER_AUTORISASJON_I_HPR) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR)
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR_INVALID(INVALID):::invalid
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR(BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR_BEHANDLER_MT_FT_KI_OVER_12_UKER(BEHANDLER_MT_FT_KI_OVER_12_UKER)
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR_BEHANDLER_MT_FT_KI_OVER_12_UKER(BEHANDLER_MT_FT_KI_OVER_12_UKER) -->|Yes| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR_BEHANDLER_MT_FT_KI_OVER_12_UKER_INVALID(INVALID):::invalid
    root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR_BEHANDLER_MT_FT_KI_OVER_12_UKER(BEHANDLER_MT_FT_KI_OVER_12_UKER) -->|No| root_BEHANDLER_IKKE_GYLDIG_I_HPR_BEHANDLER_MANGLER_AUTORISASJON_I_HPR_BEHANDLER_IKKE_LE_KI_MT_TL_FT_I_HPR_BEHANDLER_MT_FT_KI_OVER_12_UKER_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;


```
<!-- HPR_MARKER_END -->






# Legesuspensjon rule flow
<!-- LEGESUSPENSJON_MARKER_START -->
```mermaid
graph TD
    root(BEHANDLER_SUSPENDERT) -->|Yes| root_BEHANDLER_SUSPENDERT_INVALID(INVALID):::invalid
    root(BEHANDLER_SUSPENDERT) -->|No| root_BEHANDLER_SUSPENDERT_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;


```
<!-- LEGESUSPENSJON_MARKER_END -->






# PeriodLogic rule flow
<!-- PERIODLOGIC_MARKER_START -->
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
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE(IKKE_DEFINERT_PERIODE) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR(TILBAKEDATERT_MER_ENN_3_AR)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR(TILBAKEDATERT_MER_ENN_3_AR) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR(TILBAKEDATERT_MER_ENN_3_AR) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT(FREMDATERT)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT(FREMDATERT) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT(FREMDATERT) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR(TOTAL_VARIGHET_OVER_ETT_AAR)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR(TOTAL_VARIGHET_OVER_ETT_AAR) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR(TOTAL_VARIGHET_OVER_ETT_AAR) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO(BEHANDLINGSDATO_ETTER_MOTTATTDATO)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO(BEHANDLINGSDATO_ETTER_MOTTATTDATO) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO(BEHANDLINGSDATO_ETTER_MOTTATTDATO) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT(AVVENTENDE_SYKMELDING_KOMBINERT)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT(AVVENTENDE_SYKMELDING_KOMBINERT) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT(AVVENTENDE_SYKMELDING_KOMBINERT) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER(MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER(AVVENTENDE_SYKMELDING_OVER_16_DAGER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER(AVVENTENDE_SYKMELDING_OVER_16_DAGER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER(AVVENTENDE_SYKMELDING_OVER_16_DAGER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(FOR_MANGE_BEHANDLINGSDAGER_PER_UKE)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(FOR_MANGE_BEHANDLINGSDAGER_PER_UKE) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE(FOR_MANGE_BEHANDLINGSDAGER_PER_UKE) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT(GRADERT_SYKMELDING_OVER_99_PROSENT)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT(GRADERT_SYKMELDING_OVER_99_PROSENT) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_INVALID(INVALID):::invalid
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT(GRADERT_SYKMELDING_OVER_99_PROSENT) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER(SYKMELDING_MED_BEHANDLINGSDAGER)
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER(SYKMELDING_MED_BEHANDLINGSDAGER) -->|Yes| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER_MANUAL_PROCESSING(MANUAL_PROCESSING):::manuell
    root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER(SYKMELDING_MED_BEHANDLINGSDAGER) -->|No| root_PERIODER_MANGLER_FRADATO_ETTER_TILDATO_OVERLAPPENDE_PERIODER_OPPHOLD_MELLOM_PERIODER_IKKE_DEFINERT_PERIODE_TILBAKEDATERT_MER_ENN_3_AR_FREMDATERT_TOTAL_VARIGHET_OVER_ETT_AAR_BEHANDLINGSDATO_ETTER_MOTTATTDATO_AVVENTENDE_SYKMELDING_KOMBINERT_MANGLENDE_INNSPILL_TIL_ARBEIDSGIVER_AVVENTENDE_SYKMELDING_OVER_16_DAGER_FOR_MANGE_BEHANDLINGSDAGER_PER_UKE_GRADERT_SYKMELDING_OVER_99_PROSENT_SYKMELDING_MED_BEHANDLINGSDAGER_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;


```
<!-- PERIODLOGIC_MARKER_END -->






# Validation rule flow
<!-- VALIDATION_MARKER_START -->
```mermaid
graph TD
    root(PASIENT_YNGRE_ENN_13) -->|Yes| root_PASIENT_YNGRE_ENN_13_INVALID(INVALID):::invalid
    root(PASIENT_YNGRE_ENN_13) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON(UGYLDIG_REGELSETTVERSJON)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON(UGYLDIG_REGELSETTVERSJON) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON(UGYLDIG_REGELSETTVERSJON) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39(MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE(UGYLDIG_ORGNR_LENGDE) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR(AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR)
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|Yes| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR_INVALID(INVALID):::invalid
    root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR(BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR) -->|No| root_PASIENT_YNGRE_ENN_13_UGYLDIG_REGELSETTVERSJON_MANGLENDE_DYNAMISKE_SPOERSMAL_VERSJON2_UKE_39_UGYLDIG_ORGNR_LENGDE_AVSENDER_FNR_ER_SAMME_SOM_PASIENT_FNR_BEHANDLER_FNR_ER_SAMME_SOM_PASIENT_FNR_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;


```
<!-- VALIDATION_MARKER_END -->

<!-- PATIENT_AGE_OVER_70_MARKER_START -->
```mermaid
graph TD
    root(PASIENT_ELDRE_ENN_70) -->|Yes| root_PASIENT_ELDRE_ENN_70_INVALID(INVALID):::invalid
    root(PASIENT_ELDRE_ENN_70) -->|No| root_PASIENT_ELDRE_ENN_70_OK(OK):::ok
    classDef ok fill:#c3ff91,stroke:#004a00,color: black;
    classDef invalid fill:#ff7373,stroke:#ff0000,color: black;
    classDef manuell fill:#ffe24f,stroke:#ffd500,color: #473c00;


```
<!-- PATIENT_AGE_OVER_70_MARKER_END -->

<!-- ARBEIDSUFOREHET_MARKER_START -->
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
<!-- ARBEIDSUFOREHET_MARKER_END -->


<!-- GRADERT_MARKER_START -->

<!-- GRADERT_MARKER_END -->