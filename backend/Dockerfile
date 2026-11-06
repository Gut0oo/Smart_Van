# Compila e executa os testes com o mesmo Java exigido pelo projeto.
FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app

COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw

COPY src/ src/
RUN ./mvnw -B -q package

# A imagem final contém apenas o Java de execução e o JAR da aplicação.
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN addgroup -S smartvan && adduser -S -G smartvan smartvan
COPY --from=build --chown=smartvan:smartvan /app/target/*.jar app.jar

USER smartvan
EXPOSE 8080

HEALTHCHECK --interval=10s --timeout=3s --start-period=40s --retries=5 \
    CMD wget -q -O /dev/null http://127.0.0.1:8080/actuator/health || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
