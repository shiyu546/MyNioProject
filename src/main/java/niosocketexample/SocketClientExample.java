package niosocketexample;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

/**
 * Created by Administrator on 2018/4/13.
 */
public class SocketClientExample {

    public void startClient() throws IOException, InterruptedException {
        InetSocketAddress hostAddress=new InetSocketAddress("localhost",8090);
        SocketChannel client=SocketChannel.open(hostAddress);

        System.out.println("Client... started");

        String threadName=Thread.currentThread().getName();
        String[] messages=new String[]{
            threadName+":test中文会怎样",
            threadName+":test2",
            threadName+":test3"
        };

        for(int i=0;i<messages.length;i++){
            byte[] message=new String(messages[i]).getBytes();
            ByteBuffer buffer=ByteBuffer.wrap(message);
            client.write(buffer);
            System.out.println(messages[i]);
            buffer.clear();
            Thread.sleep(5000);
        }
        client.close();
    }
}
