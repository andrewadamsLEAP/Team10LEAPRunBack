package com.example.services;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MarketSymbolMapper {

    // =========================================================
    // Crypto
    // =========================================================

    public boolean isCryptoTicker(
            String ticker) {

        return ticker != null
                && ticker.contains("-");
    }

    public String toAlpacaCryptoSymbol(
            String ticker) {

        if (ticker == null) {
            return null;
        }

        return ticker.replace("-", "/");
    }

    public String toAlpacaCryptoSymbols(
            List<String> symbols) {

        return symbols.stream()
                .map(this::toAlpacaCryptoSymbol)
                .collect(Collectors.joining(","));
    }

    // =========================================================
    // Forex
    // =========================================================

    public boolean isForexTicker(
            String ticker) {

        return ticker != null
                && ticker.length() == 6
                && !ticker.contains("-");
    }

    public String toAlpacaForexSymbol(
            String ticker) {

        if (ticker == null) {
            return null;
        }

        if (ticker.contains("/")) {
            return ticker;
        }

        if (ticker.length() != 6) {

            throw new IllegalArgumentException(
                    "Invalid forex ticker: " + ticker);
        }

        return ticker.substring(0, 3)
                + "/"
                + ticker.substring(3);
    }

    public String toAlpacaForexSymbols(
            List<String> symbols) {

        return symbols.stream()
                .map(this::toAlpacaForexSymbol)
                .collect(Collectors.joining(","));
    }
}