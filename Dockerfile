FROM amazoncorretto:25-headless

WORKDIR /opt/app

COPY lemonka-tools.jar .

EXPOSE 8000
CMD ["java", "-Djava.awt.headless=true", "-jar", "/opt/app/lemonka-tools.jar"]