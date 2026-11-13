FROM eclipse-temurin:25-jre

WORKDIR /opt/app
COPY lemonka-tools.jar .

ENV DB_PATH=/opt/app/data/cache.db
EXPOSE 8000
CMD ["java", "-Djava.awt.headless=true", "-jar", "/opt/app/lemonka-tools.jar"]
