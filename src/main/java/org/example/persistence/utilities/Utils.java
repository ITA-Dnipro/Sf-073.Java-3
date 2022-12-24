package org.example.persistence.utilities;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.example.persistence.ormanager.ORManager;
import org.example.persistence.ormanager.ORManagerImpl;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.sql.Connection;
import java.util.Map;
import java.util.Properties;

public class Utils {
    private Connection connection;
    private Utils() {
    }

    public static ORManager withPropertiesFrom(String filename) {
        Path path = Path.of(filename);
        Properties properties = readProperties(path);

        String jdbcUrl = properties.getProperty("jdbc-url");
        String jdbcUser = properties.getProperty("jdbc-username", "");
        String jdbcPass = properties.getProperty("jdbc-pass", "");

        return new ORManagerImpl(createDataSource(jdbcUrl, jdbcUser, jdbcPass, Map.of()));
    }

    public static Properties readProperties(Path file) {
        Properties result = new Properties();
        try (InputStream inputStream = Utils.class.getClassLoader().getResourceAsStream(file.toString())) {
            result.load(inputStream);
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    static DataSource createDataSource(String url, String user, String password, Map<String, String> props) {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);

//        HikariDataSource dataSource = new HikariDataSource();
//        dataSource.setJdbcUrl(url);
//        dataSource.setUsername(user);
//        dataSource.setPassword(password);
//        return dataSource;

        return new HikariDataSource(config);
    }

    public static ORManager withDataSource(DataSource dataSource) {
        return new ORManagerImpl(dataSource);
    }

}