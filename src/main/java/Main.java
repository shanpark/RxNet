import com.hansdesk.net.tcp.ServerChannel;

import java.nio.channels.ServerSocketChannel;

public class Main {
    public static void log(Object x) {
        System.out.format("%s\t: %s\n", Thread.currentThread().getName(), x.toString());
    }

    public static void main(String[] args) throws InterruptedException {
        ServerChannel server = ServerChannel.create();
        server.start(8080, Main::log);

        while (server.isRunning())
            Thread.sleep(1000);
    }
}
