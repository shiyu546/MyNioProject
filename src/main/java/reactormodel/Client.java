package reactormodel;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.Socket;

import static java.lang.Thread.sleep;

public class Client {

    public static void main(String[] args) throws InterruptedException {

        while (true){
            sleep(1000);
            Socket socket=null;
            OutputStream ots=null;
            PrintWriter pw=null;
            try {
                socket=new Socket("localhost",8080);
                ots = socket.getOutputStream();
                pw = new PrintWriter(ots);
                pw.write("用户名：admin;密码：1234");
                pw.flush();
                socket.shutdownOutput();
            } catch (IOException e) {
                e.printStackTrace();
            }finally {
                pw.close();
                try {
                    ots.close();
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


    }
}
