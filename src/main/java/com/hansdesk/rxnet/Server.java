package com.hansdesk.rxnet;

public interface Server {
    Server host(String hostname);
    Server port(int port);
    Server serverHandler(ServerHandler serverHandler);
    Server defaultHandlerChain(HandlerChain handlerChain);

    HandlerChain defaultHandlerChain();

    Server start();
    void stop();
    boolean isRunning();
    void await();
    boolean await(long millis);
}
