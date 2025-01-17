import java.awt.Desktop;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Scanner;
import java.util.UUID;

class ShortLink {
    String originalUrl;
    String shortUrl;
    int clickLimit;
    int clickCount;
    long creationTime;
    long expirationTime;

    public ShortLink(String originalUrl, String shortUrl, int clickLimit, long expirationTime) {
        this.originalUrl = originalUrl;
        this.shortUrl = shortUrl;
        this.clickLimit = clickLimit;
        this.clickCount = 0;
        this.creationTime = System.currentTimeMillis();
        this.expirationTime = expirationTime;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expirationTime;
    }

    public boolean isClickLimitExceeded() {
        return clickCount >= clickLimit;
    }

    public void incrementClickCount() {
        clickCount++;
    }

    public void setClickLimit(int clickLimit) {
        this.clickLimit = clickLimit;
    }
}

class User {
    UUID userId;
    Map<String, ShortLink> links;

    public User() {
        this.userId = UUID.randomUUID();
        this.links = new HashMap<>();
    }

    public String createShortLink(String originalUrl, int clickLimit, long expirationTime) {
        String shortUrl = generateShortUrl(originalUrl);
        ShortLink shortLink = new ShortLink(originalUrl, shortUrl, clickLimit, expirationTime);
        links.put(shortUrl, shortLink);
        return shortUrl;
    }

    private String generateShortUrl(String originalUrl) {
        // Генерация уникальной короткой ссылки
        return "clck.ru/" + UUID.randomUUID().toString().substring(0, 8);
    }

    public String accessShortLink(String shortUrl) {
        ShortLink shortLink = links.get(shortUrl);
        if (shortLink == null) {
            return "Ссылка не найдена.";
        }

        if (shortLink.isExpired()) {
            return "Ссылка истекла.";
        }

        if (shortLink.isClickLimitExceeded()) {
            return "Лимит переходов исчерпан.";
        }

        shortLink.incrementClickCount();
        try {
            Desktop.getDesktop().browse(new URI(shortLink.originalUrl));
            return "Переход выполнен: " + shortLink.originalUrl;
        } catch (Exception e) {
            return "Ошибка при переходе по ссылке.";
        }
    }

    public String getLinkStatus(String shortUrl) {
        ShortLink shortLink = links.get(shortUrl);
        if (shortLink == null) {
            return "Ссылка не найдена.";
        }

        if (shortLink.isExpired()) {
            return "Ссылка истекла.";
        }

        if (shortLink.isClickLimitExceeded()) {
            return "Лимит переходов исчерпан.";
        }

        return "Ссылка активна. Осталось переходов: " + (shortLink.clickLimit - shortLink.clickCount);
    }

    public void deleteLink(String shortUrl) {
        links.remove(shortUrl);
    }

    public void setClickLimit(String shortUrl, int clickLimit) {
        ShortLink shortLink = links.get(shortUrl);
        if (shortLink != null) {
            shortLink.setClickLimit(clickLimit);
        }
    }
}

public class ShortLinkSystem {
    private Map<UUID, User> users;
    private Properties config;

    public ShortLinkSystem() {
        this.users = new HashMap<>();
        this.config = new Properties();
        loadConfig();
    }

    private void loadConfig() {
        try (FileInputStream fis = new FileInputStream("config.properties")) {
            config.load(fis);
        } catch (IOException e) {
            System.out.println("Ошибка загрузки конфигурационного файла. Используются значения по умолчанию.");
            config.setProperty("defaultExpirationTime", "1440"); // 1440 минут = 24 часа
            config.setProperty("defaultClickLimit", "5");
        }
    }

    public UUID createUser() {
        User user = new User();
        users.put(user.userId, user);
        return user.userId;
    }

    public String createShortLink(UUID userId, String originalUrl, int clickLimit, long expirationTimeInMinutes) {
        User user = users.get(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }

        // Преобразуем минуты в миллисекунды
        long defaultExpirationTime = Long.parseLong(config.getProperty("defaultExpirationTime")) * 60 * 1000;
        long actualExpirationTime = Math.min(expirationTimeInMinutes * 60 * 1000, defaultExpirationTime);

        int defaultClickLimit = Integer.parseInt(config.getProperty("defaultClickLimit"));
        int actualClickLimit = Math.min(clickLimit, defaultClickLimit);

        return user.createShortLink(originalUrl, actualClickLimit, System.currentTimeMillis() + actualExpirationTime);
    }

    public String accessShortLink(UUID userId, String shortUrl) {
        User user = users.get(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }
        return user.accessShortLink(shortUrl);
    }

    public String getLinkStatus(UUID userId, String shortUrl) {
        User user = users.get(userId);
        if (user == null) {
            return "Пользователь не найден.";
        }
        return user.getLinkStatus(shortUrl);
    }

    public void deleteLink(UUID userId, String shortUrl) {
        User user = users.get(userId);
        if (user != null) {
            user.deleteLink(shortUrl);
        }
    }

    public void setClickLimit(UUID userId, String shortUrl, int clickLimit) {
        User user = users.get(userId);
        if (user != null) {
            user.setClickLimit(shortUrl, clickLimit);
        }
    }

    public static void main(String[] args) {
        ShortLinkSystem system = new ShortLinkSystem();
        Scanner scanner = new Scanner(System.in);

        System.out.println("Добро пожаловать в сервис сокращения ссылок!");

        // Создаем двух пользователей
        UUID user1 = system.createUser();
        UUID user2 = system.createUser();

        System.out.println("Пользователь 1 создан. UUID: " + user1);
        System.out.println("Пользователь 2 создан. UUID: " + user2);

        // Создаем короткие ссылки на один и тот же URL для каждого пользователя
        String originalUrl = "https://skillfactory.ru";
        int clickLimit = 5;
        long expirationTimeInMinutes = 60; // 1 час

        String shortUrl1 = system.createShortLink(user1, originalUrl, clickLimit, expirationTimeInMinutes);
        String shortUrl2 = system.createShortLink(user2, originalUrl, clickLimit, expirationTimeInMinutes);

        System.out.println("Короткая ссылка пользователя 1: " + shortUrl1);
        System.out.println("Короткая ссылка пользователя 2: " + shortUrl2);

        // Проверяем, что ссылки разные
        if (!shortUrl1.equals(shortUrl2)) {
            System.out.println("Ссылки уникальны для каждого пользователя.");
        } else {
            System.out.println("Ошибка: Ссылки совпадают.");
        }

        // Основное меню для работы с сервисом
        UUID currentUser = user1; // По умолчанию работаем с первым пользователем
        while (true) {
            System.out.println("\nВыберите действие:");
            System.out.println("1. Создать короткую ссылку");
            System.out.println("2. Перейти по короткой ссылке");
            System.out.println("3. Проверить статус ссылки");
            System.out.println("4. Удалить ссылку");
            System.out.println("5. Изменить лимит переходов");
            System.out.println("6. Переключить пользователя");
            System.out.println("7. Выйти");

            int choice = scanner.nextInt();
            scanner.nextLine(); // Очистка буфера

            switch (choice) {
                case 1:
                    System.out.println("Введите длинный URL:");
                    String url = scanner.nextLine();
                    System.out.println("Введите лимит переходов:");
                    int limit = scanner.nextInt();
                    System.out.println("Введите время жизни ссылки (в минутах):");
                    long time = scanner.nextLong();
                    String shortUrl = system.createShortLink(currentUser, url, limit, time);
                    System.out.println("Короткая ссылка создана: " + shortUrl);
                    break;

                case 2:
                    System.out.println("Введите короткую ссылку:");
                    String accessUrl = scanner.nextLine();
                    String accessResult = system.accessShortLink(currentUser, accessUrl);
                    System.out.println(accessResult);
                    break;

                case 3:
                    System.out.println("Введите короткую ссылку:");
                    String statusUrl = scanner.nextLine();
                    String status = system.getLinkStatus(currentUser, statusUrl);
                    System.out.println(status);
                    break;

                case 4:
                    System.out.println("Введите короткую ссылку для удаления:");
                    String deleteUrl = scanner.nextLine();
                    system.deleteLink(currentUser, deleteUrl);
                    System.out.println("Ссылка удалена.");
                    break;

                case 5:
                    System.out.println("Введите короткую ссылку:");
                    String editUrl = scanner.nextLine();
                    System.out.println("Введите новый лимит переходов:");
                    int newLimit = scanner.nextInt();
                    system.setClickLimit(currentUser, editUrl, newLimit);
                    System.out.println("Лимит переходов изменён.");
                    break;

                case 6:
                    System.out.println("Выберите пользователя (1 или 2):");
                    int userChoice = scanner.nextInt();
                    currentUser = (userChoice == 1) ? user1 : user2;
                    System.out.println("Текущий пользователь: " + currentUser);
                    break;

                case 7:
                    System.out.println("Выход из системы.");
                    scanner.close();
                    return;

                default:
                    System.out.println("Неверный выбор. Попробуйте снова.");
            }
        }
    }
}