package niosslexample;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLPeerUnverifiedException;
import javax.net.ssl.SSLSession;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.security.NoSuchAlgorithmException;
import java.util.Iterator;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Created by Administrator on 2018/4/13.
 */
public class NioSSLExample {
    public static void main(String[] args) throws IOException, NoSuchAlgorithmException {
        InetSocketAddress address=new InetSocketAddress("www.baidu.com",443);
        Selector selector=Selector.open();
        SocketChannel channel=SocketChannel.open();
        channel.connect(address);
        channel.configureBlocking(false);
        int ops= SelectionKey.OP_CONNECT|SelectionKey.OP_READ;

        SelectionKey key=channel.register(selector,ops);

        final Executor ioWorker= Executors.newSingleThreadExecutor();
        final Executor taskWorker=Executors.newFixedThreadPool(2);

        final SSLEngine engine= SSLContext.getDefault().createSSLEngine();
        engine.setUseClientMode(true);
        engine.beginHandshake();
        final int ioBufferSize=32*1024;
        final NioSSLProvider ssl=new NioSSLProvider(key,engine,ioBufferSize,ioWorker,taskWorker) {
            @Override
            public void onInput(ByteBuffer decrypted) {
                byte[] dst=new byte[decrypted.remaining()];
                decrypted.get(dst);
                String response=new String(dst);
                System.out.print(response);
                System.out.flush();
            }

            @Override
            public void onFailure(Exception ex) {
                System.out.println("handshake failure");
                ex.printStackTrace();
            }

            @Override
            public void onSuccess() {
                System.out.println("handshake success");
                SSLSession session=engine.getSession();
                try{
                    System.out.println("local principal:"+session.getLocalPrincipal());
                    System.out.println("remote principal:"+session.getPeerPrincipal());
                    System.out.println("cipher:"+session.getCipherSuite());
                } catch (SSLPeerUnverifiedException e) {
                    e.printStackTrace();
                }

                //HTTP request
                StringBuilder http=new StringBuilder();
                http.append("GET / HTTP/1.0\r\n");
                http.append("Connection: close\r\n");
                http.append("\r\n");
                byte[] data=http.toString().getBytes();
                ByteBuffer send=ByteBuffer.wrap(data);
                this.sendAsync(send);

            }

            @Override
            public void onClosed() {
                System.out.println("ssl session closed");
            }
        };

        while (true){
            key.selector().select();
            Iterator<SelectionKey> keys=key.selector().selectedKeys().iterator();
            while(keys.hasNext()){
                keys.next();
                keys.remove();
                ssl.processInput();
            }
        }

    }
}
