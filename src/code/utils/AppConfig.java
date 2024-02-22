package code.utils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

public class AppConfig {
    private static final Properties properties = initialize();

    private AppConfig(){}

    private static Properties initialize() {
        Properties properties = new Properties();
        try (FileInputStream input = new FileInputStream("application.properties")) {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return properties;
    }

    public static Integer getPort() throws NumberFormatException {
        return Integer.parseInt(properties.getProperty("port", "800"));
    }

    public static String getDatabaseUrl() throws NullPointerException{
        String url = properties.getProperty("database.url");
        if (url == null) throw new NullPointerException("Property 'database.url' can't be null");
        return url;
    }

    public static Integer getMaxUsers() throws NumberFormatException{
        return Integer.parseInt(properties.getProperty("application.maxUsers", "100"));
    }

    public static Integer getDatabaseUpdatePeriod() throws NumberFormatException {
        return Integer.parseInt(properties.getProperty("database.updatePeriod", "60"));
    }

    public static Integer getRatingsUpdatePeriod() throws NumberFormatException {
        return Integer.parseInt(properties.getProperty("application.ratingUpdatePeriod", "60"));
    }

    public static String getMulticastGroup() {
        return properties.getProperty("multicast.group", "226.226.226.226");
    }

    public static Integer getMulticastPort() throws NumberFormatException {
        return Integer.parseInt(properties.getProperty("multicast.port", "4444"));
    }
}
