package com.ft.net.nio.channels;

import org.junit.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class AIOClient1 {
    @Test
    public void testCode() {
        String hel = "你好hello";
        System.out.println("你好hello");
        ByteBuffer buffer = ByteBuffer.wrap("你好hello".getBytes());
        String ss = new String(buffer.array());
        CharBuffer charBuffer = CharBuffer.allocate(64);
        CharsetDecoder decoder = Charset.defaultCharset().newDecoder();
        CoderResult rt = decoder.decode(buffer,charBuffer,false);
        charBuffer.flip();
        System.out.println("conv from utf8 to unicode " + charBuffer.toString());
    }

    public void connectByCallBack() throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        channel.connect(new InetSocketAddress("127.0.0.1", 8888), null, new CompletionHandler<Void, Object>() {
            @Override
            public void completed(Void result, Object attachment) {

                ByteBuffer readData = ByteBuffer.allocate(128);
                channel.read(readData, readData, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer readLen, ByteBuffer data) {
                        System.out.println("read length is " + readLen + ", data is " + new String(data.array(), 0, data.limit()));
                        //channel.read(readData, readData, this);
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer attachment) {
                        System.out.println("read data is failed");
                    }
                });

                ByteBuffer sendData = ByteBuffer.wrap("你好hello".getBytes());
                channel.write(sendData, sendData, new CompletionHandler<Integer, ByteBuffer>() {
                    @Override
                    public void completed(Integer result, ByteBuffer data) {
                        System.out.println("send data is " + new String(data.array(), 0, data.limit()));
                    }

                    @Override
                    public void failed(Throwable exc, ByteBuffer data) {
                        System.out.println("send data is fail");
                    }
                });
            }

            @Override
            public void failed(Throwable exc, Object attachment) {
            }
        });

        Thread.sleep(5000);

        System.out.println("end");
    }

    public void connectByFuture() throws IOException, ExecutionException, InterruptedException {
        AsynchronousSocketChannel channel = AsynchronousSocketChannel.open();
        ByteBuffer buffer = ByteBuffer.wrap("你好hello".getBytes());
        channel.connect(new InetSocketAddress("127.0.0.1",8888)).get();
        Future<Integer> future = channel.write(buffer);
        future.get();
        System.out.println("send ok");

        buffer.flip();
        channel.read(buffer).get();
        System.out.println("read message is " + new String(buffer.array(), 0, buffer.limit()));

        System.out.println("end");}

    public static void main(String[] args) throws IOException, ExecutionException, InterruptedException {
        AIOClient1 client = new AIOClient1();
        client.connectByCallBack();
    }
}
