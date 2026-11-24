package dev.rental.photorent.concurrent;

import dev.rental.photorent.model.Rental;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RentalOverdueProcessor {

    private static final Logger log = LoggerFactory.getLogger(RentalOverdueProcessor.class);

    private final int threadCount;

    public RentalOverdueProcessor(@Value("${rental.overdue.threads:4}") int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("threadCount must be at least 1");
        }
        this.threadCount = threadCount;
    }

    public List<Rental> findOverdueRentals(List<Rental> activeRentals, LocalDate today) {
        if (activeRentals.isEmpty()) {
            log.info("No active rentals to process");
            return List.of();
        }

        Queue<Rental> overdue = new ConcurrentLinkedQueue<>();
        ExecutorService executorService =
                Executors.newFixedThreadPool(threadCount, new NamedThreadFactory("OverdueChecker"));
        try {
            List<List<Rental>> chunks = splitIntoChunks(activeRentals, threadCount);
            CountDownLatch latch = new CountDownLatch(chunks.size());
            for (List<Rental> chunk : chunks) {
                executorService.submit(() -> {
                    try {
                        checkChunk(chunk, today, overdue);
                    } finally {
                        latch.countDown();
                    }
                });
            }
            awaitCompletion(latch);
        } finally {
            executorService.shutdown();
        }
        return new ArrayList<>(overdue);
    }

    private void checkChunk(List<Rental> chunk, LocalDate today, Queue<Rental> overdue) {
        String threadName = Thread.currentThread().getName();
        log.info("[{}] Started checking chunk of {} rentals", threadName, chunk.size());

        for (Rental rental : chunk) {
            if (rental.isOverdue(today)) {
                overdue.add(rental);
                log.info("[{}] Rental {} is overdue (endDate={})",
                        threadName, rental.getId(), rental.getEndDate());
            }
        }

        log.info("[{}] Finished checking chunk", threadName);
    }

    private List<List<Rental>> splitIntoChunks(List<Rental> rentals, int chunkCount) {
        List<List<Rental>> chunks = new ArrayList<>();
        int size = rentals.size();
        int actualChunkCount = Math.min(chunkCount, size);
        int baseSize = size / actualChunkCount;
        int remainder = size % actualChunkCount;

        int start = 0;
        for (int i = 0; i < actualChunkCount; i++) {
            int end = start + baseSize + (i < remainder ? 1 : 0);
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

