package com.solvians.showcase;

import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;

/**
 * Generates random, structurally valid ISINs and knows how to compute / verify the check digit.
 *
 * <p>An ISIN has 12 characters: {@code 2 uppercase letters + 9 uppercase alphanumerics + 1 check digit}.
 * The check digit is the Luhn digit of the first 11 characters once letters are expanded to numbers
 * ({@code A=10 ... Z=35}).
 *
 * <p><b>Thread safety:</b> instances are immutable and stateless. The random source is looked up on
 * every call through a {@link Supplier}, so the default {@code ThreadLocalRandom::current} is always
 * resolved on the calling thread (a {@link ThreadLocalRandom} must never be shared between threads).
 */
public final class IsinGenerator {

    public static final int ISIN_LENGTH = 12;
    private static final int BODY_LENGTH = ISIN_LENGTH - 1;
    private static final int COUNTRY_PREFIX_LENGTH = 2;

    private static final String LETTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final String ALPHANUMERICS = "0123456789" + LETTERS;

    private final Supplier<? extends Random> randomSupplier;

    /** Uses {@link ThreadLocalRandom} - fast, contention free and what you want in production. */
    public IsinGenerator() {
        this(ThreadLocalRandom::current);
    }

    /** Lets tests inject a seeded {@link Random} to get reproducible ISINs. */
    public IsinGenerator(Supplier<? extends Random> randomSupplier) {
        this.randomSupplier = Objects.requireNonNull(randomSupplier, "randomSupplier");
    }

    /** @return a new random ISIN, e.g. {@code DE1234567896}. */
    public String generate() {
        Random random = randomSupplier.get();
        StringBuilder isin = new StringBuilder(ISIN_LENGTH);
        for (int i = 0; i < COUNTRY_PREFIX_LENGTH; i++) {
            isin.append(LETTERS.charAt(random.nextInt(LETTERS.length())));
        }
        for (int i = COUNTRY_PREFIX_LENGTH; i < BODY_LENGTH; i++) {
            isin.append(ALPHANUMERICS.charAt(random.nextInt(ALPHANUMERICS.length())));
        }
        return isin.append(calculateCheckDigit(isin)).toString();
    }

    /**
     * Calculates the check digit for the first 11 characters of an ISIN.
     *
     * @param body 11 characters, uppercase letters and digits only
     * @return the check digit, 0-9
     * @throws IllegalArgumentException if the body is not exactly 11 uppercase alphanumerics
     */
    public static int calculateCheckDigit(CharSequence body) {
        Objects.requireNonNull(body, "body");
        if (body.length() != BODY_LENGTH) {
            throw new IllegalArgumentException(
                    "ISIN body must have " + BODY_LENGTH + " characters but had " + body.length() + ": " + body);
        }

        // Step 1: expand letters into numbers (A=10 ... Z=35) -> every character becomes 1 or 2 digits.
        StringBuilder digits = new StringBuilder(BODY_LENGTH * 2);
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (c >= '0' && c <= '9') {
                digits.append(c);
            } else if (c >= 'A' && c <= 'Z') {
                digits.append(c - 'A' + 10);
            } else {
                throw new IllegalArgumentException("Invalid ISIN character '" + c + "' in: " + body);
            }
        }

        // Steps 2+3: from the right, double every other digit (starting with the rightmost),
        // and add up the individual digits of every result (14 -> 1 + 4).
        int sum = 0;
        boolean doubleIt = true;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int value = digits.charAt(i) - '0';
            if (doubleIt) {
                value *= 2;
            }
            sum += value / 10 + value % 10;
            doubleIt = !doubleIt;
        }

        // Step 4: distance to the next multiple of 10. The extra "% 10" makes a sum that already is a
        // multiple of 10 produce 0 instead of 10.
        return (10 - sum % 10) % 10;
    }

    /** @return {@code true} if the value has the ISIN shape and a correct check digit. */
    public static boolean isValid(String isin) {
        if (isin == null || isin.length() != ISIN_LENGTH) {
            return false;
        }
        for (int i = 0; i < COUNTRY_PREFIX_LENGTH; i++) {
            char c = isin.charAt(i);
            if (c < 'A' || c > 'Z') {
                return false;
            }
        }
        char last = isin.charAt(ISIN_LENGTH - 1);
        if (last < '0' || last > '9') {
            return false;
        }
        try {
            return calculateCheckDigit(isin.subSequence(0, BODY_LENGTH)) == last - '0';
        } catch (IllegalArgumentException invalidCharacter) {
            return false;
        }
    }
}
