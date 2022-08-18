package io.mini.rpc.client.handler;

import io.mini.rpc.codec.RpcRequest;
import io.mini.rpc.codec.RpcResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.AbstractQueuedSynchronizer;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author caohao
 * @date 2022/8/17
 */
public class RpcFuture implements Future<Object> {

    private static final Logger logger = LoggerFactory.getLogger(RpcFuture.class);

    private static final long RESPONSE_TIME_THRESHOLD = 5000;

    private final Sync sync;
    private final RpcRequest request;
    private final long startTime;

    private final List<AsyncCallback> pendingCallbacks = new ArrayList<>();

    private final ReentrantLock lock = new ReentrantLock();

    private RpcResponse response;

    public RpcFuture(RpcRequest request) {
        this.request = request;
        this.sync = new Sync();
        startTime = System.currentTimeMillis();
    }

    @Override
    public Object get() throws InterruptedException, ExecutionException {
        sync.acquire(1);
        if (response != null) {
            return response.getResult();
        }
        return null;
    }

    @Override
    public Object get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        boolean success = sync.tryAcquireNanos(1, unit.toNanos(timeout));
        if (success) {
            if (response != null) {
                return response.getResult();
            } else {
                return null;
            }
        }
        throw new RuntimeException("Timeout exception. Request id: " + this.request.getRequestId()
                + ". Request class name: " + this.request.getClassName()
                + ". Request method: " + this.request.getMethodName());
    }

    public void done(RpcResponse response) {
        this.response = response;
        sync.release(1);

        long responseTime = System.currentTimeMillis() - startTime;
        if (responseTime > RESPONSE_TIME_THRESHOLD) {
            logger.warn("Service response time is too slow. Request id = " + response.getRequestId() + ". Response Time = " + responseTime + "ms");
        }
    }

    @Override
    public boolean isDone() {
        return sync.isDone();
    }

    private void invokeCallbacks() {
        lock.lock();
        try {
            for (AsyncCallback callback : pendingCallbacks) {
                runCallback(callback);
            }
        } finally {
            lock.unlock();
        }
    }

    public RpcFuture addCallback(AsyncCallback callback) {
        lock.lock();
        try {
            if (isDone()) {
                runCallback(callback);
            } else {
                pendingCallbacks.add(callback);
            }
        } finally {
            lock.unlock();
        }
        return this;
    }

    private void runCallback(final AsyncCallback callback) {
        final RpcResponse res = response;
        // TODO: 2022/8/17 run
    }


    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean isCancelled() {
        throw new UnsupportedOperationException();
    }

    static class Sync extends AbstractQueuedSynchronizer {
        private static final long serialVersionUID = 1L;

        //future status
        private final int done = 1;
        private final int pending = 0;

        @Override
        protected boolean tryAcquire(int arg) {
            return getState() == done;
        }

        @Override
        protected boolean tryRelease(int arg) {
            if (getState() == pending) {
                if (compareAndSetState(pending, done)) {
                    return true;
                } else {
                    return false;
                }
            } else {
                return true;
            }
        }

        protected boolean isDone() {
            return getState() == done;
        }
    }

}
