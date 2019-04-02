/*
JAK TO DODAĆ:

> IntelliJ
> włączyć Klasę Swapper
> Ctrl + T -> Create New Tests
> Testing Library -> JUnit4 (zainstallować jeżeli trzeba)
> Wkleić to do nowego pliku
> Jeżeli coś się podświetla na czerwono, to dodać JUnit4 do Classpath (na przykład jak się już wklei, to najechać na
  >>import static org.junit.Assert.*;<< -> kliknąć -> Alt+Enter -> Wybrać Add to Classpath

            ***ENJOY***
 */

package swapperTests;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import swapper.Swapper;

import java.util.*;
//import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
//import java.util.concurrent.CyclicBarrier;

import static org.junit.Assert.*;

public class SwapperTest {

    private Swapper<Integer> swapper;
    private List<Integer> emptySet = Collections.emptyList();

    @Before
    public void setUp() {
        swapper = new Swapper<>();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    private List<Integer> getAndSort(Swapper swapper) {
        List<Integer> l = new ArrayList<>(swapper.getValueSet());
        Collections.sort(l);
        return l;
    }

    @Test
    public void emptyArguments() throws InterruptedException {
        swapper.swap(Collections.emptySortedSet(), Collections.emptyList());
        assertEquals(Collections.emptyList(), getAndSort(swapper));
    }

    @Test
    public void babySteps() throws InterruptedException {
        swapper.swap(emptySet, Collections.singletonList(1));
        assertEquals(Collections.singletonList(1), getAndSort(swapper));
    }

    @Test
    public void babySteps2() throws InterruptedException {
        swapper.swap(emptySet, Collections.singletonList(1));
        assertEquals(Collections.singletonList(1), getAndSort(swapper));
        swapper.swap(Collections.singletonList(1), emptySet);
        assertEquals(emptySet, getAndSort(swapper));
    }



    @Test
    public void add() throws InterruptedException {
        swapper.swap(emptySet, Collections.singleton(1));
        assertEquals(Collections.singletonList(1), getAndSort(swapper));
        swapper.swap(emptySet, Arrays.asList(29, 42, 76));
        assertEquals(Arrays.asList(1, 29, 42, 76), getAndSort(swapper));
        swapper.swap(emptySet, Collections.singleton(29));
        assertEquals(Arrays.asList(1, 29, 42, 76), getAndSort(swapper));
        swapper.swap(emptySet, Arrays.asList(76, 28, 42, 1, 28, 29, 42, 76));
        assertEquals(Arrays.asList(1, 28, 29, 42, 76), getAndSort(swapper));
        swapper.swap(emptySet, Arrays.asList(666, 666, 666, 666, 666, 666, 666, 666, 666));
        assertEquals(Arrays.asList(1, 28, 29, 42, 76, 666), getAndSort(swapper));
    }

    @Test
    public void addRemove() throws InterruptedException {
        swapper.swap(emptySet, Set.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10), getAndSort(swapper));
        swapper.swap(Arrays.asList(2), emptySet);
        assertEquals(Arrays.asList(1, 3, 4, 5, 6, 7, 8, 9, 10), getAndSort(swapper));
        swapper.swap(Arrays.asList(1, 3, 5), emptySet);
        assertEquals(Arrays.asList(4, 6, 7, 8, 9, 10), getAndSort(swapper));
        swapper.swap(Arrays.asList(7, 8, 9, 10), Arrays.asList(7, 8, 9, 10));
        assertEquals(Arrays.asList(4, 6, 7, 8, 9, 10), getAndSort(swapper));
    }

    @Test
    public void removeNonExistent() throws InterruptedException {
        Thread remThr = new Thread(() -> {
            try {
                swapper.swap(Collections.singleton(1), emptySet);
                exception.expect(InterruptedException.class);
            } catch (InterruptedException e) {}
        });
        remThr.start();
        Thread.sleep(1000);
        remThr.interrupt();
    }

    private class Swaping implements Runnable {
        private final Collection<Integer> add;
        private final Collection<Integer> rem;
        private final CountDownLatch latch;
        private final int n;

        public Swaping(Collection<Integer> rem, Collection<Integer> add, CountDownLatch latch, int n) {
            this.add = add;
            this.rem = rem;
            this.latch = latch;
            this.n = n;
        }

        @Override
        public void run() {
            try {
                for (int i = 0; i < n; i++) {
                    swapper.swap(rem, add);
                }
                latch.countDown();
            } catch (InterruptedException e) {
                fail();
            }
        }
    }

    @Test
    public void twoThousandTimes() throws InterruptedException {
        final int TEST_REPEATS = 100;
        final int THREAD_REPEATS = 1000;

        CountDownLatch latch;
        swapper.swap(emptySet, Collections.singleton(1));
        for(int i = 0; i < TEST_REPEATS; i++) {
            latch = new CountDownLatch(2);
            Thread t1 = new Thread(new Swaping(Collections.singleton(1), Collections.singleton(2), latch, THREAD_REPEATS));
            Thread t2 = new Thread(new Swaping(Collections.singleton(2), Collections.singleton(1), latch, THREAD_REPEATS));
            t1.start();
            t2.start();
            latch.await(10, TimeUnit.SECONDS);
            Thread.sleep(50);
            if (t1.isAlive() || t2.isAlive()) {
                fail("Took too long");
            }

            assertEquals(Collections.singletonList(1), getAndSort(swapper));
        }
    }

    @Test
    public void twoThousandThreads() throws InterruptedException {
        final int TEST_REPEATS = 1;
        final int THREADS = 100;   // NEEDS TO BE EVEN

        CountDownLatch latch;
        swapper.swap(emptySet, Collections.singleton(1));
        for (int i = 0; i < TEST_REPEATS; i++) {
            latch = new CountDownLatch(THREADS);
            for(int j = 0; j < THREADS / 2; j++) {
                Thread t1 = new Thread(new Swaping(Collections.singleton(1), Collections.singleton(2), latch, 1));
                Thread t2 = new Thread(new Swaping(Collections.singleton(2), Collections.singleton(1), latch, 1));
                t1.start();
//                t1.interrupt();
                t2.start();
            }
            latch.await();

            assertEquals(Collections.singletonList(1), getAndSort(swapper));
        }
    }

    @Test
    public void twoThousandThreads2() throws InterruptedException {
        final int TEST_REPEATS = 1;
        final int THREADS = 100;

        CountDownLatch latch;
        swapper.swap(emptySet, Collections.singleton(0));
        for (int i = 0; i < TEST_REPEATS; i++) {
            latch = new CountDownLatch(THREADS);
            for(int j = 0; j < THREADS; j++) {
                Thread t = new Thread(new Swaping(Collections.singleton(j), Collections.singleton(j + 1), latch, 1));
                t.start();
            }
            latch.await();
            assertEquals(Collections.singletonList(THREADS), getAndSort(swapper));
            swapper.swap(Collections.singletonList(THREADS), Collections.singletonList(0));
        }
    }

//    private class SwapingCascade implements Runnable{
//
//        private final Collection<Integer> add;
//        private final Collection<Integer> rem;
//        private final CyclicBarrier barrier;
//        private final int n;
//
//        public SwapingCascade(Collection<Integer> add, Collection<Integer> rem, CyclicBarrier barrier, int n) {
//            this.add = add;
//            this.rem = rem;
//            this.barrier = barrier;
//            this.n = n;
//        }
//
//        @Override
//        public void run() {
//            try {
//                for (int i = 0; i < n; i++) {
//                    barrier.await();
//                    swapper.swap(rem, add);
//                }
//            } catch (InterruptedException | BrokenBarrierException e) {
//                fail();
//            }
//        }
//    }

    // tu coś nie działa, ale może wymyśli się
//    @Test
//    public void cascadingThreads() throws  InterruptedException {
//       final int TEST_REPEATS = 1;
//       final int THREADS = 2000;    // NEEDS TO BE EVEN
//
//       CountDownLatch latch = new CountDownLatch(TEST_REPEATS);
//       CyclicBarrier barrier = new CyclicBarrier(THREADS, () -> {
//           assertEquals(Collections.emptyList(), getAndSort(swapper));
//           latch.countDown();
//       });
//
//       for (int i = 0; i < THREADS / 2; i++) {
//           Thread t1 = new Thread(new SwapingCascade(Collections.singleton(i), emptySet, barrier, TEST_REPEATS));
//           Thread t2 = new Thread(new SwapingCascade(emptySet, Collections.singleton(i), barrier, TEST_REPEATS));
//           t1.start();
//           t2.start();
//       }
//       latch.await();
//    }

//   @Test
//    public void () throws  InterruptedException {
//    }

}