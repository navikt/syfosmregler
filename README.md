[![Build status](https://github.com/navikt/syfosmregler/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)](https://github.com/navikt/syfosmregler/workflows/Deploy%20to%20dev%20and%20prod/badge.svg)

# SYFO sykemelding regler
This project contains just the rules for validating whether or not a user is supposed to get paid sick leave

# Technologies used
* Kotlin
* Ktor
* Gradle
* Spek
* Jackson

#### Requirements

* JDK 11

## Getting started
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

## Contact us
### Code/project related questions can be sent to
* Joakim Kartveit, `joakim.kartveit@nav.no`
* Andreas Nilsen, `andreas.nilsen@nav.no`
* Sebastian Knudsen, `sebastian.knudsen@nav.no`
* Tia Firing, `tia.firing@nav.no`

### For NAV employees
We are available at the Slack channel #team-sykmelding
