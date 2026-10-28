package com.solvians.showcase;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Random;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IsinGeneratorTest {

    private static final Pattern ISIN_SHAPE = Pattern.compile("[A-Z]{2}[A-Z0-9]{9}[0-9]");

    // ---- check digit -------------------------------------------------------------------------------

    @Test
    void checkDigitOfTheExampleInTheSpecification() {
        assertEquals(6, IsinGenerator.calculateCheckDigit("DE123456789"));
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
            "US037833100, 5",   // Apple      US0378331005
            "US594918104, 5",   // Microsoft  US5949181045
            "US023135106, 7",   // Amazon     US0231351067
            "GB000263494, 6",   // BAE        GB0002634946
            "DE000BAY001, 7",   // Bayer      DE000BAY0017
            "DE000716460, 0",   // SAP        DE0007164600 (sum is a multiple of 10 -> 0, not 10)
            "CH001222171, 6",   // ABB        CH0012221716
            "GB000540528, 6",   // HSBC       GB0005405286
            "GB003134865, 8"    // Barclays   GB0031348658
    })
    void checkDigitOfRealWorldIsins(String body, int expected) {
        assertEquals(expected, IsinGenerator.calculateCheckDigit(body));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "DE12345678", "DE1234567890", "de123456789", "DE12345678-", "DE 23456789"})
    void checkDigitRejectsMalformedBodies(String body) {
        assertThrows(IllegalArgumentException.class, () -> IsinGenerator.calculateCheckDigit(body));
    }

    // ---- validation --------------------------------------------------------------------------------

    @ParameterizedTest
    @ValueSource(strings = {"DE1234567896", "US0378331005", "DE0007164600", "GB0002634946"})
    void acceptsValidIsins(String isin) {
        assertTrue(IsinGenerator.isValid(isin));
    }

    @ParameterizedTest
    @ValueSource(strings = {"DE1234567897", "DE123456789", "DE12345678961", "1E1234567896", "de1234567896",
            "DE123456789A", "DE12345-7896", ""})
    void rejectsInvalidIsins(String isin) {
        assertFalse(IsinGenerator.isValid(isin));
    }

    @Test
    void catchesFatFingerTyposInARealIsin() {
        assertTrue(IsinGenerator.isValid("GB0005405286"));   // HSBC, as it should be typed
        assertFalse(IsinGenerator.isValid("GB0005405287"));  // one digit wrong
        assertFalse(IsinGenerator.isValid("GB0005405826"));  // two neighbouring digits swapped
    }

    @Test
    void rejectsNull() {
        assertFalse(IsinGenerator.isValid(null));
    }

    // ---- generator ---------------------------------------------------------------------------------

    @Test
    void generatedIsinsAreAlwaysValid() {
        IsinGenerator generator = new IsinGenerator();
        for (int i = 0; i < 20_000; i++) {
            String isin = generator.generate();
            assertTrue(ISIN_SHAPE.matcher(isin).matches(), "bad shape: " + isin);
            assertTrue(IsinGenerator.isValid(isin), "bad check digit: " + isin);
        }
    }

    @Test
    void sameSeedGivesSameIsin() {
        String first = new IsinGenerator(() -> new Random(42)).generate();
        String second = new IsinGenerator(() -> new Random(42)).generate();
        assertEquals(first, second);
    }

    @Test
    void differentSeedsGiveDifferentIsins() {
        String first = new IsinGenerator(() -> new Random(1)).generate();
        String second = new IsinGenerator(() -> new Random(2)).generate();
        assertFalse(first.equals(second));
    }

    @Test
    void randomSourceIsRequired() {
        assertThrows(NullPointerException.class, () -> new IsinGenerator(null));
    }
}
