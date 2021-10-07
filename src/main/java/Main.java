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
                server.stop();
            }
        };

        Handler handler = new Handler() {
            @Override
            public Object onInbound(Channel channel, Object inboundObj) {
                channel.out(inboundObj);
                return null;
            }
        };

        Server s = Servers.newTcpServer()
                .port(8080)
                .serverHandler(serverHandler)
                .defaultHandlerChain(HandlerChain.of(handler))
                .start();

        s.await();
    }
}
