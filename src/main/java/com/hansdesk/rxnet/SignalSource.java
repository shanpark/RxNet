package com.hansdesk.rxnet;

import io.reactivex.rxjava3.core.Observable;
import io.reactivex.rxjava3.disposables.Disposable;
import io.reactivex.rxjava3.schedulers.Schedulers;
import io.reactivex.rxjava3.subjects.Subject;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 생성 즉시 selector를 통한 이벤트를 모니터링하고 적절히 값을 발행하는 thread가 시작된다.
 * 사용되는 scheduler는 Schedulers.computation()이다.
 * TCP connection이 들어오면 TcpChannel을 발행할 것이고 peer로부터 data가 들어오면 Buffer를 발행할 것이다.
 */
class SignalSource {

    private static class RegisterRequest {
        public Selectable selectable;
        public int ops;
        public Subject<Integer> receiver;

        public RegisterRequest(Selectable selectable, int ops, Subject<Integer> receiver) {
            this.selectable = selectable;
            this.ops = ops;
            this.receiver = receiver;
        }

        public void register(Selector selector) throws ClosedChannelException {
            selectable.selectionKey(selectable.channel().register(selector, ops, receiver));
        }
    }

    private static class SignalRequest {
        public int signal;
        public Subject<Integer> receiver;

        public SignalRequest(int signal, Subject<Integer> receiver) {
            this.signal = signal;
            this.receiver = receiver;
        }
    }

    private Disposable disposable;
    private final Selector selector;
    private final List<RegisterRequest> registerRequestQueue = new ArrayList<>();
    private final List<SignalRequest> signalRequestQueue = new ArrayList<>();

    SignalSource() {
        try {
            selector = Selector.open(); // 여기서 IOException이 발생한다면 더 이상 진행은 의미없다.
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        disposable = Observable.<SelectionKey>create(emitter -> {
                    try {
                        while (selector.isOpen()) {
                            throw new RuntimeException("Test");

                            int count = selector.select();
                            if (count > 0) {
                                final Set<SelectionKey> keys = selector.selectedKeys();
                                for (SelectionKey key : keys)
                                    if (key.isValid())
                                        emitter.onNext(key);
                                keys.clear();
                            }

                            if (!signalRequestQueue.isEmpty()) {
                                synchronized (signalRequestQueue) {
                                    for (SignalRequest request : signalRequestQueue)
                                        request.receiver.onNext(request.signal);
                                    signalRequestQueue.clear();
                                }
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
                .subscribeOn(Schedulers.computation()) // current thread가 아닌 다른 thread여야 한다.
                .subscribe(this::onNext, this::onError, this::onComplete);
    }

    private void onNext(SelectionKey key) {
        try {
            //noinspection unchecked
            ((Subject<Integer>) key.attachment()).onNext(key.readyOps());
        } catch (Exception e) {
            //noinspection unchecked
            ((Subject<Integer>) key.attachment()).onError(e);
        }
    }

    private void onError(Throwable e) {
        try {
            e.printStackTrace();
            stop();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private void onComplete() {
        try {
            stop();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private boolean isRunning() {
        return ((disposable != null) && !disposable.isDisposed());
    }

    /**
     * event 생성을 끝낸다.
     * 한 번 stop하면 다시 시작할 수 있는 방법은 없다.
     */
    public void stop() throws IOException {
        if (isRunning())
            disposable.dispose();

        if (selector.isOpen())
            selector.close(); // selector가 blocked 상태라도 즉시 interrupt하고 close시킨다.
    }

    /**
     * selector에 발생하는 ops 이벤트를 받을 수 있도록 channel을 등록한다. channel 객체는 Selectable 인터페이스를 구현해야 한다.
     * 등록이 된 후부터 이벤트를 받을 수 있다.
     *
     * @param selectable selector에 등록해서 selectionKey의 ops에 대응하는 이벤트를 받을 객체. Selectable 인터페이스를 구현해야 한다.
     * @param ops 관심이 있는 ops값. (OP_ACCEPT, OP_READ, ...)
     * @param receiver 이벤트를 수신할 Subject 객체.
     */
    public void register(Selectable selectable, int ops, Subject<Integer> receiver) {
        synchronized (registerRequestQueue) {
            registerRequestQueue.add(new RegisterRequest(selectable, ops, receiver));
        }
        selector.wakeup();
    }

    /**
     * receiver가 signal을 subject를 통해서 받을 수 있도록 요청한다.
     * 요청하면 receiver로 signal이 수신된다.
     *
     * @param signal 보낼 signal
     * @param receiver signal을 받을 Subject 객체.
     */
    public void signal(int signal, Subject<Integer> receiver) {
        synchronized (signalRequestQueue) {
            signalRequestQueue.add(new SignalRequest(signal, receiver));
        }
        selector.wakeup();
    }
}
