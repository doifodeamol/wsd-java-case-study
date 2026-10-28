package com.solvians.showcase;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * Runs the certificate update generation on a pool of worker threads.
 *
 * <p>Every one of the {@code threads} workers is responsible for {@code quotes} updates, so a run
 * produces {@code threads * quotes} updates in total.
 */
public class CertificateUpdateGenerator {

    private final int threads;
    private final int quotes;
    private final CertificateUpdateFactory factory;

    public CertificateUpdateGenerator(int threads, int quotes) {
        this(threads, quotes, new CertificateUpdateFactory());
    }

    public CertificateUpdateGenerator(int threads, int quotes, CertificateUpdateFactory factory) {
        if (threads < 1) {
            throw new IllegalArgumentException("Number of threads must be at least 1 but was " + threads);
        }
        if (quotes < 0) {
            throw new IllegalArgumentException("Number of quotes must not be negative but was " + quotes);
        }
        this.threads = threads;
        this.quotes = quotes;
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    /** @return the generated updates as feed lines, e.g. {@code 1352122280502,DE1234567896,101.23,1000,103.45,1000}. */
    public List<String> generateLines() {
        return this.<String>runAll(() -> new CertificateUpdateTask(factory));
    }

    /** @return the generated updates as objects. */
    public Stream<CertificateUpdate> generateQuotes() {
        return this.<CertificateUpdate>runAll(() -> factory::create).stream();
    }

    /**
     * Submits {@code threads * quotes} tasks to a fixed pool and waits for all of them.
     * Results come back in submission order, independent of which thread finished first.
     */
    private <T> List<T> runAll(Supplier<Callable<T>> taskFactory) {
        int totalTasks = Math.multiplyExact(threads, quotes);
        List<Callable<T>> tasks = new ArrayList<>(totalTasks);
        for (int i = 0; i < totalTasks; i++) {
            tasks.add(taskFactory.get());
        }

        ExecutorService executor = Executors.newFixedThreadPool(threads);
        try {
            List<Future<T>> futures = executor.invokeAll(tasks);
            List<T> results = new ArrayList<>(totalTasks);
            for (Future<T> future : futures) {
                results.add(future.get());
            }
            return results;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt(); // never swallow an interrupt
            throw new IllegalStateException("Interrupted while generating certificate updates", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException("Generating a certificate update failed", e.getCause());
        } finally {
            executor.shutdownNow();
        }
    }
}
