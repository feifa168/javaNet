package com.ft.net.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.Scanner;

public class MyAIOClient {
    private String host;
    private int port;
    AsynchronousSocketChannel clientChannel = null;

    public MyAIOClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void sendMessage(String msg) {
        ByteBuffer writeData = ByteBuffer.wrap(msg.getBytes());
        clientChannel.write(writeData, writeData, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer writeLen, ByteBuffer data) {
                try {
                    System.out.println("send to " + clientChannel.getRemoteAddress()
                            + ", write length is " + writeLen
                            + " , data is " + new String(data.array(), 0, data.limit()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer attachment) {
                System.out.println("write to is failed, message is " + exc.getMessage());
            }
        });
    }

    public void start() {
        try {
            clientChannel = AsynchronousSocketChannel.open();
            clientChannel.connect(new InetSocketAddress(host, port), null,
                    new CompletionHandler<Void, Object>() {
                        @Override
                        public void completed(Void result, Object attachment) {
                            System.out.println("client is connected");

                            ByteBuffer readBuf = ByteBuffer.allocate(128);
                            clientChannel.read(readBuf, readBuf, new CompletionHandler<Integer, ByteBuffer>() {
                                @Override
                                public void completed(Integer readLen, ByteBuffer data) {
                                    data.flip();
                                    clientChannel.read(readBuf, readBuf, this);

                                    try {
                                        System.out.println("read from " + clientChannel.getRemoteAddress()
                                                + ", read length is " + readLen
                                                + " , data is " + new String(data.array(), 0, data.limit()));
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                    data.clear();
                                }

                                @Override
                                public void failed(Throwable exc, ByteBuffer data) {
                                    try {
                                        System.out.println("read from " + clientChannel.getRemoteAddress()
                                                + " is failed, message is " + exc.getMessage());
                                    } catch (IOException e) {
                                        e.printStackTrace();
                                    }
                                }
                            });
                        }

                        @Override
                        public void failed(Throwable exc, Object attachment) {
                            System.out.println("client connect is failed, message is " + exc.getMessage());
                        }
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MyAIOClient client = new MyAIOClient("127.0.0.1", 12345);
        client.start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("please input finish to exit");
            String input = sc.next();
            if ("finish".equals(input)) {
                System.out.println("finished");
                break;
            } else {
                client.sendMessage(input);
            }
        }
    }
}
