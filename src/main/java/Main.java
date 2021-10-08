import com.hansdesk.rxnet.*;

import java.util.concurrent.ExecutionException;

public class Main {
    public static void log(Object x) {
        System.out.format("%s\t: %s\n", Thread.currentThread().getName(), x.toString());
    }

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        ServerHandler serverHandler = new ServerHandler() {
            @Override
            public void onNewChannel(Server server, Channel channel) {
                log(channel);
            }
        };

        Handler handler = new Handler() {
            @Override
            public void onStart(Channel channel) {
                System.out.format("New Channel started: %s\n", channel.toString());
            }

            @Override
            public void onInbound(Channel channel, Buffer buffer) {
                channel.write(buffer);
                throw new RuntimeException("Test!!!");
            }

            @Override
            public void onStop(Channel channel) {
                System.out.format("Channel stopped: %s\n", channel.toString());
            }

            @Override
            public void onError(Channel channel, Throwable e) {
                System.out.format("Channel error: %s\n", channel.toString());
                e.printStackTrace();
            }
        };

        Server s = Servers.newTcpServer()
                .port(9000)
                .serverHandler(serverHandler)
                .channelHandler(handler)
                .start();

        s.await();
    }
}
