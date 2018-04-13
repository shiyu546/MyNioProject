package niosslexample;

import javax.net.ssl.SSLEngine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Executor;

/**
 * Created by Administrator on 2018/4/13.
 */
public abstract class NioSSLProvider extends SSLProvider {
    private final ByteBuffer buffer=ByteBuffer.allocate(32*1024);
    private final SelectionKey key;

    public NioSSLProvider(SelectionKey key,SSLEngine engine,int bufferSize,Executor ioWorker,Executor taskWorkers) {
        super(engine,bufferSize,ioWorker,taskWorkers);
        this.key=key;
    }

    public void onOutput(ByteBuffer encrypted){
        try{
            ((WritableByteChannel)this.key.channel()).write(encrypted);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean processInput(){
        buffer.clear();
        int bytes;
        try{
            bytes=((ReadableByteChannel)this.key.channel()).read(buffer);
        } catch (IOException e) {
            bytes=-1;
        }
        if(bytes==-1){
            return false;
        }
        buffer.flip();
        ByteBuffer copy=ByteBuffer.allocate(bytes);
        copy.put(buffer);
        copy.flip();
        this.notify(copy);
        return true;
    }

}
