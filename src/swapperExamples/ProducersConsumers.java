package swapperExamples;

import swapper.Swapper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProducersConsumers {

    private static final Swapper<Integer> swapper = new Swapper<>();

    private static final int BUFFER_SIZE = 10;
    private static final int PRODUCERS = 10;
    private static final int CONSUMERS = 10;
    private static final int PRODUCED = 10;
    private static final int CONSUMED = PRODUCERS * PRODUCED / CONSUMERS;

    private static Map<Integer, List<Integer>> consumersResults = new HashMap<>();


    private static void initResults() {
        for (int i = 0; i < CONSUMERS; i++) {
            consumersResults.put(i, new ArrayList<>());
        }
    }

    private static class Producer implements Runnable {

        private final int product;

        Producer(int product) {
            this.product = product;
        }

        @Override
        public void run() {
            for (int i = 0; i < PRODUCED; i++) {
                swapper.setMutex(true);
                try {
                    System.out.println(">> Enter producer");
                    if (swapper.collectionSize() == BUFFER_SIZE) {
                        System.out.println("BUFFER FULL");
                        swapper.waitFull();
                    }
                    System.out.println("> Producer adds " + product + " to " + swapper.getValueSet());
                    swapper.swap(Collections.emptySet(), Collections.singleton(product));

                    if (swapper.collectionSize() == 1) {
                        swapper.signalEmpty();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println(Thread.currentThread().getName() + " interrupted");
                } finally {
                    swapper.setMutex(false);
                }
            }
        }
    }

    private static class Consumer implements Runnable {

        private final int id;
        private final int product;

        Consumer(int id, int product) {
            this.id = id;
            this.product = product;
        }

        @Override
        public void run() {
            for (int i = 0; i < CONSUMED; i++) {
                swapper.setMutex(true);
                try {
                    System.out.println(">> Enter consumer");
                    if (swapper.collectionSize() == 0) {
                        System.out.println("Buffer empty");
                        swapper.waitEmpty();
                    }
                    System.out.println("< Consumer takes " + product + " from " + swapper.getValueSet());
                    swapper.swap(Collections.singleton(product), Collections.emptySet());
                    consumersResults.get(id).add(product);
                    if (swapper.collectionSize() == BUFFER_SIZE - 1) {
                        swapper.signalFull();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    System.err.println(Thread.currentThread().getName() + " interrupted");
                } finally {
                    swapper.setMutex(false);
                }
            }
        }
    }

    public static void main(String[] args) {

        /*
        Konsumenci pobierają dokładnie tyle, ile wyprodują producenci
        Kolekcja Swappera służy jako buffer
        */

        List<Thread> producers = new ArrayList<>();
        List<Thread> consumers = new ArrayList<>();
        initResults();
        for (int i = 0; i < PRODUCERS; i++) {
            producers.add(new Thread(new Producer(i)));
        }
        for (int i = 0; i < CONSUMERS; i++) {
            consumers.add(new Thread(new Consumer(i, i)));
        }
        for (Thread t : producers) {
            t.start();
        }
        for (Thread t : consumers) {
            t.start();
        }

        try {
            for (Thread t : producers) {
                t.join();
            }
            for (Thread t : consumers) {
                t.join();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            System.err.println(Thread.currentThread().getName() + " interrupted");
        }

        for (int i = 0; i < CONSUMERS; i++) {
            System.out.println("Consumer " + i + " collected " + consumersResults.get(i));
        }
    }
}
