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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Проверка TTL операции: в профиле ttl-test время жизни равно нулю,
 * поэтому confirm сразу после transfer должен вернуть 400.
 * Живёт в отдельном контейнере, чтобы не портить остальные тесты.
 */
@Testcontainers
class TtlIntegrationTest {

    private static final Pattern OP_ID = Pattern.compile("\"operationId\"\\s*:\\s*\"([^\"]+)\"");

    /**
     * Общий HttpClient на класс: держит пул соединений, а не пересоздаёт
     * их на каждый вызов — меньше нагрузки на Docker-прокси под Windows.
     */
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_1_1)
            .connectTimeout(Duration.ofSeconds(10))
            .build();

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
            new ImageFromDockerfile("money-transfer-ttl-it", false)
                    .withFileFromString("Dockerfile", DOCKERFILE)
                    .withFileFromFile("pom.xml", Path.of("pom.xml").toFile())
                    .withFileFromPath("src", Path.of("src"))
                    .withFileFromPath("data", Path.of("data")))
            .withExposedPorts(8080)
            .withEnv("SPRING_PROFILES_ACTIVE", "ttl-test")
            .withEnv("TRANSFER_CONFIRMCODE", "0000")
            .waitingFor(Wait.forHttp("/").forStatusCodeMatching(status -> status < 500))
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

    @Test
    void confirmOperation_expiredByTtl_returns400() throws Exception {
        var t = post("/transfer", """
                {"cardFromNumber":"4111111111111111","cardFromValidTill":"12/29",
                 "cardFromCVV":"123","cardToNumber":"5555555555554444",
                 "amount":{"value":10000,"currency":"RUR"}}""");
        assertEquals(200, t.statusCode(), t.body());
        Matcher m = OP_ID.matcher(t.body());
        assertTrue(m.find(), "В ответе нет operationId: " + t.body());
        String opId = m.group(1);

        var c = post("/confirmOperation", """
                {"operationId":"%s","code":"0000"}""".formatted(opId));
        assertEquals(400, c.statusCode(), c.body());
        assertTrue(c.body().contains("message") && c.body().contains("id"), c.body());
    }
}