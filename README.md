# SYFO sykemelding regler
This project contains just the rules for validating whether or not a user is supposed to get paid sick leave

# Technologies used
* Kotlin
* Ktor
* Gradle
* Spek
* Jackson


## Getting started
## Running locally
The application should be able to run locally in any IDE by just setting the environment variables
`SRVSYFOSYKEMELDINGREGLER_USERNAME` and `SYFOSYKEMELDINGREGLER_PASSWORD`

### Building the application
#### Compile and package application
To build locally and run the integration tests you can simply run `./gradlew installDist` or  on windows 
`gradlew.bat installDist`

#### Creating a docker image
Creating a docker image should be as simple as `docker build -t syfosmregler .`


## Contact us
### Code/project related questions can be sent to
* Kevin Sillerud, `kevin.sillerud@nav.no`
* Anders Østby, `anders.ostby@nav.no`
* Joakim Kartveit, `joakim.kartveit@nav.no`

### For NAV employees
We are available at the Slack channel #barken
