package ru.example.moneytransfer.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Этап 7: интеграционный тест в "настоящем" контейнере.
 * Собираем образ приложения из тестового Dockerfile (наш + COPY data),
 * поднимаем, прогоняем полный цикл transfer -> confirmOperation.
 */
@Testcontainers
class MoneyTransferIntegrationTest {

    private static final Pattern OP_ID = Pattern.compile("\"operationId\"\\s*:\\s*\"([^\"]+)\"");

    /** Наш Dockerfile + COPY data в рантайм-стадию, чтобы карты и лог лежали в образе. */
    private static final String DOCKERFILE = """
            FROM maven:3.9-eclipse-temurin-21 AS build
            WORKDIR /app
            COPY pom.xml .
            RUN mvn dependency:go-offline -B
            COPY src ./src
            RUN mvn clean package -DskipTests -B

            FROM eclipse-temurin:21-jre
            WORKDIR /app
            COPY --from=build /app/target/*.jar app.jar
            COPY data ./data
            EXPOSE 8080
            ENTRYPOINT ["java", "-jar", "app.jar"]
            """;

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> app = new GenericContainer<>(
            new ImageFromDockerfile("money-transfer-it", false)
                    .withFileFromString("Dockerfile", DOCKERFILE)
                    .withFileFromFile("pom.xml", Path.of("pom.xml").toFile())
                    .withFileFromPath("src", Path.of("src"))
                    .withFileFromPath("data", Path.of("data")))
            .withExposedPorts(8080)
            // 404 на "/" — норма для REST-сервиса без корневого маппинга
            .waitingFor(Wait.forHttp("/").forStatusCodeMatching(status -> status < 500))
            .withLogConsumer(frame -> System.out.print("[APP] " + frame.getUtf8String()))
            .withStartupTimeout(Duration.ofSeconds(120));

    static final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    static String base;

    @BeforeAll
    static void setUp() {
        base = "http://" + app.getHost() + ":" + app.getMappedPort(8080);
    }

    private static HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        return http.send(req, HttpResponse.BodyHandlers.ofString());
    }

    private static String transferBody(String from, String to, int valueKopecks) {
        return """
                {"cardFromNumber":"%s","cardFromValidTill":"12/29","cardFromCVV":"123",
                 "cardToNumber":"%s","amount":{"value":%d,"currency":"RUR"}}"""
                .formatted(from, to, valueKopecks);
    }

    private static String confirmBody(String opId, String code) {
        return """
                {"operationId":"%s","code":"%s"}""".formatted(opId, code);
    }

    private static String operationIdOf(String body) {
        Matcher m = OP_ID.matcher(body);
        assertTrue(m.find(), "В ответе нет operationId: " + body);
        return m.group(1);
    }

    @Test
    void fullCycle_transferConfirmRepeat_successThen400() throws Exception {
        // 1. Перевод
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 10_000));
        assertEquals(200, t.statusCode(), t.body());
        String opId = operationIdOf(t.body());

        // 2. Подтверждение кодом 0000
        var c = post("/confirmOperation", confirmBody(opId, "0000"));
        assertEquals(200, c.statusCode(), c.body());

        // 3. Повторное подтверждение -> 400
        var again = post("/confirmOperation", confirmBody(opId, "0000"));
        assertEquals(400, again.statusCode(), again.body());
        assertTrue(again.body().contains("message") && again.body().contains("id"));

        // 4. Лог внутри контейнера содержит успешную операцию
        var log = app.execInContainer("cat", "/app/data/transfer.log");
        assertTrue(log.getStdout().contains("SUCCESS"), log.getStdout());
    }

    @Test
    void transfer_invalidCardNumber_returns400() throws Exception {
        var t = post("/transfer", transferBody("1234567890123456", "5555555555554444", 10_000));
        assertEquals(400, t.statusCode(), t.body());
        assertTrue(t.body().contains("message") && t.body().contains("id"), t.body());
    }

    @Test
    void transfer_insufficientFunds_returns500() throws Exception {
        // 500 000 руб. — заведомо больше любого баланса в тестовых картах
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 50_000_000));
        assertEquals(500, t.statusCode(), t.body());
    }

    @Test
    void confirmOperation_unknownId_returns400() throws Exception {
        var c = post("/confirmOperation",
                confirmBody("00000000-0000-0000-0000-000000000000", "0000"));
        assertEquals(400, c.statusCode(), c.body());
    }

    @Test
    void confirmOperation_wrongCode_returns400() throws Exception {
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 5_000));
        assertEquals(200, t.statusCode(), t.body());

        var c = post("/confirmOperation", confirmBody(operationIdOf(t.body()), "9999"));
        assertEquals(400, c.statusCode(), c.body());
    }
}