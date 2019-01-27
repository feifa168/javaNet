package com.ft.net.nio.channels;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;

public class FtAIOClient {
    public FtAIOClient(String host, int port) {
        init(host, port);
        initBuf(defaultReadBufLen);
    }

    public void initBuf(int readBufLen) {
        this.readBufLen = readBufLen>defaultReadBufLen ? readBufLen : defaultReadBufLen;
    }

    public void start() throws IOException {
        clientChannel = AsynchronousSocketChannel.open();
        readBuf = ByteBuffer.allocate(readBufLen);
        running = true;

        doConnect();
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
                    System.out.println("write to " + clientChannel.getRemoteAddress() + ", write length is " + writeLen + ", data is " + new String(data.array(), 0, data.limit()));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void failed(Throwable exc, ByteBuffer data) {
                try {
                    System.out.println("write to " + clientChannel.getRemoteAddress() + " fail, message is " + exc.getMessage());
                } catch (IOException e) {
                    doEnd();
                    e.printStackTrace();
                }
            }
        });
    }

    private void init(String host, int port) {
        this.host = host;
        this.port = port;
    }

    private void doConnect() {
        clientChannel.connect(new InetSocketAddress(host, port), this, new CompletionHandler<Void, FtAIOClient>() {
            @Override
            public void completed(Void result, FtAIOClient client) {
                try {
                    System.out.println("connect to " + client.clientChannel.getRemoteAddress() + " is succed");
                } catch (IOException e) {
                    e.printStackTrace();
                }

                client.doRead();
            }

            @Override
            public void failed(Throwable exc, FtAIOClient attachment) {
                try {
                    System.out.println("connect to " + attachment.clientChannel.getRemoteAddress() + " is fail, message is " + exc.getMessage());
                } catch (IOException e) {
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
                try {
                    data.flip();
                    System.out.println("read from " + clientChannel.getRemoteAddress() + ", data is " + new String(data.array(), 0, data.limit()));
                } catch (IOException e) {
                    e.printStackTrace();
                }

                data.clear();
                clientChannel.read(readBuf, readBuf, this);
            }

            @Override
            public void failed(Throwable exc, ByteBuffer data) {
                try {
                    System.out.println("read from " + clientChannel.getRemoteAddress() + " fail, message is " + exc.getMessage());
                } catch (IOException e) {
                    doEnd();
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

    private String host;
    private int port;
    private boolean running;
    AsynchronousSocketChannel clientChannel;

    private ByteBuffer readBuf;
    private int readBufLen;
    private static final int defaultReadBufLen = 1024;

    public static void main(String[] args) {
        FtAIOClient client = new FtAIOClient("127.0.0.1", 12345);
        try {
            client.start();
            BufferedReader input = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.println("please input finish to quit");
                String sendMessage = input.readLine();
                if ("finish".equals(sendMessage)) {
                    client.stop();
                    break;
                } else if (sendMessage.length() > 0){
                    client.doWrite(ByteBuffer.wrap(sendMessage.getBytes()));
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
