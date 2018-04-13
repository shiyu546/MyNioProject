package niosslexample;

import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * Created by Administrator on 2018/4/13.
 */
public abstract class SSLProvider implements Runnable{
    final SSLEngine engine;
    final Executor ioWorkder,taskWorkers;
    final ByteBuffer clientWrap,clientUnwrap;
    final ByteBuffer serverWrap,serverUnwrap;

    public SSLProvider(SSLEngine engine,int capactiy,Executor ioWorker,Executor taskWorkers){
        this.clientWrap=ByteBuffer.allocate(capactiy);
        this.serverWrap=ByteBuffer.allocate(capactiy);
        this.clientUnwrap=ByteBuffer.allocate(capactiy);
        this.serverUnwrap=ByteBuffer.allocate(capactiy);
        this.clientUnwrap.limit(0);
        this.engine=engine;
        this.ioWorkder=ioWorker;
        this.taskWorkers=taskWorkers;
        this.ioWorkder.execute(this);
    }

    public abstract void onInput(ByteBuffer decrypted);
    public abstract void onOutput(ByteBuffer encrypted);
    public abstract void onFailure(Exception ex);
    public abstract void onSuccess();
    public abstract void onClosed();

    public void sendAsync(final ByteBuffer data){
        this.ioWorkder.execute(new Runnable() {
            @Override
            public void run() {
                clientWrap.put(data);
                SSLProvider.this.run();
            }
        });
    }

    public void notify(final ByteBuffer data){
        this.ioWorkder.execute(new Runnable() {
            @Override
            public void run() {
                clientUnwrap.put(data);
                SSLProvider.this.run();
            }
        });
    }

    public void run(){
        while(this.isHandShaking()){
            continue;
        }
    }

    private synchronized boolean isHandShaking() {
        switch (engine.getHandshakeStatus()){
            case NOT_HANDSHAKING:
                boolean occupied=false;
                if(clientWrap.position()>0)
                    occupied |=this.wrap();
                if(clientUnwrap.position()>0)
                    occupied |=this.unwrap();
                return occupied;
            case NEED_WRAP:
                if(!this.wrap())
                    return false;
                break;
            case NEED_UNWRAP:
                if(!this.unwrap())
                    return false;
                break;
            case NEED_TASK:
                final Runnable sslTask=engine.getDelegatedTask();
                Runnable wrappedTask=new Runnable() {
                    @Override
                    public void run() {
                        sslTask.run();
                        ioWorkder.execute(SSLProvider.this);
                    }
                };
                taskWorkers.execute(wrappedTask);
                return false;
            case FINISHED:
                throw new IllegalStateException("FINISHED");
        }
        return true;
    }

    private boolean wrap() {
        SSLEngineResult wrapResult;
        try{
            clientWrap.flip();
            wrapResult=engine.wrap(clientWrap,serverWrap);
            clientWrap.compact();
        } catch (SSLException e) {
            e.printStackTrace();
            return false;
        }

        switch (wrapResult.getStatus()){
            case OK:
                if(serverWrap.position()>0){
                    serverWrap.flip();
                    this.onOutput(serverWrap);
                    serverWrap.compact();
                }
                break;
            case BUFFER_UNDERFLOW:
                break;
            case BUFFER_OVERFLOW:
                throw new IllegalStateException("failed to wrap");
            case CLOSED:
                this.onClosed();
                return false;
        }
        return true;

    }

    private boolean unwrap() {
        SSLEngineResult unwrapResult;

        try
        {
            clientUnwrap.flip();
            unwrapResult = engine.unwrap(clientUnwrap, serverUnwrap);
            clientUnwrap.compact();
        }
        catch (SSLException ex)
        {
            this.onFailure(ex);
            return false;
        }

        switch (unwrapResult.getStatus()) {
            case OK:
                if (serverUnwrap.position() > 0) {
                    serverUnwrap.flip();
                    this.onInput(serverUnwrap);
                    serverUnwrap.compact();
                }
                break;

            case CLOSED:
                this.onClosed();
                return false;

            case BUFFER_OVERFLOW:
                throw new IllegalStateException("failed to unwrap");

            case BUFFER_UNDERFLOW:
                return false;
        }

        if (unwrapResult.getHandshakeStatus() == SSLEngineResult.HandshakeStatus.FINISHED)
        {
            this.onSuccess();
            return false;
        }

        return true;
    }

}
