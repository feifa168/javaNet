package com.ft.net.nio.channels;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedList;
import java.util.List;
import java.util.Scanner;

public class MyAIOServer {
    private int port;
    private List<AsynchronousSocketChannel> clients = new LinkedList<>();

    public MyAIOServer(int port) {
        this.port = port;
    }

    public void sendMessage(String msg) {
        ByteBuffer writeData = ByteBuffer.wrap(msg.getBytes());
        clients.forEach(client->client.write(writeData, writeData, new CompletionHandler<Integer, ByteBuffer>() {
            @Override
            public void completed(Integer writeLen, ByteBuffer data) {
                try {
                    System.out.println("send to " + client.getRemoteAddress()
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
        }));
    }

    public void start() {
        try {
            AsynchronousServerSocketChannel serverChannel = AsynchronousServerSocketChannel.open();
            serverChannel.bind(new InetSocketAddress(port));
            serverChannel.accept(null, new CompletionHandler<AsynchronousSocketChannel, Object>() {
                @Override
                public void completed(AsynchronousSocketChannel clientChannel, Object data) {
                    serverChannel.accept(null, this);

                    clients.add(clientChannel);
                    try {
                        System.out.println("client from " + clientChannel.getRemoteAddress() + " is conneced");
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

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

                                clients.remove(clientChannel);

                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    });
                }

                @Override
                public void failed(Throwable exc, Object attachment) {
                    System.out.println("read from " + " is failed, message is " + exc.getMessage());
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        MyAIOServer server = new MyAIOServer(12345);
        server.start();

        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("please input finish to exit");
            String input = sc.next();
            if ("finish".equals(input)) {
                System.out.println("finished");
                break;
            } else {
                server.sendMessage(input);
            }
        }
    }
}
