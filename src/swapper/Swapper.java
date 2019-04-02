package swapper;

import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Swapper<E> {

    private class Waiter {
        Condition condtition;
        int conditionQueueLen;

        Waiter(Condition c, int ctr) {
            this.condtition = c;
            this.conditionQueueLen = ctr;
        }
    }

    public Collection<E> getValueSet() {
        return this.SwapperCollection;
    }

    private Collection<E> SwapperCollection = ConcurrentHashMap.newKeySet();
    private final ConcurrentHashMap<Collection<E>, Waiter> waitingQueues = new ConcurrentHashMap<>();

    private final ReentrantLock lock = new ReentrantLock(true);
    private final Condition full = lock.newCondition();
    private final Condition empty = lock.newCondition();


    public Swapper() {
    }

    public void setMutex(boolean l) {
        if (l) {
            lock.lock();
        } else {
            lock.unlock();
        }
    }

    public void waitFull() throws InterruptedException {
        full.await();
    }

    public void signalFull() {
        full.signal();
    }

    public void waitEmpty() throws InterruptedException {
        empty.await();
    }

    public void signalEmpty() {
        empty.signal();
    }

    public int collectionSize() {
        return SwapperCollection.size();
    }

    private void swapAndFind(Collection<E> removed, Collection<E> added) {
        Collection<E> Copy = SwapperCollection;

        SwapperCollection.removeAll(removed);
        SwapperCollection.addAll(added);
        for (Collection<E> key : waitingQueues.keySet()) {
            if (SwapperCollection.containsAll(key)) {
                waitingQueues.get(key).conditionQueueLen--;
                Waiter w = waitingQueues.get(key);
                if (waitingQueues.get(key).conditionQueueLen == 0) {
                    waitingQueues.remove(key);
                }
                w.condtition.signal();
            }
        }
    }

    private void waitForSet(Collection<E> removed) throws InterruptedException {
        if (!waitingQueues.keySet().contains(removed)) {
            waitingQueues.put(removed, new Waiter(lock.newCondition(), 1));
            waitingQueues.get(removed).condtition.await();
        } else {
            waitingQueues.get(removed).conditionQueueLen++;
            waitingQueues.get(removed).condtition.await();
        }
    }

    public void swap(Collection<E> removed, Collection<E> added) throws InterruptedException {

        lock.lock();
        try {
            while (!SwapperCollection.containsAll(removed)) {
                waitForSet(removed);
            }
            swapAndFind(removed, added);
        } finally {
            lock.unlock();
        }
    }
}
