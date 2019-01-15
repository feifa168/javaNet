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

