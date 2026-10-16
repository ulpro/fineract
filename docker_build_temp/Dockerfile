FROM eclipse-temurin:21-jre-jammy
WORKDIR /opt/fineract
COPY fineract-provider.jar /opt/fineract/fineract-provider.jar
EXPOSE 8443
ENTRYPOINT ["java", "-jar", "/opt/fineract/fineract-provider.jar"]
