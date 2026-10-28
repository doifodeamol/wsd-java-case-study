package com.solvians.showcase;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Unit of work for the thread pool: produces <b>one</b> line of certificate update, e.g.
 * {@code 1352122280502,DE1234567896,101.23,1000,103.45,1000}.
 */
public final class CertificateUpdateTask implements Callable<String> {

    private final CertificateUpdateFactory factory;

    public CertificateUpdateTask() {
        this(new CertificateUpdateFactory());
    }

    public CertificateUpdateTask(CertificateUpdateFactory factory) {
        this.factory = Objects.requireNonNull(factory, "factory");
    }

    @Override
    public String call() {
        return factory.create().toLine();
    }
}
