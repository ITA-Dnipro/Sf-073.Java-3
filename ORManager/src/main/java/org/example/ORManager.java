package ORManager.src.main.java.org.example;

import ORManager.src.main.java.org.example.annotations.Id;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.UUID;

public interface ORManager {
    // let it work with ids:
    // - Long (autogenerated at DB side)  (HIGH)
    @Id
    UUID uuid = UUID.randomUUID();
    // - String                           (OPTIONAL)
    // The fields may be of types:
    // - int/Integer                      (HIGH)
    // - long/Long                        (HIGH)
    // - double/Double                    (OPTIONAL)
    // - boolean/Boolean                  (OPTIONAL)
    // - String                           (HIGH)
    LocalDate localDate = null;
    LocalTime localTime = null;
    // - LocalDateTime/Instant            (MEDIUM)
    // - BigDecimal                       (OPTIONAL)
    // - Enum +                           (OPTIONAL)
    //   @Enumerated(EnumType.ORDINAL/EnumType.STRING)
    static ORManager withPropertiesFrom(String filename) {
        return null; // todo
    }

    // initialize connection factory for the DB based on the DataSource
    static ORManager withDataSource(DataSource dataSource) {
        return null; // todo
    }
}
