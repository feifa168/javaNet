## 简介
javaNet适用于java网络编程，手写NIO服务端和客户端。客户端和服务端可以互相发送消息，服务端发送消息为发送给所有客户端。

## 参考资料
* [基于NIO的同步非阻塞编程完整案例](https://www.cnblogs.com/houzheng/p/9460450.html)
* [NIO深入理解ServerSocketChannel](https://blog.csdn.net/yhl_jxy/article/details/79335692)
* [关于NIO客户端断开连接出现死循环的bug修复](https://blog.csdn.net/sinat_32435535/article/details/49513703)
* [selector避免重复](https://stackoverflow.com/questions/9939989/java-nio-selector-select-returns-0-although-channels-are-ready)
* [selector和selectedKeys](https://www.cnblogs.com/drizzlewithwind/p/6676172.html)
* [手写JAVA NIO实现Socket通信及其过程中注意的问题](https://blog.csdn.net/ccityzh/article/details/76141562)
* [NIO写服务端判断客户端断开连接的方法](https://blog.csdn.net/cao478208248/article/details/41648513)

### 坑点参考
* [selector 为什么无限触发就绪事件](https://www.jianshu.com/p/6bdee8cfee90)
* [NIO网络编程中重复触发读（写）事件](https://www.cnblogs.com/xdouby/p/8942083.html)
* [关于NIO注册OP_WRITE状态的问题](https://bbs.csdn.net/topics/391817333)
* [NIO就绪处理之OP_WRITE](https://blog.csdn.net/robinjwong/article/details/41912365)

## 依赖
> 仅使用jdk1.8，没有使用第三方库。

## 模块
* MyNIOServer，用于NIO服务端
* MyNIOClient，用于NIO客户端

## 知识点
* Selector
* SelectionKey
* 判断客户端断开连接
> client方异常退出时server会收到一个OP_READ，服务端收到后处理读操作触发异常"远程主机强迫关闭了一个现有的连接"
* 判断服务端断开连接
> server方异常退出时client会收到一个OP_READ，服务端收到后处理读操作触发异常"远程主机强迫关闭了一个现有的连接"


## 难点
> 异常断开连接后的读写操作都会导致异常，所以设计良好的异常处理很重要。本来在client和server中都有errMessage字段用于替代e.printStackTrace，实际操作上会漏掉，这要根据函数的返回值判断，而实际上对外暴露的接口并没有触发异常，而是内部执行导致了异常，对外是不可见的，还需要更好的设计展示。

> 写就绪相对有一点特殊，一般来说，不应该注册写事件。 **写操作的就绪条件为底层缓冲区有空闲空间，而写缓冲区绝大部分时间都是有空闲空间的，所以当你注册写事件后，写操作一直是就绪的，选择处理线程全占用整个CPU资源，这也就是导致一直接收到OP_WRITE的原因**。所以，只有当你确实有数据要写时再注册写操作，并在写完以后马上取消注册。

当有数据在写时，将数据写到缓冲区中，并注册写事件。

```java
public void write(byte[] data) throws IOException {  
    writeBuffer.put(data);  
    key.interestOps(SelectionKey.OP_WRITE);
} 
```

注册写事件后，写操作就绪，这时将之前写入缓冲区的数据写入通道，并取消注册。
```java
channel.write(writeBuffer);  
key.interestOps(key.interestOps() & ~SelectionKey.OP_WRITE);  
```