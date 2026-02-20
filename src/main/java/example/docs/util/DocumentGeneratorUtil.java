package example.docs.util;

import java.io.FileInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

public class DocumentGeneratorUtil {

    private static final String API_URL = "http://localhost:8080/api/v1/documents";

    public static void main(String[] args) {
        int count = loadCountFromProperties();
        if (count <= 0) {
            System.out.println("Ошибка: Неверное количество документов в generator.properties");
            return;
        }

        System.out.println(" Начинаем генерацию " + count + " документов...");
        long startTime = System.currentTimeMillis();

        HttpClient client = HttpClient.newHttpClient();
        AtomicInteger successCount = new AtomicInteger(0);

        for (int i = 1; i <= count; i++) {
            String jsonBody = String.format("{\"author\":\"Author-%d\", \"title\":\"Generated Doc %d\"}", i, i);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            try {
                HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() == 201) {
                    successCount.incrementAndGet();
                }

                // Прогресс в консоль каждые 10 документов
                if (i % 10 == 0 || i == count) {
                    System.out.println(String.format("Прогресс: %d / %d создано...", i, count));
                }
            } catch (Exception e) {
                System.err.println("Ошибка при создании документа " + i + ": " + e.getMessage());
            }
        }

        long executionTime = System.currentTimeMillis() - startTime;
        System.out.println(String.format("Генерация завершена! Успешно создано: %d из %d.", successCount.get(), count));
        System.out.println(String.format("Время выполнения: %d мс.", executionTime));
    }

    private static int loadCountFromProperties() {
        Properties props = new Properties();
        try (FileInputStream in = new FileInputStream("generator.properties")) {
            props.load(in);
            return Integer.parseInt(props.getProperty("generator.count", "10"));
        } catch (Exception e) {
            System.err.println("Не удалось прочитать файл параметров. Используем значение по умолчанию: 10");
            return 10;
        }
    }

}
