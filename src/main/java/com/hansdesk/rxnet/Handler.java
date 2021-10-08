package com.hansdesk.rxnet;

public interface Handler {
    default void onStart(Channel channel) {}

    /**
     * channel에 read된 데이터가 있으면 호출된다.
     * 여기서 메소드를 나가면  buffer는 항상 unmark 상태로 돌아간다.
     *
     * @param channel current channel.
     * @param buffer read buffer.
     */
    default void onInbound(Channel channel, Buffer buffer) {}

    default void onOutbound(Channel channel, Buffer buffer) {}

    default void onStop(Channel channel) {}

    default void onError(Channel channel, Throwable e) {}
}
