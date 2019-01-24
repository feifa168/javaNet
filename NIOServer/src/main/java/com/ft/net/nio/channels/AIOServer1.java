package com.ft.net.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;

public class AIOServer1 {
    public static void main(String[] args) {
        final AsynchronousServerSocketChannel channel;
        try {
            channel = AsynchronousServerSocketChannel
                    .open()
                    .bind(new InetSocketAddress(8888));

            channel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Void>() {
                @Override
                public void completed(final AsynchronousSocketChannel client, Void attachment) {
                    channel.accept(null, this);

                    ByteBuffer sendData = ByteBuffer.wrap("this is server".getBytes());
                    System.out.println("send data to " + new String(sendData.array()));
                    client.write(sendData, sendData, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result, ByteBuffer attachment) {
                            System.out.println("send data is completed ");
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            System.out.println("send data is failed");
                        }
                    });

                    ByteBuffer readData = ByteBuffer.allocate(1024);
                    client.read(readData, readData, new CompletionHandler<Integer, ByteBuffer>() {
                        @Override
                        public void completed(Integer result_num, ByteBuffer attachment) {
                            attachment.flip();
                            CharBuffer charBuffer = CharBuffer.allocate(64);
                            CharsetDecoder decoder = Charset.forName("gbk").newDecoder();
                            CoderResult rt = decoder.decode(attachment,charBuffer,false);
                            charBuffer.flip();
                            String data = new String(charBuffer.array(),0, charBuffer.limit());
                            System.out.println("read data:" + charBuffer);
                        }

                        @Override
                        public void failed(Throwable exc, ByteBuffer attachment) {
                            System.out.println("read data is failed");
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, Void attachment) {
                    System.out.println("accept error");
                }
            });

        } catch (IOException e) {
            e.printStackTrace();
        }
        while (true){
            try {
                Thread.sleep(10000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }
}
