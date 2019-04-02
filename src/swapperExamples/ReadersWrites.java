package swapperExamples;

import swapper.Swapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ReadersWrites {

    private static final int READERS = 10;
    private static final int WRITERS = 10;

    private static final Collection<Integer> reader = Collections.singleton(0);
    private static final Collection<Integer> writer = Collections.singleton(1);
    private static final Collection<Integer> mutex = Collections.singleton(2);
    private static final Collection<Integer> emptySet = Collections.emptySet();

    private static int readers = 0;
    private static int writers = 0;
    private static int waitingReaders = 0;
    private static int waitingWriters = 0;

    private static final Swapper<Integer> swapper = new Swapper<>();

    private static class Reader implements Runnable {

        private final int id;

        public Reader(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                System.out.println("Reader " + id + " tries to enter");

                swapper.swap(mutex, emptySet);
                if (writers + waitingWriters > 0) {
                    waitingReaders++;
                    swapper.swap(emptySet, mutex);
                    swapper.swap(emptySet, reader);
                    waitingReaders--;
                }
                readers++;
                if (waitingReaders > 0) {
                    swapper.swap(reader, emptySet);
                } else {
                    swapper.swap(emptySet, mutex);
                }
                System.out.println("Reader " + id + " is reading");
                swapper.swap(mutex, emptySet);
                readers--;
                if (readers == 0 && waitingWriters > 0) {
                    swapper.swap(writer, emptySet);
                } else {
                    swapper.swap(emptySet, mutex);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private static class Writer implements Runnable {

        private final int id;

        public Writer(int id) {
            this.id = id;
        }

        @Override
        public void run() {
            try {
                System.out.println("Writer " + id + " tries to enter");

                swapper.swap(mutex, emptySet);
                if (writers + readers > 0) {
                    waitingWriters++;
                    swapper.swap(emptySet, mutex);
                    swapper.swap(emptySet, writer);
                    waitingWriters--;
                }
                writers++;
                swapper.swap(emptySet, mutex);
                System.out.println("Writer " + id + " is writing");
                swapper.swap(mutex, emptySet);
                writers--;
                if (waitingReaders > 0) {
                    swapper.swap(reader, emptySet);
                } else if (waitingWriters > 0) {
                    swapper.swap(writer, emptySet);
                } else {
                    swapper.swap(emptySet, mutex);
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }



    public static void main(String[] args) throws InterruptedException {

        /*
        Problem czytelników i pisarzy zaimplementowany na podstawie
        gdzie semafor ochrona to singleton {2}
        semafor czytelnicy to singleton {0}
        semafor pisarze to singleton {1}
         */

        swapper.swap(emptySet, mutex);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < READERS; i++) {
            threads.add(new Thread(new Reader(i)));

        }

        for (int i = 0; i < WRITERS; i++) {
            threads.add(new Thread(new Writer(i)));
        }

        for (Thread t : threads) {
            t.start();
        }
        try {
            for (Thread t : threads) {
                t.join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
