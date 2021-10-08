package com.hansdesk.rxnet.util;

import com.hansdesk.rxnet.Handler;
import com.hansdesk.rxnet.ServerHandler;

public class Handlers {
    public static ServerHandler EMPTY_SERVER_HANDLER = new ServerHandler() {};
    public static Handler EMPTY_CHANNEL_HANDLER = new Handler() {};
}
