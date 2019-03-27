FROM navikt/java:11
COPY build/libs/*.jar app.jar
COPY icpc2.json .
COPY icd10.json .
ENV JAVA_OPTS="-Dlogback.configurationFile=logback-remote.xml -Xmx256M"
