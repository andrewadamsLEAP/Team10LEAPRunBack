package com.example.mappers;

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

    public String toDatabaseCryptoSymbol(
            String ticker) {

        if (ticker == null) {
            return null;
        }

        return ticker.replace("/", "-");
    }

    // =========================================================
    // Forex
    // =========================================================

    public boolean isForexTicker(
            String ticker) {

        return ticker != null
            && (ticker.matches("[A-Za-z]{6}")
            || ticker.matches("[A-Za-z]{3}-[A-Za-z]{3}"));
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

    public String toDatabaseForexSymbol(
            String ticker) {

        if (ticker == null) {
            return null;
        }

        return ticker.replace("/", "");
    }

    public String toFrankfurterBase(String ticker) {
        String normalized = normalizeForexTicker(ticker);
        return normalized.substring(0, 3);
    }

    public String toFrankfurterQuote(String ticker) {
        String normalized = normalizeForexTicker(ticker);
        return normalized.substring(3);
    }

    private String normalizeForexTicker(String ticker) {
        if (ticker == null) {
            throw new IllegalArgumentException("Forex ticker cannot be null");
        }

        String normalized = ticker.replace("-", "").replace("/", "").toUpperCase();
        if (!normalized.matches("[A-Z]{6}")) {
            throw new IllegalArgumentException("Invalid forex ticker: " + ticker);
        }

        return normalized;
    }
}