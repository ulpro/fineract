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
