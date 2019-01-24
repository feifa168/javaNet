package com.ft.net.nio.channels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.LinkedHashSet;
import java.util.Set;

public class FtAIOServer {
    static class FtAIOClient {
        public FtAIOClient(AsynchronousSocketChannel clientChannel) {
            this.clientChannel = clientChannel;
            running = true;

            initBuf(defaultReadBufLen);
        }

        public void initBuf(int readBufLen) {
            this.readBufLen = readBufLen>defaultReadBufLen ? readBufLen : defaultReadBufLen;
        }

        public void start() {
            running = true;
            readBuf = ByteBuffer.allocate(readBufLen);

            doRead();
        }

        public void stop() {
            running = false;
        }

        public void doWrite(ByteBuffer buf) {
            if (!running) {
                doEnd();
                return;
            }
            clientChannel.write(buf, buf, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer writeLen, ByteBuffer data) {
                    try {
                        System.out.println("write to " + clientChannel.getRemoteAddress() + ", data is " + new String(data.array(), 0, data.limit()));
                    } catch (IOException e) {
                        doEnd();
                        e.printStackTrace();
                    }
                }

                @Override
                public void failed(Throwable exc, ByteBuffer data) {
                    try {
                        System.out.println("write to " + clientChannel.getRemoteAddress() + " fail, message is " + exc.getMessage());
                        doEnd();
                    } catch (IOException e) {
                        doEnd();
                        e.printStackTrace();
                    }
                }
            });
        }

        private void doRead() {
            if (!running) {
                doEnd();
                return;
            }

            clientChannel.read(readBuf, readBuf, new CompletionHandler<Integer, ByteBuffer>() {
                @Override
                public void completed(Integer readLen, ByteBuffer data) {
                    data.flip();
                    try {
                        System.out.println("read from " + clientChannel.getRemoteAddress() + " len is " + readLen + ", data is " + new String(data.array(), 0, data.limit()));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    //doRead();
                    //clientChannel.read(readBuf, readBuf, this);
                }

                @Override
                public void failed(Throwable exc, ByteBuffer data) {
                    try {
                        System.out.println("read from " + clientChannel.getRemoteAddress() + " fail, message is " + exc.getMessage());
                        doEnd();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        private void doEnd() {
            try {
                clientChannel.shutdownInput();
                clientChannel.shutdownOutput();
                clientChannel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private AsynchronousSocketChannel clientChannel;
        private boolean running;

        private ByteBuffer readBuf;
        private int readBufLen;
        private static final int defaultReadBufLen = 1024;
    }

    static class ServerAcceptCompletionHandler implements java.nio.channels.CompletionHandler<AsynchronousSocketChannel, Object> {
        public ServerAcceptCompletionHandler(FtAIOServer server) {
            this.server = server;
        }

        @Override
        public void completed(AsynchronousSocketChannel clientChannel, Object attachment) {
            server.aioServerChannel.accept(null, this);

            FtAIOClient client = new FtAIOClient(clientChannel);
            server.clients.add(client);
            try {
                System.out.println("client from " + clientChannel.getRemoteAddress() + " is connected");
                server.doRead(client);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void failed(Throwable exc, Object attachment) {
            System.out.println("client connect is fail, message is " + exc.getMessage());
        }

        private FtAIOServer server;
    }

    public FtAIOServer(int port) {
        init(port);
    }

    public FtAIOServer() {
        init(defaultPort);
    }
    public void setPort(int port) {this.port = port;}

    public void start() throws IOException {
        aioServerChannel = AsynchronousServerSocketChannel.open();
        aioServerChannel.bind(new InetSocketAddress(port));
        running = true;
        System.out.println("server is running on port " + port);

        doAccept();
    }

    public void stop() {
        running = false;
    }

    private void init(int port) {
        this.port = port;
    }

    private void doAccept() {
        if (!running) {
            doEnd();
            return;
        }

        aioServerChannel.accept(null, new ServerAcceptCompletionHandler(this));
    }

    private void doRead(FtAIOClient client) {
        client.doRead();
    }
    private void doWrite(ByteBuffer buf, FtAIOClient client) {
        client.doWrite(buf);
    }
    private void doWrite(ByteBuffer buf) {
        clients.forEach(client->client.doWrite(buf));
    }
    private void doEnd(FtAIOClient client) {
        client.stop();
        clients.remove(client);
    }
    private void doEnd() {
        clients.forEach(client->client.stop());
        clients.clear();
    }

    private int port;
    private static int defaultPort = 12345;
    private AsynchronousServerSocketChannel aioServerChannel;
    private boolean running;
    private String errMessage;
    private Set<FtAIOClient> clients = new LinkedHashSet();

    public static void main(String[] args) {
        FtAIOServer server = new FtAIOServer(12345);
        try {
            server.start();

            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("please input finish to exit");
                String sendMessage = input.readLine();
                if ("finish".equals(sendMessage)) {
                    break;
                } else if (sendMessage.length() > 0){
                    server.doWrite(ByteBuffer.wrap(sendMessage.getBytes()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
