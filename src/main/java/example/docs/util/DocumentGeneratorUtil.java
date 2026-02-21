package example.docs.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentGeneratorUtil {

    public static void main(String[] args) {
        Properties props = loadProperties();
        int count = Integer.parseInt(props.getProperty("generator.document.count", "10"));
        String apiUrl = props.getProperty("generator.api.url", "http://localhost:8080/api/v1/documents");

        if (count <= 0) {
            System.err.println("Ошибка: Неверное количество документов в generator.properties");
            return;
        }

        System.out.println("Начинаем генерацию " + count + " документов по адресу: " + apiUrl);
        long startTime = System.currentTimeMillis();

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();

        AtomicInteger successCount = new AtomicInteger(0);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 1; i <= count; i++) {
            int docIndex = i;
            String jsonBody = String.format("{\"author\":\"Author-%d\", \"title\":\"Generated Doc %d\"}", docIndex, docIndex);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            CompletableFuture<Void> future = client.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenAccept(response -> {
                        if (response.statusCode() == 201) {
                            int currentSuccess = successCount.incrementAndGet();
                            if (currentSuccess % 10 == 0 || currentSuccess == count) {
                                System.out.println(String.format("Прогресс: %d / %d создано...", currentSuccess, count));
                            }
                        } else {
                            System.err.println("Ошибка на сервере для документа " + docIndex + ": HTTP " + response.statusCode());
                        }
                    })
                    .exceptionally(ex -> {
                        System.err.println("Ошибка сети при создании документа " + docIndex + ": " + ex.getMessage());
                        return null;
                    });

            futures.add(future);
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        long executionTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("Генерация завершена! Успешно создано: %d из %d.", successCount.get(), count));
        System.out.println(String.format("Время выполнения: %d мс.", executionTime));
    }

    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream input = new FileInputStream("generator.properties")) {
            props.load(input);
        } catch (Exception ex) {
            System.err.println("Файл generator.properties не найден в корне проекта. Используем дефолтные значения.");
        }
        return props;
    }
}