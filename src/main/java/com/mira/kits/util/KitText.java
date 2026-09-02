package com.mira.kits.util;

import java.math.BigDecimal;
import java.util.Locale;

public final class KitText {
    private KitText() {
    }

    public static String normalizeId(String input) {
        if (input == null) return "";
        String normalized = input.trim().toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized;
    }

    public static String prettyId(String id) {
        if (id == null || id.isBlank()) return "Kit";
        String[] words = id.toLowerCase(Locale.ROOT).split("[_\\s-]+");
        StringBuilder out = new StringBuilder();
        for (String word : words) {
            if (word.isBlank()) continue;
            if (!out.isEmpty()) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    public static String roman(int value) {
        if (value <= 0) return Integer.toString(value);
        if (value > 3999) return Integer.toString(value);
        int[] values = {1000, 900, 500, 400, 100, 90, 50, 40, 10, 9, 5, 4, 1};
        String[] numerals = {"M", "CM", "D", "CD", "C", "XC", "L", "XL", "X", "IX", "V", "IV", "I"};
        StringBuilder result = new StringBuilder();
        int remaining = value;
        for (int i = 0; i < values.length; i++) {
            while (remaining >= values[i]) {
                remaining -= values[i];
                result.append(numerals[i]);
            }
        }
        return result.toString();
    }

    public static String duration(long millis) {
        if (millis <= 0) return "Ready";
        long seconds = (millis + 999L) / 1000L;
        long days = seconds / 86400L;
        seconds %= 86400L;
        long hours = seconds / 3600L;
        seconds %= 3600L;
        long minutes = seconds / 60L;
        seconds %= 60L;
        if (days > 0) return days + "d " + hours + "h";
        if (hours > 0) return hours + "h " + minutes + "m";
        if (minutes > 0) return minutes + "m " + seconds + "s";
        return seconds + "s";
    }

    public static String money(BigDecimal amount, String symbol) {
        BigDecimal safe = amount == null ? BigDecimal.ZERO : amount.max(BigDecimal.ZERO);
        String prefix = symbol == null || symbol.isBlank() ? "$" : symbol;
        return prefix + safe.stripTrailingZeros().toPlainString();
    }
}
