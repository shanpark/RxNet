package com.hansdesk.rxnet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.exceptions.OnErrorNotImplementedException;
import io.reactivex.rxjava3.internal.functions.Functions;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * 생성 즉시 selector를 통한 이벤트를 모니터링하고 적절히 값을 발행하는 thread가 시작된다.
 * 사용되는 scheduler는 Schedulers.computation()이다.
 * TCP connection이 들어오면 TcpChannel을 발행할 것이고 peer로부터 data가 들어오면 Buffer를 발행할 것이다.
 */
class SignalSource {

    private static class RegisterRequest {
        public SelectableChannel channel;
        public int ops;
        public Object attachment;

        public RegisterRequest(SelectableChannel channel, int ops, Object attachment) {
            this.channel = channel;
            this.ops = ops;
            this.attachment = attachment;
        }

        public void register(Selector selector) throws ClosedChannelException {
            channel.register(selector, ops, attachment);
        }
    }

    private Selector selector;
    private final List<RegisterRequest> registerRequestQueue = new ArrayList<>();

    SignalSource() {
        this(e -> { throw new OnErrorNotImplementedException(e); }, Functions.EMPTY_RUNNABLE);
    }

    SignalSource(Consumer<Throwable> onError, Runnable onComplete) {
        Observable.<SelectionKey>create(emitter -> {
                    try {
                        selector = Selector.open();
                        while (selector.isOpen()) {
                            int count = selector.select();
                            if (count > 0) {
                                final Set<SelectionKey> keys = selector.selectedKeys();
                                for (SelectionKey key : keys)
                                    emitter.onNext(key);
                                keys.clear();
                            }

                            if (!registerRequestQueue.isEmpty()) {
                                synchronized (registerRequestQueue) {
                                    for (RegisterRequest request : registerRequestQueue)
                                        request.register(selector);
                                    registerRequestQueue.clear();
                                }
                            }
                        }

                        emitter.onComplete();
                    } catch (Throwable e) {
                        emitter.onError(e);
                    }
                })
                .subscribeOn(Schedulers.computation())
                .subscribe(this::onNext, onError::accept, onComplete::run);
    }

    private void onNext(SelectionKey key) {
        //noinspection unchecked
        ((Subject<Long>) key.attachment()).onNext(key.readyOps());
    }

    /**
     * event 생성을 끝낸다.
     * 한 번 stop하면 다시 시작할 수 있는 방법은 없다.
     */
    public void stop() throws IOException {
        if (selector.isOpen())
            selector.close();
    }

    public void register(SelectableChannel channel, int ops, Object attachment) {
        synchronized (registerRequestQueue) {
            registerRequestQueue.add(new RegisterRequest(channel, ops, attachment));
        }
        selector.wakeup();
    }
}
