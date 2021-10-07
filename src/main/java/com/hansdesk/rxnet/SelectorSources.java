package com.hansdesk.rxnet;

public class SelectorSources {
    private static final SignalSource single = new SignalSource();

    public static SignalSource single() {
        return single;
    }
}
