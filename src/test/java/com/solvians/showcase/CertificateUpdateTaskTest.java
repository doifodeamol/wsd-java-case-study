package com.solvians.showcase;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CertificateUpdateTaskTest {

    /** timestamp,ISIN,bidPrice,bidSize,askPrice,askSize - prices always with exactly 2 decimals. */
    private static final Pattern LINE = Pattern.compile(
            "(\\d+),([A-Z]{2}[A-Z0-9]{9}\\d),(\\d{3}\\.\\d{2}),(\\d+),(\\d{3}\\.\\d{2}),(\\d+)");

    /** A "random" source that always answers with the lowest or the highest value the caller allows. */
    private static final class ExtremeRandom extends Random {
        private static final long serialVersionUID = 1L;

        private final boolean highest;

        ExtremeRandom(boolean highest) {
            this.highest = highest;
        }

        @Override
        public int nextInt(int bound) {
            return highest ? bound - 1 : 0;
        }
    }

    private static CertificateUpdateTask taskWith(boolean highest, long fixedTime) {
        return new CertificateUpdateTask(new CertificateUpdateFactory(
                new IsinGenerator(() -> new ExtremeRandom(highest)),
                () -> new ExtremeRandom(highest),
                () -> fixedTime));
    }

    @Test
    void producesOneLineInTheSpecifiedFormat() throws Exception {
        String line = new CertificateUpdateTask().call();
        Matcher m = LINE.matcher(line);
        assertTrue(m.matches(), "unexpected line: " + line);
        assertTrue(IsinGenerator.isValid(m.group(2)), "invalid ISIN in: " + line);
    }

    @Test
    void usesTheCurrentTimeInMillis() throws Exception {
        long before = System.currentTimeMillis();
        String line = new CertificateUpdateTask().call();
        long after = System.currentTimeMillis();

        long timestamp = Long.parseLong(line.substring(0, line.indexOf(',')));
        assertTrue(timestamp >= before && timestamp <= after, "timestamp " + timestamp + " not in [" + before + "," + after + "]");
    }

    /** Replaces the ISIN of a line with a placeholder so the rest can be compared exactly. */
    private static String replaceIsin(String line) {
        return line.replaceFirst(",[A-Z0-9]{12},", ",ISIN,");
    }

    @Test
    void lowerBoundsAreInclusive() throws Exception {
        assertEquals("1352122280502,ISIN,100.00,1000,100.00,1000", replaceIsin(taskWith(false, 1352122280502L).call()));
    }

    @Test
    void upperBoundsAreInclusive() throws Exception {
        assertEquals("1352122280502,ISIN,200.00,5000,200.00,10000", replaceIsin(taskWith(true, 1352122280502L).call()));
    }

    @Test
    void valuesStayInsideTheirRangesForManyRandomRuns() {
        CertificateUpdateFactory factory = new CertificateUpdateFactory();
        for (int i = 0; i < 50_000; i++) {
            CertificateUpdate u = factory.create();
            assertInRange(u.getBidPrice(), "100.00", "200.00");
            assertInRange(u.getAskPrice(), "100.00", "200.00");
            assertTrue(u.getBidSize() >= 1_000 && u.getBidSize() <= 5_000, "bid size " + u.getBidSize());
            assertTrue(u.getAskSize() >= 1_000 && u.getAskSize() <= 10_000, "ask size " + u.getAskSize());
            assertEquals(2, u.getBidPrice().scale());
            assertEquals(2, u.getAskPrice().scale());
        }
    }

    @Test
    void sameSeedAndClockGiveTheSameLine() throws Exception {
        CertificateUpdateTask a = seeded(7);
        CertificateUpdateTask b = seeded(7);
        assertEquals(a.call(), b.call());
    }

    private static CertificateUpdateTask seeded(long seed) {
        Random random = new Random(seed);
        return new CertificateUpdateTask(new CertificateUpdateFactory(
                new IsinGenerator(() -> random), () -> random, () -> 1L));
    }

    private static void assertInRange(BigDecimal value, String min, String max) {
        assertTrue(value.compareTo(new BigDecimal(min)) >= 0 && value.compareTo(new BigDecimal(max)) <= 0,
                value + " not in [" + min + ", " + max + "]");
    }
}
