# The layers that `./gradlew :updater:extractLayers` writes to build/image, the build context. With only COPY,
# an amd64 runner builds the arm64 image without emulation
FROM gcr.io/distroless/java25-debian13:nonroot@sha256:8ac5525549d14b8ddd34562f9fc8e6ba4840c995363a1507b17cadf8bb7ab57e
COPY --link dependencies/ /app/
COPY --link snapshot-dependencies/ /app/
COPY --link application/ /app/
# The user of the nonroot base, written down so that a wrong digest above can never run the updater as root
USER 65532:65532
# Changelogs are read from the classpath under /workspace, after the jar so that they never shadow its code: Liquibase's
# includeAll fails on a file: changelog through Spring (liquibase/liquibase#6978). C1 alone starts it 14% faster
ENTRYPOINT ["java", "-XX:TieredStopAtLevel=1", "-cp", "/app/updater.jar:/workspace", "com.drinkit.updater.UpdaterApplicationKt"]
