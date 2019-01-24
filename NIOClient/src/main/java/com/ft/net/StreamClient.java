package com.ft.net;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

public class StreamClient {
    public static void main(String[] args) {
        try {
            Socket client = new Socket("127.0.0.1", 11111);
            InputStream is = client.getInputStream();
            OutputStream os = client.getOutputStream();
            String writeData = "this is client";
            System.out.println("send data is " + writeData);
            os.write(writeData.getBytes());

            byte[] ibuff = new byte[128];
            int readLen = is.read(ibuff);
            System.out.println("receive data is " + new String(ibuff));

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
