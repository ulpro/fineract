$ErrorActionPreference = "Stop"

$buildDir = "build_docker"
if (Test-Path $buildDir) {
    Write-Output "Cleaning old build_docker folder..."
    Remove-Item -Recurse -Force $buildDir
}

Write-Output "Creating directories..."
New-Item -ItemType Directory -Path "$buildDir/extracted" > $null
New-Item -ItemType Directory -Path "$buildDir/plugins" > $null
New-Item -ItemType Directory -Path "$buildDir/pentahoReports" > $null

Write-Output "Extracting fineract-provider.jar..."
$mainJar = Get-ChildItem -Path "fineract-provider/build/libs" -Filter "fineract-provider-*.jar" | Where-Object {
    $_.Name -notmatch "-plain\.jar" -and $_.Name -notmatch "-sources\.jar" -and $_.Name -notmatch "-javadoc\.jar"
} | Select-Object -First 1

Push-Location "$buildDir/extracted"
& "C:\Program Files\Java\jdk-21\bin\jar.exe" -xf "../../fineract-provider/build/libs/$($mainJar.Name)"
Pop-Location

# Copy META-INF/orm.xml to BOOT-INF/classes/META-INF/orm.xml to ensure it's on the classpath
if (Test-Path "$buildDir/extracted/META-INF/orm.xml") {
    Write-Output "Ensuring orm.xml is in BOOT-INF/classes/META-INF/..."
    New-Item -ItemType Directory -Path "$buildDir/extracted/BOOT-INF/classes/META-INF" -Force | Out-Null
    Copy-Item -Path "$buildDir/extracted/META-INF/orm.xml" -Destination "$buildDir/extracted/BOOT-INF/classes/META-INF/orm.xml" -Force
}


Write-Output "Copying custom plugin JARs..."
# Copy plugin build outputs
Get-ChildItem -Path "custom" -Filter "*.jar" -Recurse | Where-Object {
    $_.FullName -match "build\\libs" -and $_.Name -notmatch "-sources\.jar" -and $_.Name -notmatch "-javadoc\.jar"
} | ForEach-Object {
    Write-Output "Copying: $($_.Name)"
    Copy-Item -Path $_.FullName -Destination "$buildDir/plugins"
}

# Copy dependency libraries from custom plugin libs folders
Get-ChildItem -Path "custom" -Filter "*.jar" -Recurse | Where-Object {
    $_.FullName -match "\\libs\\" -and $_.Name -notmatch "-sources\.jar" -and $_.Name -notmatch "-javadoc\.jar"
} | ForEach-Object {
    Write-Output "Copying lib: $($_.Name)"
    Copy-Item -Path $_.FullName -Destination "$buildDir/plugins" -ErrorAction SilentlyContinue
}

Write-Output "Copying pentahoReports..."
if (Test-Path "pentahoReports") {
    Copy-Item -Path "pentahoReports/*" -Destination "$buildDir/pentahoReports" -Recurse -Force
}

Write-Output "Generating single-stage Dockerfile..."
$dockerfileContent = @"
FROM eclipse-temurin:21-jre-alpine

# Install fontconfig and ttf-dejavu fonts (REQUIRED by Pentaho for PDF/HTML report rendering)
RUN apk add --no-cache fontconfig ttf-dejavu

WORKDIR /app

# Create a non-root group and user for security
RUN addgroup -S fineract && adduser -S fineract -G fineract

# Create folders for libraries, plugins, logs, reports, and temp files
RUN mkdir -p /app/libs /app/plugins /app/pentahoReports /var/logs/fineract /tmp/fineract \
    && chown -R fineract:fineract /app /var/logs/fineract /tmp/fineract

# Switch to the non-root user
USER fineract

# Copy the extracted main application classes and libs
COPY --chown=fineract:fineract extracted/ /app/extracted/

# Copy all compiled custom plugin JARs and their dependencies
COPY --chown=fineract:fineract plugins/ /app/plugins/

# Copy default Pentaho report definition files (.prpt templates)
COPY --chown=fineract:fineract pentahoReports/ /app/pentahoReports/

# Restore execute permissions on all directories (crucial for zip files created on Windows)
USER root
RUN find /app -type d -exec chmod +x {} +
USER fineract

# Expose default HTTP (8080) and HTTPS (8443) ports
EXPOSE 8080 8443

# Set default JVM and Spring Boot environment variables.
ENV FINERACT_SERVER_PORT=8443 \
    FINERACT_SERVER_SSL_ENABLED=true \
    FINERACT_PENTAHO_REPORTS_PATH="/app/pentahoReports" \
    JAVA_TOOL_OPTIONS="-Duser.home=/tmp/fineract -Dfile.encoding=UTF-8 -Duser.timezone=UTC -Djava.security.egd=file:/dev/./urandom" \
    FINERACT_LOG_PATH="/var/logs/fineract"

# Run Fineract as an extracted application with standard JVM flat classpath.
ENTRYPOINT ["java", "-cp", "/app/extracted:/app/extracted/BOOT-INF/classes:/app/extracted/BOOT-INF/lib/*:/app/plugins/*", "org.apache.fineract.ServerApplication"]
"@

$dockerfileContent | Out-File -FilePath "$buildDir/Dockerfile" -Encoding utf8

Write-Output "Done! Docker build directory is ready in: $buildDir"
