package code.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = initialize();

    private static Properties initialize() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("src/resources/application.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    public static Integer getPort() throws NumberFormatException {
        return Integer.parseInt(properties.getProperty("port", "80"));
    }

    public static String getDatabaseUrl() throws NullPointerException{
        String url = properties.getProperty("database.url");
        if (url == null) throw new NullPointerException("Property 'database.url' can't be null");
        return url;
    }

    public static Integer getMaxUsers() throws NumberFormatException{
        return Integer.parseInt(properties.getProperty("application.maxUsers", "100"));
    }
}
