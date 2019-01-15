package com.ft.net.nio;

import sun.security.pkcs.ParsingException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Set;

public class MyNIOClient {
    public MyNIOClient() {
    }
    public MyNIOClient(String host, int port) {
        init(host, port);
    }
    public void setHost(String host) { this.host = host; }
    public void setPort(int port) { this.port = port; }

    private void init(String host, int port) {
        this.host = host;
        this.port = port;

        readBuf = ByteBuffer.allocate(1024);
        writeBuf = ByteBuffer.allocate(1024);
    }

    public SocketChannel getClientChannel() { return clientChannel; }

    public boolean start() {
        boolean isok = true;

        try {
            SocketChannel client = SocketChannel.open();
            clientChannel = client;
            client.configureBlocking(false);
            client.connect(new InetSocketAddress(host, port));

            sel = Selector.open();
            client.register(sel, SelectionKey.OP_CONNECT);
            run = true;

            handleSelector();

        } catch (IOException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
            isok = false;
        } finally {
            if(!isok) {
                handleFinish(clientChannel);
            }
        }

        return isok;
    }

    private void handleSelector() throws IOException {
        int selNum = 0;
        while (run) {
            selNum = sel.select(2000);
            if (0 == selNum) {
                continue;
            }
            Set<SelectionKey> keys = sel.selectedKeys();
            for (SelectionKey key : keys) {
                if (!key.isValid()) {
                    continue;
                }
                if (key.isConnectable()) {
                    handleConnect(key);
                } else if (key.isReadable()) {
                    handleRead(key);
                } else if (key.isWritable()) {
                    handleWrite(key);
                }
            }
            keys.clear();
        }
    }

    private boolean handleConnect(SelectionKey key) {
        System.out.println("client is connect on " + host + " port " + port);
        boolean isok = false;
        try {
            if (clientChannel.isConnectionPending()) {
                clientChannel.finishConnect();
            }
            clientChannel.configureBlocking(false);
            clientChannel.register(sel, SelectionKey.OP_READ);

            isok = true;

        } catch (ClosedChannelException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
        } catch (IOException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
        } finally {
            if (!isok) {
                handleFinish(key);
            }
        }

        return isok;
    }

    private boolean handleRead(SelectionKey key) {
        SocketChannel client = (SocketChannel)(key.channel());
        int readLen = 0;
        readBuf.clear();
        boolean isok = true;

        try {
            while (readBuf.hasRemaining()) {
                readLen = client.read(readBuf);
                if (readLen < 1) {
                    break;
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
            isok = false;
        } finally {
            if (readBuf.limit() > 0) {
                readBuf.flip();
                System.out.println(new String(readBuf.array(), 0, readBuf.limit()));
            }

            if (!isok) {
                handleFinish(key);
            }
        }

        return isok;
    }

    private boolean handleWrite(SelectionKey key) {
        System.out.println("write ok");
        return true;
    }
    public boolean handleWrite(ByteBuffer buf) {
        //buf.flip();
        int writeLen = 0;
        boolean isok = true;

        try {
            while (buf.hasRemaining()) {
                writeLen = clientChannel.write(buf);
                if (writeLen < 1) {
                    break;
                }
            }
        } catch (IOException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
            isok = false;
        } finally {
            if (!isok) {
                handleFinish(clientChannel);
            }
        }

        return isok;
    }

    private void handleFinish(SelectionKey key) {
        System.out.println("close socket");
        finishRun();

        try {
            if (null != key) {
                key.cancel();
                SocketChannel client = (SocketChannel)(key.channel());
                client.socket().close();
                client.close();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
        }
    }
    private void handleFinish(SocketChannel client) {
        System.out.println("close socket");
        finishRun();

        try {
            if (null != client) {
                try {
                    System.out.println("close socket " + client.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                client.socket().close();
                client.close();
            } else {
                clientChannel.close();
            }
        } catch (IOException e) {
            //e.printStackTrace();
            errMessage = e.getMessage();
        }
    }
    public void finishRun() { run = false; }

    public String getErrMessage() { return errMessage==null ? "" : errMessage; }

    private String  host;
    private int     port;
    private Selector sel;
    private SocketChannel clientChannel;
    private boolean run;
    private ByteBuffer readBuf;
    private ByteBuffer writeBuf;
    private String errMessage;

    public static void main(String[] args) {

        MyNIOClient client = new MyNIOClient("127.0.0.1", 12345);

        try {
            Thread sendThread = new Thread(()->{
                try {
                    while (true) {
                        System.out.println("please input message to send, or input exit to quit");
                        BufferedReader bufRd = new BufferedReader(new InputStreamReader(System.in));
                        String readMsg = bufRd.readLine();
                        if (readMsg.equals("exit")) {
                            client.finishRun();
                            break;
                        }
                        if ( !client.handleWrite(ByteBuffer.wrap(readMsg.getBytes())) ) {
                            System.out.println(client.getErrMessage());
                            break;
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            sendThread.start();

            client.start();
            System.out.println(client.getErrMessage());

            sendThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
