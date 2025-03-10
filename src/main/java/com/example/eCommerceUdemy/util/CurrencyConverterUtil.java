package com.example.eCommerceUdemy.util;

public class CurrencyConverterUtil {
    private static final double EXCHANGE_RATE = 25000.0; // Example: 1 USD = 25,000 VND

    public static long convertUsdToVnd(double usdAmount) {
        if (usdAmount < 0) {
            throw new IllegalArgumentException("Amount cannot be negative");
        }
        return Math.round(usdAmount * EXCHANGE_RATE); // Convert & round to nearest VND
    }
}
