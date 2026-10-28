package com.solvians.showcase;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class AppTest {
    @Test
    public void expectTwoIntArgs() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            App.main(new String[]{"xxx"});
        });
        NumberFormatException numbers = Assertions.assertThrows(NumberFormatException.class, () -> {
            App.main(new String[]{"xxx", "zzz"});
        });
        numbers = Assertions.assertThrows(NumberFormatException.class, () -> {
            App.main(new String[]{"10", "zzz"});
        });
        assertEquals("For input string: \"zzz\"", numbers.getMessage());
    }

    @Test
    void printsOneValidLinePerCertificateUpdate() {
        String[] lines = runAndCapture("3", "5").split(System.lineSeparator());

        assertEquals(15, lines.length);
        for (String line : lines) {
            assertEquals(6, line.split(",").length, line);
            assertTrue(IsinGenerator.isValid(line.split(",")[1]), line);
        }
    }

    @Test
    void mainRunsSuccessfullyWithValidArguments() {
        PrintStream original = System.out;
        System.setOut(new PrintStream(new ByteArrayOutputStream()));
        try {
            App.main(new String[]{"2", "3"}); // must not throw
        } finally {
            System.setOut(original);
        }
    }

    @Test
    void noArgumentsIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> App.main(new String[0]));
    }

    @Test
    void nonPositiveThreadCountIsRejected() {
        assertThrows(IllegalArgumentException.class, () -> App.main(new String[]{"0", "5"}));
    }

    private static String runAndCapture(String... args) {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        App.run(args, new PrintStream(buffer, true, StandardCharsets.UTF_8));
        return buffer.toString(StandardCharsets.UTF_8);
    }
}
