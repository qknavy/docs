## 一、基础扩展

### 1、Unix5种IO模型：

* 阻塞I/O

  > 应用程序 -(系统调用)-> 内核无数据报准备好-(等待数据准备就绪)->数据准备好-(数据从内核复制到用户空间)->复制完成-(返回成功)->处理数据包

* 非阻塞I/O

  > 系统反复调用（轮询）检查是否有数据，没有数据直接返回EWOULDBLACK错误，直到数据准备就绪，就从内核复制到用户空间然后返回，客户端再处理

* I/O复用

  > * select/pull模型，系统将一个或者多个fd传递给select或poll，select/pull侦测多个fd是否是就绪状态，但是fd数量有限，所以使用起来会有一些限制。
  > * Linux提供一个基于事件驱动而不是顺序扫描的epoll模型，性能更高，当有fd就绪的时候立即返回callback

* 信号驱动I/O

  > 非阻塞的。系统调用执行一个信号处理函数，此系统立即返回，进程继续工作，当有数据准备就绪的时候通过信号通知应用程序调用函数来获取相应数据，并通知主函数处理数据

* 异步I/O

  > 告知内核完成什么操作，并让内核在整个操作完成之后（包括数据从内核空间复制到用户空间）通知我们。和信号驱动不同是异步IO是通知我们什么时候操作已经完成，而信号驱动IO是通知我们什么时候开始IO操作



### 2、Java I/O的演进

`BIO`：

> BIO是同步阻塞IO，一问一答的模型简化了上层应用的开发，但是性能和可靠性方面存在很大的瓶颈。

`NIO`：

> jdk1.4引入NIO的支持。



```sequence
NIOServer -> Reactor Thread :1、打开ServerSocketChannel\n2、绑定监听地址InetSocketAddress
Reactor Thread -> IOHandler :3、创建Selector，启动线程
NIOServer -> Reactor Thread :4、将ServerSocketChannel注册到Selector，监听
Reactor Thread -> IOHandler :5、Selector轮询就绪的key
IOHandler -> Reactor Thread :6、handlerAccept处理新的客户端接入
```



### 3、处理粘包/拆包问题【解码器】

* LineBasedFrameDecoder
* DelimiterBasedFrameDecoder
* FixedLengthFrameDecoder
* StringDecoder





## 二、编解码

1、几种主流的编解码协议

* Google的`protobuf`

  > 默认支持c++，Java和Python三种语言，序列化后的字节很小，适合网络传输，序列化和反序列化的效率都非常高，它会把对象转为一个`.proto`后缀的文件

* Facebook的`Thrift`

  > 2007年贡献给Apache基金会，支持的语言比较多：c++、c#、Erlang、Java、Perl、PHP、Python、Ruby等等...，比较适合多语言系统间的通信。同时它还支持多种类型的rpc服务，所以适合用于搭建大型的数据交换及存
  >
  > 但是因为每次数据结构发生变化的时候需要重新生成IDL文件，所以比较适合静态的数据交换

  Thrift5部分组成：

  * 语言系统及IDL编译器
  * TProtocol：
  * TTransport
  * TProcessor：
  * TServer：

  > Thrift支持的数据格式包括Java的八种基本数据类型和Map、Set、List，同时支持可选和必选定义

* JBoss的`Marshalling`

  > Java对象即使没有实现Serializable接口也能实现序列化，通过缓存技术提升序列化和反序列化的性能

* MessagePack

  > 编解码性能高，序列化后的字节码流小，支持跨语言

```xml
<dependency>
    <groupId>org.msgpack</groupId>
    <artifactId>msgpack</artifactId>
    <version>${msgpack.version}</version>
</dependency>
```

