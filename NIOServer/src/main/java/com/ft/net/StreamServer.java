package com.ft.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class StreamServer {
    public static void main(String[] args) {

        try {
            ServerSocket server = new ServerSocket(11111);
            System.out.println("server is started");
            while ( true ) {
                Socket client = server.accept();
                InputStream is = client.getInputStream();
                OutputStream os = client.getOutputStream();
                byte[] ibuf = new byte[128];
                int readLen = is.read(ibuf);
                System.out.println("receive data is " + new String(ibuf));

                String sendData = "this is server";
                System.out.println("send data is " + sendData);
                os.write(sendData.getBytes());

                is.close();
                os.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
