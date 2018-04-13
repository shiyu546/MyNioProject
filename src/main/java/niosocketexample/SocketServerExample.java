package niosocketexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.*;

/**
 * Created by Administrator on 2018/4/13.
 */
public class SocketServerExample {
    private Selector selector;
    private Map<SocketChannel,List<byte[]>> dataMapper;
    private InetSocketAddress listenAddress;

    public SocketServerExample(String address,int port) {
        listenAddress=new InetSocketAddress(address,port);
        dataMapper=new HashMap<SocketChannel,List<byte[]>>();
    }

    public static void main(String[] args) {
        Runnable server=new Runnable() {
            @Override
            public void run() {
                try{
                    new SocketServerExample("localhost",8090).startServer();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };

        Runnable client=new Runnable() {
            @Override
            public void run() {
                try{
                    new SocketClientExample().startClient();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        };
        new Thread(server).start();
        new Thread(client,"client-A").start();
        new Thread(client,"client-B").start();
    }

    private void startServer() throws IOException {
        this.selector = Selector.open();
        ServerSocketChannel serverChannel = ServerSocketChannel.open();
        serverChannel.configureBlocking(false);

        // retrieve server socket and bind to port
        serverChannel.socket().bind(listenAddress);
        serverChannel.register(this.selector, SelectionKey.OP_ACCEPT);

        System.out.println("Server started...");

        while (true) {
            // wait for events
            this.selector.select();

            //work on selected keys
            Iterator<SelectionKey> keys = this.selector.selectedKeys().iterator();
            while (keys.hasNext()) {
                SelectionKey key = (SelectionKey) keys.next();

                // this is necessary to prevent the same key from coming up 
                // again the next time around.
                keys.remove();

                if (!key.isValid()) {
                    continue;
                }

                if (key.isAcceptable()) {
                    this.accept(key);
                }
                else if (key.isReadable()) {
                    this.read(key);
                }
            }
        } 
    }

    private void read(SelectionKey key) throws IOException {
        SocketChannel channel= (SocketChannel) key.channel();
        ByteBuffer buffer=ByteBuffer.allocate(10);
        int numRead=-1;
        numRead=channel.read(buffer);
        System.out.println("numRead的值是"+numRead);
        if(numRead==-1){
            this.dataMapper.remove(channel);
            Socket socket=channel.socket();
            SocketAddress remoteAddr=socket.getRemoteSocketAddress();
            System.out.println("Connection closed by client:"+remoteAddr);
            channel.close();
            key.cancel();
            return;
        }

        byte[] data=new byte[numRead];
        System.arraycopy(buffer.array(),0,data,0,numRead);
        System.out.println("Got:"+new String(data));

    }

    private void accept(SelectionKey key) throws IOException {
        ServerSocketChannel serverChannel = (ServerSocketChannel) key.channel();
        SocketChannel channel = serverChannel.accept();
        channel.configureBlocking(false);
        Socket socket = channel.socket();
        SocketAddress remoteAddr = socket.getRemoteSocketAddress();
        System.out.println("Connected to: " + remoteAddr);

        // register channel with selector for further IO
        dataMapper.put(channel, new ArrayList<byte[]>());
        channel.register(this.selector, SelectionKey.OP_READ);
    }
}
