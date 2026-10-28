package com.solvians.showcase;

import java.io.PrintStream;
import java.util.List;

/**
 * Command line entry point.
 *
 * <pre>
 *   java -cp target/classes com.solvians.showcase.App &lt;numberOfThreads&gt; &lt;updatesPerThread&gt;
 * </pre>
 *
 * Generates {@code threads * updatesPerThread} certificate updates on {@code threads} worker threads
 * and prints one line per update to standard output.
 */
public final class App {

    private App() {
    }

    public static void main(String[] args) {
        run(args, System.out);
    }

    /** Separated from {@link #main} so tests can capture the output instead of reading System.out. */
    static void run(String[] args, PrintStream out) {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException(
                    "Expected two arguments: <numberOfThreads> <numberOfQuotes> but got "
                            + (args == null ? 0 : args.length));
        }
        int threads = Integer.parseInt(args[0]);
        int quotes = Integer.parseInt(args[1]);

        List<String> lines = new CertificateUpdateGenerator(threads, quotes).generateLines();

        StringBuilder output = new StringBuilder(lines.size() * 50);
        for (String line : lines) {
            output.append(line).append(System.lineSeparator());
        }
        out.print(output); // one write instead of millions of small ones
        out.flush();
    }
}
