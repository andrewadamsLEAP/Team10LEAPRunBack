package com.example.generalServices;

import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Service
public class MarketHoursService {

    private static final ZoneId US_EASTERN =
            ZoneId.of("America/New_York");

    private static final LocalTime MARKET_OPEN =
            LocalTime.of(9, 30);

    private static final LocalTime MARKET_CLOSE =
            LocalTime.of(16, 0);

    public boolean isUsMarketHours() {

        OffsetDateTime easternNow =
                OffsetDateTime.now(
                        US_EASTERN);

        DayOfWeek day =
                easternNow.getDayOfWeek();

        LocalTime time =
                easternNow.toLocalTime();

        return day != DayOfWeek.SATURDAY
                && day != DayOfWeek.SUNDAY
                && !time.isBefore(MARKET_OPEN)
                && time.isBefore(MARKET_CLOSE);
    }
}