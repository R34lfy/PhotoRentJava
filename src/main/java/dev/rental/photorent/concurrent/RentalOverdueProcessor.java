package dev.rental.photorent.concurrent;

import dev.rental.photorent.model.Rental;
import dev.rental.photorent.model.RentalStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

public class RentalOverdueProcessor {

    private static final Logger log = LoggerFactory.getLogger(RentalOverdueProcessor.class);

    private final int threadCount;

    public RentalOverdueProcessor(int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be at least 1");
        }
        this.threadCount = threadCount;
    }

    public void processOverdueRentals(List<Rental> activeRentals, LocalDate today) {
        if (activeRentals.isEmpty()) {
            log.info("No active rentals to process");
            return;
        }

        ThreadFactory namedThreadFactory = new NamedThreadFactory("OverdueChecker");
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount, namedThreadFactory);

        List<List<Rental>> chunks = splitIntoChunks(activeRentals, threadCount);
        CountDownLatch latch = new CountDownLatch(chunks.size());

        for (List<Rental> chunk : chunks) {
            executorService.submit(() -> {
                try {
                    processChunk(chunk, today);
                } finally {
                    latch.countDown();
                }
            });
        }

        awaitCompletion(latch);
        executorService.shutdown();
    }

    private void processChunk(List<Rental> chunk, LocalDate today) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Started processing chunk of {} rentals", threadName, chunk.size());

        for (Rental rental : chunk) {
            if (rental.isOverdue(today)) {
                rental.setStatus(RentalStatus.OVERDUE);
                log.info("[{}] Rental {} marked as OVERDUE (endDate={})",
                        threadName, rental.getId(), rental.getEndDate());
            }
        }

        log.info("[{}] Finished processing chunk", threadName);
    }

    private List<List<Rental>> splitIntoChunks(List<Rental> rentals, int chunkCount) {
        List<List<Rental>> chunks = new java.util.ArrayList<>();
        int size = rentals.size();
        int actualChunkCount = Math.min(chunkCount, size);
        int baseSize = size / actualChunkCount;
        int remainder = size % actualChunkCount;

        int start = 0;
        for (int i = 0; i < actualChunkCount; i++) {
            int currentChunkSize = baseSize + (i < remainder ? 1 : 0);
            int end = start + currentChunkSize;
            chunks.add(rentals.subList(start, end));
            start = end;
        }
        return chunks;
    }

    private void awaitCompletion(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Processing was interrupted", e);
        }
    }

    private static class NamedThreadFactory implements ThreadFactory {
        private final String prefix;
        private final AtomicInteger counter = new AtomicInteger(1);

        private NamedThreadFactory(String prefix) {
            this.prefix = prefix;
        }

        @Override
        public Thread newThread(Runnable runnable) {
            return new Thread(runnable, prefix + "-" + counter.getAndIncrement());
        }
    }
}

