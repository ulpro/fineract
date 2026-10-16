# ==============================================================================
# STAGE 1: Build the application and custom modules
# ==============================================================================
FROM eclipse-temurin:21-jdk AS builder

# Increase connection and read timeouts for the Gradle wrapper download to prevent network timeouts
ENV GRADLE_OPTS="-Dhttp.connectionTimeout=120000 -Dhttp.socketTimeout=120000 -Dorg.gradle.wrapper.connection.timeout=120000 -Dorg.gradle.wrapper.socket.timeout=120000"

WORKDIR /app

# Copy the entire workspace
COPY . .

# Optimize memory limits and add download retries for the Docker build to prevent crashes
RUN sed -i 's/-Xmx12g/-Xmx6g/g' gradle.properties && \
    sed -i 's/org.gradle.parallel=true/org.gradle.parallel=false/g' gradle.properties && \
    echo "org.gradle.workers.max=1" >> gradle.properties && \
    echo "systemProp.org.gradle.internal.repository.max.retries=10" >> gradle.properties && \
    echo "systemProp.org.gradle.internal.repository.max.tentatives=10" >> gradle.properties

# Build the application and all custom projects (including acme and mifos custom plugins)
# We use a cache mount for /root/.gradle so that downloaded dependencies are cached across runs
RUN --mount=type=cache,target=/root/.gradle ./gradlew clean assemble -x test -x cucumber -x rat -x spotlessJava -x spotlessCheck -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest -x modernizer --no-daemon --console=plain

# Collect all compiled custom plugin jars and their dependency jars (like Pentaho reporting libraries)
# We exclude sources and javadoc jars to prevent Liquibase from finding duplicate changelogs on the classpath
RUN mkdir -p /tmp/plugins && \
    find custom -type f -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -path "*/build/libs/*" -exec cp {} /tmp/plugins/ \; && \
    find custom -type f -name "*.jar" -not -name "*-sources.jar" -not -name "*-javadoc.jar" -path "*/libs/*" -exec cp {} /tmp/plugins/ \;

# Extract the main jar in builder stage to run it unpacked for maximum classpath lookup performance
RUN mkdir -p /tmp/extracted && \
    cd /tmp/extracted && \
    jar -xf /app/fineract-provider/build/libs/fineract-provider.jar

# ==============================================================================
# STAGE 2: Lightweight runtime image with Font Support (Crucial for Pentaho)
# ==============================================================================
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
COPY --from=builder --chown=fineract:fineract /tmp/extracted/ /app/extracted/

# Copy all compiled custom plugin JARs and their dependencies
COPY --from=builder --chown=fineract:fineract /tmp/plugins/ /app/plugins/

# Copy default Pentaho report definition files (.prpt templates)
COPY --from=builder --chown=fineract:fineract /app/pentahoReports/ /app/pentahoReports/

# Expose default HTTP (8080) and HTTPS (8443) ports
EXPOSE 8080 8443

# Set default JVM and Spring Boot environment variables.
ENV FINERACT_SERVER_PORT=8443 \
    FINERACT_SERVER_SSL_ENABLED=true \
    FINERACT_PENTAHO_REPORTS_PATH="/app/pentahoReports" \
    JAVA_TOOL_OPTIONS="-Duser.home=/tmp/fineract -Dfile.encoding=UTF-8 -Duser.timezone=UTC -Djava.security.egd=file:/dev/./urandom" \
    FINERACT_LOG_PATH="/var/logs/fineract"

# Run Fineract as an extracted application with standard JVM flat classpath.
# This completely bypasses the slow Spring Boot nested JarUrlClassLoader overhead.
ENTRYPOINT ["java", "-cp", "/app/extracted/BOOT-INF/classes:/app/extracted/BOOT-INF/lib/*:/app/plugins/*", "org.apache.fineract.ServerApplication"]
