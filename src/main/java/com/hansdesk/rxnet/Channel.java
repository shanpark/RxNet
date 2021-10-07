package com.hansdesk.rxnet;

import io.reactivex.rxjava3.subjects.Subject;

public interface Channel {
    Subject<Integer> subject();

    Channel appendHandler(Handler handler);

    void handlerChain(HandlerChain handlerChain);
    HandlerChain handlerChain();
    void write(Object obj);
    void stop();
}
