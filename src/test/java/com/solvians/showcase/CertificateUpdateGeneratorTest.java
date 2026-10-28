package com.solvians.showcase;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateUpdateGeneratorTest {

    @Test
    public void generateQuotes() {
        CertificateUpdateGenerator certificateUpdateGenerator = new CertificateUpdateGenerator(10, 100);
        Stream<CertificateUpdate> quotes = certificateUpdateGenerator.generateQuotes();
        assertNotNull(quotes);
        assertEquals(10 * 100, quotes.count());
    }

    @Test
    void generatesTheRequestedNumberOfValidLines() {
        List<String> lines = new CertificateUpdateGenerator(4, 250).generateLines();

        assertEquals(1_000, lines.size());
        for (String line : lines) {
            String[] fields = line.split(",");
            assertEquals(6, fields.length, line);
            assertTrue(IsinGenerator.isValid(fields[1]), line);
        }
    }

    @Test
    void zeroQuotesGivesAnEmptyResult() {
        assertTrue(new CertificateUpdateGenerator(3, 0).generateLines().isEmpty());
    }

    @Test
    void worksWithMoreThreadsThanTasks() {
        assertEquals(8, new CertificateUpdateGenerator(8, 1).generateLines().size());
    }

    @Test
    void reallyUsesMoreThanOneThread() {
        Set<String> workerThreads = ConcurrentHashMap.newKeySet();
        CertificateUpdateFactory recordingFactory = new CertificateUpdateFactory(
                new IsinGenerator(),
                () -> {
                    workerThreads.add(Thread.currentThread().getName());
                    return java.util.concurrent.ThreadLocalRandom.current();
                },
                () -> {
                    try {
                        Thread.sleep(2); // keep each task busy so the pool has to spread the work
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return 0L;
                });

        new CertificateUpdateGenerator(4, 50, recordingFactory).generateLines();

        assertTrue(workerThreads.size() > 1, "expected several worker threads but saw " + new HashSet<>(workerThreads));
    }

    @Test
    void failureInATaskIsReportedWithItsCause() {
        CertificateUpdateFactory broken = new CertificateUpdateFactory(
                new IsinGenerator(), java.util.concurrent.ThreadLocalRandom::current,
                () -> {
                    throw new IllegalStateException("clock broke");
                });

        IllegalStateException e = assertThrows(IllegalStateException.class,
                () -> new CertificateUpdateGenerator(2, 2, broken).generateLines());
        assertEquals("clock broke", e.getCause().getMessage());
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new CertificateUpdateGenerator(0, 10));
        assertThrows(IllegalArgumentException.class, () -> new CertificateUpdateGenerator(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> new CertificateUpdateGenerator(2, -1));
    }
}
