package reactormodel;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.*;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class Reactor {
    private Map<Integer,EventHandler> registeredHandlers=new ConcurrentHashMap<>();
    private Selector demultiplexer;

    public Reactor() throws IOException {
        demultiplexer=Selector.open();
    }

    public void registerEventHandler(int eventType,EventHandler eventHandler){
        registeredHandlers.put(eventType,eventHandler);
    }

    public void registerChannel(int eventType, SelectableChannel channel) throws ClosedChannelException {
        channel.register(demultiplexer,eventType);
    }

    public void run(){
        try {
            while (true){
                demultiplexer.select();
                Set<SelectionKey> readyHandles=demultiplexer.selectedKeys();
                Iterator<SelectionKey> handleIterator=readyHandles.iterator();
                while(handleIterator.hasNext()){
                    SelectionKey handle=handleIterator.next();
                    handleIterator.remove();
                    if(handle.isAcceptable()){
                        EventHandler handler=registeredHandlers.get(SelectionKey.OP_ACCEPT);
                        try {
                            handler.handleEvent(handle);
                        }finally {
                            handle.cancel();
                        }

                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws IOException {
        ServerSocketChannel server=ServerSocketChannel.open();
        server.socket().bind(new InetSocketAddress(8080));
        server.configureBlocking(false);

        Reactor reactor=new Reactor();

        reactor.registerChannel(SelectionKey.OP_ACCEPT,server);

        reactor.registerEventHandler(SelectionKey.OP_ACCEPT,new AcceptEventHandler());

        reactor.run();

    }

}
