# Используем официальный образ Maven с Java 21 для сборки
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# Финальный образ с приложением
FROM eclipse-temurin:21-jre

WORKDIR /app

# Копируем собранный JAR
COPY --from=build /app/target/demo-0.0.1-SNAPSHOT.jar app.jar

# Создаем пользователя и группу
RUN groupadd -r spring && useradd -r -g spring spring

# Создаем директорию для логов с правильными правами
RUN mkdir -p /app/logs && \
    chown -R spring:spring /app && \
    chmod 755 /app && \
    chmod 775 /app/logs

# Переключаемся на пользователя spring
USER spring

EXPOSE 8080

# Запускаем приложение (логи будут в /app/logs/application.log)
ENTRYPOINT ["java", "-jar", "/app/app.jar"]