package dev.prasadgaikwad.springaiapps.observability;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.stereotype.Service;

/**
 * Single-writer asynchronous sink for DuckDB trace spans.
 *
 * <p>Callers build and enqueue a row without blocking on JDBC; {@link #flush()} drains
 * queued writes through a barrier so REST responses and tests can observe committed rows
 * deterministically. Retention runs at startup and after each flush.
 */
@Service
class TraceSpanSink implements DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(TraceSpanSink.class);

    private final TraceSpanRepository repository;
    private final int retentionDays;
    private final ExecutorService writer = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "trace-span-writer");
        thread.setDaemon(true);
        return thread;
    });
    private final AtomicReference<Throwable> lastFailure = new AtomicReference<>();

    TraceSpanSink(TraceSpanRepository repository, TraceProperties properties) {
        this.repository = repository;
        this.retentionDays = Math.max(1, properties.retentionDays());
    }

    @PostConstruct
    void pruneOnStartup() {
        prune();
    }

    /** Builds the span without blocking and queues its insert. */
    TraceSpan write(String id, String traceId, String parentSpanId, String name, String kind,
                    Instant startTime, Instant endTime, Map<String, String> tags, String status) {
        TraceSpan span = this.repository.toSpan(id, traceId, parentSpanId, name, kind, startTime, endTime, tags, status);
        enqueue(() -> this.repository.persist(span));
        return span;
    }

    /** Waits for already-queued writes, applies retention, and surfaces the first async failure. */
    void flush() {
        CountDownLatch drained = new CountDownLatch(1);
        enqueue(drained::countDown);
        try {
            if (!drained.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting for trace span writes");
            }
        }
        catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted waiting for trace span writes", ex);
        }
        prune();
        Throwable failure = this.lastFailure.getAndSet(null);
        if (failure instanceof RuntimeException runtimeFailure) {
            throw runtimeFailure;
        }
        if (failure != null) {
            throw new IllegalStateException("Trace span write failed", failure);
        }
    }

    private void enqueue(Runnable task) {
        this.writer.execute(() -> {
            try {
                task.run();
            }
            catch (RuntimeException ex) {
                this.lastFailure.compareAndSet(null, ex);
                log.warn("Async trace span write failed", ex);
            }
        });
    }

    private void prune() {
        this.repository.deleteOlderThan(Instant.now().minus(Duration.ofDays(this.retentionDays)));
    }

    @Override
    public void destroy() {
        try {
            flush();
        }
        finally {
            this.writer.shutdown();
            try {
                if (!this.writer.awaitTermination(5, TimeUnit.SECONDS)) {
                    this.writer.shutdownNow();
                }
            }
            catch (InterruptedException ex) {
                this.writer.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }
}
