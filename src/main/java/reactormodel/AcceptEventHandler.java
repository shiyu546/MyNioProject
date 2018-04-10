package reactormodel;

import java.nio.channels.SelectionKey;

public class AcceptEventHandler implements EventHandler {

    @Override
    public void handleEvent(SelectionKey handle) {
        System.out.println("处理连接请求...");
    }
}
