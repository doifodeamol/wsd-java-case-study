package com.solvians.showcase;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.LongSupplier;
import java.util.function.Supplier;

/**
 * Creates random {@link CertificateUpdate}s that respect the ranges of the specification.
 *
 * <p>Stateless and therefore safe to share between threads. Everything non-deterministic (random numbers,
 * the clock) is injected, which is what makes it trivial to unit test.
 */
public final class CertificateUpdateFactory {

    static final int MIN_PRICE_CENTS = 10_000;   // 100.00
    static final int MAX_PRICE_CENTS = 20_000;   // 200.00
    static final int MIN_BID_SIZE = 1_000;
    static final int MAX_BID_SIZE = 5_000;
    static final int MIN_ASK_SIZE = 1_000;
    static final int MAX_ASK_SIZE = 10_000;

    private final IsinGenerator isinGenerator;
    private final Supplier<? extends Random> randomSupplier;
    private final LongSupplier clock;

    public CertificateUpdateFactory() {
        this(new IsinGenerator(), ThreadLocalRandom::current, System::currentTimeMillis);
    }

    public CertificateUpdateFactory(IsinGenerator isinGenerator,
                                    Supplier<? extends Random> randomSupplier,
                                    LongSupplier clock) {
        this.isinGenerator = Objects.requireNonNull(isinGenerator, "isinGenerator");
        this.randomSupplier = Objects.requireNonNull(randomSupplier, "randomSupplier");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    public CertificateUpdate create() {
        Random random = randomSupplier.get(); // resolved on the calling thread, see IsinGenerator
        return new CertificateUpdate(
                clock.getAsLong(),
                isinGenerator.generate(),
                randomPrice(random),
                randomInclusive(random, MIN_BID_SIZE, MAX_BID_SIZE),
                randomPrice(random),
                randomInclusive(random, MIN_ASK_SIZE, MAX_ASK_SIZE));
    }

    /** Works in whole cents so the price is exact and always has exactly two decimals. */
    private static BigDecimal randomPrice(Random random) {
        return BigDecimal.valueOf(randomInclusive(random, MIN_PRICE_CENTS, MAX_PRICE_CENTS), 2);
    }

    /** {@code Random.nextInt(bound)} is exclusive, the specification's ranges are inclusive. */
    private static int randomInclusive(Random random, int min, int max) {
        return min + random.nextInt(max - min + 1);
    }
}
