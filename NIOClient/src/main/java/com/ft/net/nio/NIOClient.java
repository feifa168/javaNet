package com.ft.net.nio;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;

/**
 * client实例代码
 */
public class NIOClient {
    // 通道管理器
    private Selector selector;
    private SocketChannel clientChannel;
    private boolean run;

    public void setRun(boolean run) {
        this.run = run;
    }

    public SocketChannel getClientChannel() { return clientChannel; }

    /**
     * 获得一个Socket通道，并对该通道做一些初始化的工作
     * @param ip 连接服务器ip
     * @param port 连接服务器端口
     * @throws IOException
     */
    public void initClient(String ip, int port) throws IOException {
        // 获得一个Socket通道
        SocketChannel channel = SocketChannel.open();
        clientChannel = channel;
        run = true;
        // 设置通道为非阻塞
        channel.configureBlocking(false);
        // 获得一个通道管理器
        this.selector = Selector.open();
        // 用channel.finishConnect();才能完成连接
        // 客户端连接服务器,其实方法执行并没有实现连接，需要在listen()方法中调
        channel.connect(new InetSocketAddress(ip, port));
        // 将通道管理器和该通道绑定，并为该通道注册SelectionKey.OP_CONNECT事件
        channel.register(selector, SelectionKey.OP_CONNECT);
        System.out.println("init server " + ip + " port " + port);
    }

    /**
     * 采用轮询的方式监听selector上是否有需要处理的事件，如果有，则进行处理
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public void listen() throws Exception {
        // 轮询访问selector
        while (run) {
            /*
             * 选择一组可以进行I/O操作的事件，放在selector中,客户端的该方法不会阻塞，
             * selector的wakeup方法被调用，方法返回，而对于客户端来说，通道一直是被选中的
             * 这里和服务端的方法不一样，查看api注释可以知道，当至少一个通道被选中时。
             */
            selector.select();
            // 获得selector中选中的项的迭代器
            Iterator ite = this.selector.selectedKeys().iterator();
            while (ite.hasNext()) {
                SelectionKey key = (SelectionKey) ite.next();
                // 删除已选的key，以防重复处理
                ite.remove();
                // 连接事件发生
                if (!key.isValid()) {
                    System.out.println("not valid selector");
                    continue;
                }

                if (key.isConnectable()) {
                    // 如果正在连接，则完成连接
                    SocketChannel channel = (SocketChannel) key.channel();
                    if (channel.isConnectionPending()) {
                        channel.finishConnect();
                    }
                    // 设置成非阻塞
                    channel.configureBlocking(false);
                    // 在这里可以给服务端发送信息哦
                    channel.write(ByteBuffer.wrap(new String("hello server!").getBytes()));
                    // 在和服务端连接成功之后，为了可以接收到服务端的信息，需要给通道设置读的权限。
                    channel.register(this.selector, SelectionKey.OP_READ); // 获得了可读的事件
                } else if (key.isReadable()) {
                    read(key);
                }
            }
        }
    }

    private void read(SelectionKey key){
        SocketChannel channel = (SocketChannel) key.channel();
        // 分配缓冲区
        ByteBuffer buffer = ByteBuffer.allocate(10);
        int readLen = 0;
        try {
            readLen = channel.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
            try {
                channel.socket().close();
                channel.close();
                return;
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }

        byte[] data = buffer.array();
        String msg = new String(data).trim();
        System.out.println("client receive msg from server:" + msg);
//        ByteBuffer outBuffer = ByteBuffer.wrap(msg.getBytes());
//        channel.write(outBuffer);
    }

    /**
     * 启动客户端测试
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        NIOClient client = new NIOClient();
        client.initClient("localhost", 12345);

        byte bt[] = new byte[256];
        Thread sendThread = new Thread(()->{
            while (true) {
                try {
                    int readLen = System.in.read(bt);
//                    if ( ((readLen == 2) && ('\r' == bt[0]) && ('\n' == bt[1]))
//                        || (readLen == 1 && ('\n') == bt[0]) ) {
//                        continue;
//                    }
                    if (new String(bt, 0, readLen).startsWith("exit")) {

                        break;
                    }
                    ByteBuffer btBuf = ByteBuffer.wrap(bt, 0, readLen);
                    client.getClientChannel().write(btBuf);
                } catch (IOException e) {
                    e.printStackTrace();
                    try {
                        client.getClientChannel().socket().close();
                        client.getClientChannel().close();
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                    break;
                }
            }
        });
        sendThread.start();

        client.listen();

        sendThread.join();
    }
}
