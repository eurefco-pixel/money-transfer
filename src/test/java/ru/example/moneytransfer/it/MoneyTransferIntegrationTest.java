package ru.example.moneytransfer.it;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.images.builder.ImageFromDockerfile;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Testcontainers
class MoneyTransferIntegrationTest {

    private static final Pattern OP_ID = Pattern.compile("\"operationId\"\s*:\s*\"([^\"]+)\"");

    /**
     * Наш Dockerfile + COPY data в рантайм-стадию, чтобы карты и лог лежали в образе.
     */
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

    /**
     * Один клиент на весь класс: переиспользует соединения вместо нового сокета на каждый запрос.
     */
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> app = new GenericContainer<>(
            new ImageFromDockerfile("money-transfer-it", false)
                    .withFileFromString("Dockerfile", DOCKERFILE)
                    .withFileFromFile("pom.xml", Path.of("pom.xml").toFile())
                    .withFileFromPath("src", Path.of("src"))
                    .withFileFromPath("data", Path.of("data")))
            .withExposedPorts(8080)
            .withEnv("TRANSFER_CONFIRMCODE", "0000")
            .waitingFor(Wait.forHttp("/").forStatusCodeMatching(status -> status < 500))
            .withLogConsumer(frame -> System.out.print("[APP] " + frame.getUtf8String()))
            .withStartupTimeout(Duration.ofSeconds(120));

    static String base;

    @BeforeAll
    static void setUp() {
        base = "http://" + app.getHost() + ":" + app.getMappedPort(8080);
    }

    /**
     * Ретрай на сетевые сбои Docker-прокси под Windows (Connection reset, «no bytes»):
     * на WSL2 они случаются одноразово, повторный запрос проходит.
     */
    private static HttpResponse<String> post(String path, String json) throws Exception {
        HttpRequest req = HttpRequest.newBuilder(URI.create(base + path))
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(15))
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();
        IOException last = null;
        for (int attempt = 1; attempt <= 5; attempt++) {
            try {
                return HTTP.send(req, HttpResponse.BodyHandlers.ofString());
            } catch (IOException e) {
                last = e;
                // Затухающая пауза: 250, 500, 750, 1000, 1250 мс — окно до ~4 сек
                Thread.sleep(250L * attempt);
            }
        }
        throw last;
    }

    private static String transferBody(String from, String to, int valueKopecks) {
        return transferBodyWithValidTill(from, "12/29", to, valueKopecks);
    }

    private static String transferBodyWithCurrency(String from, String to, int valueKopecks, String currency) {
        return transferBodyWithValidTillAndCurrency(from, "12/29", to, valueKopecks, currency);
    }

    private static String transferBodyWithValidTillAndCurrency(
            String from, String validTill, String to, int valueKopecks, String currency) {
        return """
                {"cardFromNumber":"%s","cardFromValidTill":"%s","cardFromCVV":"123",
                 "cardToNumber":"%s","amount":{"value":%d,"currency":"%s"}}"""
                .formatted(from, validTill, to, valueKopecks, currency);
    }

    private static String transferBodyWithValidTill(String from, String validTill, String to, int valueKopecks) {
        return """
                {"cardFromNumber":"%s","cardFromValidTill":"%s","cardFromCVV":"123",
                 "cardToNumber":"%s","amount":{"value":%d,"currency":"RUR"}}"""
                .formatted(from, validTill, to, valueKopecks);
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
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 10_000));
        assertEquals(200, t.statusCode(), t.body());
        String opId = operationIdOf(t.body());

        var c = post("/confirmOperation", confirmBody(opId, "0000"));
        assertEquals(200, c.statusCode(), c.body());

        var again = post("/confirmOperation", confirmBody(opId, "0000"));
        assertEquals(400, again.statusCode(), again.body());
        assertTrue(again.body().contains("message") && again.body().contains("id"));

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
    void transfer_validationError_isLoggedAsFailed() throws Exception {
        var t = post("/transfer", transferBody("1234567890123456", "5555555555554444", 10_000));
        assertEquals(400, t.statusCode(), t.body());

        var log = app.execInContainer("cat", "/app/data/transfer.log");
        assertTrue(log.getStdout().contains("FAILED"), "В логе нет FAILED-записи: " + log.getStdout());
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

    @Test
    void transfer_expiredValidTill_returns400() throws Exception {
        var t = post("/transfer", transferBodyWithValidTill("4111111111111111", "01/20", "5555555555554444", 10_000));
        assertEquals(400, t.statusCode(), t.body());
        assertTrue(t.body().contains("message") && t.body().contains("id"), t.body());
    }

    @Test
    void transfer_unsupportedCurrency_returns400() throws Exception {
        var t = post("/transfer", transferBodyWithCurrency(
                "4111111111111111", "5555555555554444", 10_000, "USD"));
        assertEquals(400, t.statusCode(), t.body());
        assertTrue(t.body().contains("message") && t.body().contains("id"), t.body());
    }

    @Test
    void transfer_amountAboveLimit_returns400() throws Exception {
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 100_000_001));
        assertEquals(400, t.statusCode(), t.body());
        assertTrue(t.body().contains("message") && t.body().contains("id"), t.body());
    }

    @Test
    void confirmOperation_concurrentDoubleConfirm_deductsOnce() throws Exception {
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 10_000));
        assertEquals(200, t.statusCode(), t.body());
        String opId = operationIdOf(t.body());

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            var f1 = pool.submit(() -> post("/confirmOperation", confirmBody(opId, "0000")));
            var f2 = pool.submit(() -> post("/confirmOperation", confirmBody(opId, "0000")));
            var r1 = f1.get(15, TimeUnit.SECONDS);
            var r2 = f2.get(15, TimeUnit.SECONDS);

            var statuses = List.of(r1.statusCode(), r2.statusCode());
            assertTrue(statuses.contains(200), "Ни один confirm не прошёл: " + statuses);
            assertTrue(statuses.contains(400), "Оба confirm прошли — двойное списание! " + statuses);
        } finally {
            pool.shutdownNow();
        }

        var log = app.execInContainer("cat", "/app/data/transfer.log");
        long successCount = log.getStdout().lines()
                .filter(line -> line.contains("SUCCESS") && line.contains(opId))
                .count();
        assertEquals(1L, successCount,
                "Ожидался ровно один SUCCESS, найдено " + successCount);
    }

    @Test
    void transfer_malformedJson_returns400AndLogged() throws Exception {
        var t = post("/transfer", "{\"cardFrom\":");
        assertEquals(400, t.statusCode(), t.body());

        var log = app.execInContainer("cat", "/app/data/transfer.log");
        assertTrue(log.getStdout().contains("Некорректный JSON"),
                "В логе нет записи о битом JSON: " + log.getStdout());
    }

    @Test
    void transfer_sameCard_returns400() throws Exception {
        var t = post("/transfer", transferBody("4111111111111111", "4111111111111111", 10_000));
        assertEquals(400, t.statusCode(), t.body());
        assertTrue(t.body().contains("message") && t.body().contains("id"), t.body());

        var log = app.execInContainer("cat", "/app/data/transfer.log");
        assertTrue(log.getStdout().contains("Нельзя переводить на ту же карту"),
                "В логе нет записи о переводе на ту же карту: " + log.getStdout());
    }

    @Test
    void transfer_sameCardWithSpaces_returns400() throws Exception {
        // Регрессия: пробелы в номере не должны обходить проверку
        var t = post("/transfer", transferBody("4111 1111 1111 1111", "4111111111111111", 10_000));
        assertEquals(400, t.statusCode(), t.body());
    }

    @Test
    void confirmOperation_tooManyAttempts_blocksOperation() throws Exception {
        var t = post("/transfer", transferBody("4111111111111111", "5555555555554444", 5_000));
        assertEquals(200, t.statusCode(), t.body());
        String opId = operationIdOf(t.body());

        // Три неверных ввода — разрешённый максимум
        for (int i = 0; i < 3; i++) {
            var c = post("/confirmOperation", confirmBody(opId, "1111"));
            assertEquals(400, c.statusCode(), c.body());
        }

        // Четвёртый — с верным кодом — всё равно 400: операция заблокирована
        var last = post("/confirmOperation", confirmBody(opId, "0000"));
        assertEquals(400, last.statusCode(), last.body());
        assertTrue(last.body().contains("Превышено число попыток"), last.body());
    }
}