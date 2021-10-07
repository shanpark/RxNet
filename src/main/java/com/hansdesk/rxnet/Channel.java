package com.hansdesk.rxnet;

import io.reactivex.rxjava3.subjects.Subject;

public interface Channel {
    Subject<Long> subject();

    Channel appendHandler(Handler handler);

    void handlerChain(HandlerChain handlerChain);
    HandlerChain handlerChain();
    void out(Object obj);
    void stop();
}
