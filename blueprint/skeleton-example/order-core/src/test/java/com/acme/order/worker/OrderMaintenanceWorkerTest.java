package com.acme.order.worker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.acme.order.config.ResilienceConfig;
import com.acme.order.repository.OrderRepository;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Spring Framework 7 dayaniklilik kaniti (referans Bolum 4.7, 2.1): @EnableResilientMethods altinda
 * @Retryable gecici hatayi toplam 3 denemeye kadar tekrarlar (maxRetries=2), kalici hatayi tekrarlamaz;
 * @ConcurrencyLimit(1) es zamanli cagrilari seri hale getirir (gozlenen maksimum es zamanlilik == 1).
 * Retry gecikmesi property ile 1ms'ye cekilir; beklemek icin sleep yok - bekleyen thread'ler durumlarindan izlenir.
 */
@SpringBootTest(classes = {ResilienceConfig.class, OrderMaintenanceWorker.class}, properties = "order.worker.retry-delay=1ms")
class OrderMaintenanceWorkerTest {

    @Autowired OrderMaintenanceWorker worker;
    @MockitoBean OrderRepository repo;

    @Test
    void retryable_retriesTransientFailure_untilSuccess_withinThreeAttempts() {
        UUID id = UUID.randomUUID();
        when(repo.updateStatus(id, "CANCELLED"))
                .thenThrow(new ConcurrencyFailureException("could not serialize access (40001)"))
                .thenThrow(new ConcurrencyFailureException("deadlock detected (40P01)"))
                .thenReturn(1);

        assertThat(worker.markCancelled(id)).isEqualTo(1);
        verify(repo, times(3)).updateStatus(id, "CANCELLED");
    }

    @Test
    void retryable_givesUpAfterThreeAttempts_andSurfacesLastTransientFailure() {
        UUID id = UUID.randomUUID();
        when(repo.updateStatus(eq(id), any())).thenThrow(new ConcurrencyFailureException("still deadlocked"));

        Throwable t = catchThrowable(() -> worker.markCancelled(id));

        assertThat(t).isNotNull();
        assertThat(rootCause(t)).isInstanceOf(ConcurrencyFailureException.class).hasMessage("still deadlocked");
        verify(repo, times(3)).updateStatus(id, "CANCELLED");
    }

    @Test
    void retryable_doesNotRetryPermanentFailure() {
        UUID id = UUID.randomUUID();
        when(repo.updateStatus(eq(id), any())).thenThrow(new DataIntegrityViolationException("check constraint"));

        Throwable t = catchThrowable(() -> worker.markCancelled(id));

        assertThat(t).isInstanceOf(DataIntegrityViolationException.class);
        verify(repo, times(1)).updateStatus(id, "CANCELLED");
    }

    @Test
    void concurrencyLimitOne_serializesConcurrentCalls() throws Exception {
        int callers = 4;
        AtomicInteger inFlight = new AtomicInteger();
        AtomicInteger maxInFlight = new AtomicInteger();
        CountDownLatch firstEntered = new CountDownLatch(1);
        CountDownLatch gate = new CountDownLatch(1);
        when(repo.findAll()).thenAnswer(inv -> {
            int now = inFlight.incrementAndGet();
            maxInFlight.accumulateAndGet(now, Math::max);
            firstEntered.countDown();
            gate.await(30, TimeUnit.SECONDS);          // ilk cagiran icerde tutulur; digerleri semaphore'da beklemeli
            inFlight.decrementAndGet();
            return List.of();
        });

        Thread[] threads = new Thread[callers];
        for (int i = 0; i < callers; i++) {
            threads[i] = new Thread(() -> worker.rebuildProjection(), "rebuild-" + i);
        }
        threads[0].start();
        assertThat(firstEntered.await(10, TimeUnit.SECONDS)).isTrue();
        for (int i = 1; i < callers; i++) threads[i].start();

        // Sleep yok: diger thread'ler ya semaphore'da (dogru) ya da mock'un gate'inde (limit yok) bekler; ikisi de
        // WAITING/TIMED_WAITING durumudur. Hepsi bekleme durumuna gecince gozlem kararlidir; sinirli poll (<= 10 sn).
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!allWaiting(threads, 1) && System.nanoTime() < deadline) Thread.onSpinWait();
        assertThat(allWaiting(threads, 1)).as("bekleyen thread'ler").isTrue();
        assertThat(inFlight).as("icerdeki cagri sayisi").hasValue(1);

        gate.countDown();
        for (Thread t : threads) t.join(10_000);
        assertThat(maxInFlight).as("gozlenen maksimum es zamanlilik").hasValue(1);
        verify(repo, times(callers)).findAll();
    }

    static boolean allWaiting(Thread[] threads, int from) {
        for (int i = from; i < threads.length; i++) {
            Thread.State s = threads[i].getState();
            if (s != Thread.State.WAITING && s != Thread.State.TIMED_WAITING && s != Thread.State.TERMINATED) return false;
        }
        return true;
    }

    static Throwable rootCause(Throwable t) {
        Throwable r = t;
        while (r.getCause() != null && r.getCause() != r) r = r.getCause();
        return r;
    }
}
