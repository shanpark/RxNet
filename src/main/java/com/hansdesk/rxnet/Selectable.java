package com.hansdesk.rxnet;

import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;

public interface Selectable {
    SelectableChannel channel();
    void selectionKey(SelectionKey key);
}
