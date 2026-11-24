package dev.rental.photorent;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneOffset;

// в тестах "сегодня" всегда одно и то же, чтобы результат не зависел от дня запуска
@TestConfiguration
public class FixedClockConfig {

    public static final LocalDate TODAY = LocalDate.of(2026, 11, 18);

    @Bean
    public Clock clock() {
        return Clock.fixed(TODAY.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }
}
