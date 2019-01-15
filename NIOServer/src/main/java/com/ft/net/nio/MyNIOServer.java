package com.ft.net.nio;

import javax.swing.text.html.HTMLDocument;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class MyNIOServer {
    public MyNIOServer(){
        init(12345);
    }

    public MyNIOServer(int port){
        init(port);
        readBuf = ByteBuffer.allocate(16);
    }

    private void init(int port){
        this.port = port;
    }

    public void setPort(int port) { this.port = port; }

    public void start() throws IOException {
        System.out.println("start server on port " + port);

        ServerSocketChannel ssc = ServerSocketChannel.open();
        ssc.configureBlocking(false);
        ssc.socket().bind(new InetSocketAddress(port));

        sel = Selector.open();
        ssc.register(sel, SelectionKey.OP_ACCEPT);

        this.run = true;
        handleSelector();
    }
    private void handleSelector() {
        while (run) {
            try {
                int selNum = sel.select(2000);
                if (0 == selNum) {
                    System.out.println("select result is 0");
                    continue;
                }

                Set<SelectionKey> keys = sel.selectedKeys();
                for (SelectionKey key : keys) {
                    if (!key.isValid()) {
                        continue;
                    }

                    if (key.isAcceptable()) {
                        handleAccept(key);
                    } else if (key.isReadable()) {
                        handleRead(key);
                    } else if (key.isWritable()) {
                        handleWrite(key);
                    }
                }
                keys.clear();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean handleAccept(SelectionKey key) {
        ServerSocketChannel ssc = (ServerSocketChannel)(key.channel());
        SocketChannel client = null;
        boolean isok = false;
        try {
            client = ssc.accept();

            if (null != client) {
                clients.add(client);

                client.configureBlocking(false);
                client.register(sel, SelectionKey.OP_READ);
                isok = true;

                try {
                    System.out.println("client " + client.getRemoteAddress().toString() + " is connected");
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (ClosedChannelException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
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
        boolean isok = true;
        readBuf.clear();

        try {
            while (readBuf.hasRemaining()) {
                readLen = client.read(readBuf);
                if (readLen < 1) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
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
        if (null != key) {
            SocketChannel client = (SocketChannel) key.channel();
            try {
                System.out.println(client.getRemoteAddress().toString() + " write ok");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return true;
    }

    public void handleWrite(ByteBuffer buff) {
        for (SocketChannel client : clients) {
            boolean isok = true;
            try {
                client.write(buff);
                buff.flip();
            } catch (IOException e) {
                e.printStackTrace();
                isok = false;
            } finally {
                if (!isok) {
                    handleFinish(client);
                }
            }
        }
    }

    private void handleFinish(SelectionKey key) {
        try {
            if (null != key) {
                key.cancel();
                SocketChannel client = (SocketChannel)(key.channel());
                try {
                    System.out.println("close socket " + client.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                client.socket().close();
                client.close();
                clients.remove(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void handleFinish(SocketChannel client) {
        try {
            if (null != client) {
                try {
                    System.out.println("close socket " + client.getRemoteAddress().toString());
                } catch (IOException e) {
                    e.printStackTrace();
                }

                client.socket().close();
                client.close();
                clients.remove(client);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void finishRun() { run = false; }
    public boolean isRunning() { return run; }

    private int port;
    private Selector sel = null;
    private List<SocketChannel> clients = new ArrayList<>();
    private boolean run;
    private ByteBuffer readBuf;

    public static void main(String[] args) {
        MyNIOServer server = new MyNIOServer(12345);

        Thread sendThread = new Thread(()->{
            try {
                while (true) {
                        System.out.println("please input message to send, or input exit to quit");
                        BufferedReader bufRd = new BufferedReader(new InputStreamReader(System.in));
                        String rdMsg = bufRd.readLine();

                        if (rdMsg.equals("exit")) {
                            server.finishRun();
                            break;
                        }
                        ByteBuffer wtBuf = ByteBuffer.wrap(rdMsg.getBytes());
                        server.handleWrite(wtBuf);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        sendThread.start();

        try {
            server.start();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            sendThread.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
