# Netty自定义协议栈

协议栈承载了业务内部各模块之间的消息交互和服务调用，主要功能如下：

* 基于Netty的NIO通信框架，提供高性能的异步通信能力
* 提供消息的编解码框架，可以实现POJO的序列化和反序列化
* 提供基于IP地址的白名单接入认证机制
* 链路的有下行校验机制
* 链路的断连重连机制



> 实现一个心跳机制，每隔5秒发送一个心跳包。进行心跳检测前需要完成白名单登录校验



### 1、封装几个自定义的类：

##### 1.1、`Header`：消息头

```java
public class Header
{
    private int version = 0xabef0101;
    private int length;
    private long sessionID;
    private byte type;
    private Map<String, Object> attachments = new HashMap<>();

    public int getVersion()
    {
        return version;
    }

    public void setVersion(int version)
    {
        this.version = version;
    }

    public int getLength()
    {
        return length;
    }

    public void setLength(int length)
    {
        this.length = length;
    }

    public long getSessionID()
    {
        return sessionID;
    }

    public void setSessionID(long sessionID)
    {
        this.sessionID = sessionID;
    }

    public byte getType()
    {
        return type;
    }

    public void setType(byte type)
    {
        this.type = type;
    }

    public Map<String, Object> getAttachments()
    {
        return attachments;
    }

    public void setAttachments(Map<String, Object> attachments)
    {
        this.attachments = attachments;
    }

    @Override
    public String toString()
    {
        return "Header{" + "version=" + version + ", length=" + length + ", sessionID=" + sessionID + ", type=" + type
                + ", attachments=" + attachments + '}';
    }
}
```

##### 1.2、`MyMessage`：消息

```java
public class MyMessage
{
    private Header header;

    private Object body;

    public Header getHeader()
    {
        return header;
    }

    public void setHeader(Header header)
    {
        this.header = header;
    }

    public Object getBody()
    {
        return body;
    }

    public void setBody(Object body)
    {
        this.body = body;
    }

    @Override
    public String toString()
    {
        return "MyMessage{" + "header=" + header + ", body=" + body + '}';
    }
}
```

##### 1.3、`MessageType`：消息类型

```java
public enum MessageType
{
    SERVICE_REQ(0), SERVICE_RESP(1), ONE_WAY(2), LOGIN_REQ(3), LONGIN_RESP(4), HEART_REQ(5), HEART_RESP(6);

    private byte value;

    MessageType(int value)
    {
        this.value = (byte) value;
    }

    public byte getValue()
    {
        return value;
    }

    public void setValue(byte value)
    {
        this.value = value;
    }
}
```



### 2、`MyMessageEncoder`编码器

```java
public class MyMessageEncoder extends MessageToMessageEncoder<MyMessage>
{
    @Override
    protected void encode(ChannelHandlerContext channelHandlerContext, MyMessage msg, List<Object> list)
            throws Exception
    {
        if (null == msg || null == msg.getHeader()){
            throw new Exception("要编码的消息为空");
        }
        ByteBuf sendBuf = Unpooled.buffer();//分配一个空间
        //写内容
        sendBuf.writeInt(msg.getHeader().getVersion());
        sendBuf.writeInt(msg.getHeader().getLength());
        sendBuf.writeLong(msg.getHeader().getSessionID());
        sendBuf.writeByte(msg.getHeader().getType());
        //处理attachment
        byte[] attachByte = toByteArray(msg.getHeader().getAttachments());
        sendBuf.writeInt(attachByte.length);
        sendBuf.writeBytes(attachByte);

        //Java对象的序列化
        if (null != msg.getBody()){
            byte[] bodyByte = toByteArray(msg.getBody());
            sendBuf.writeInt(bodyByte.length);
            sendBuf.writeBytes(bodyByte);
        }else {
            sendBuf.writeInt(0);
        }
        //设置长度
        sendBuf.setInt(4,sendBuf.readableBytes());
        list.add(sendBuf);
    }

    private byte[] toByteArray(Object object)
    {
        byte[] bytes = null;
        ByteArrayOutputStream bos = null;
        ObjectOutputStream oos = null;
        try
        {
            bos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(bos);
            oos.writeObject(object);
            oos.flush();
            bytes = bos.toByteArray();
            return bytes;
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != bos){
                try
                {
                    bos.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            if (null != oos){
                try
                {
                    oos.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
```

### 3、`MyMessageDecoder`：解码器

```java
public class MyMessageDecoder extends LengthFieldBasedFrameDecoder
{
    public MyMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength)
    {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength);
    }

    public MyMessageDecoder(int maxFrameLength, int lengthFieldOffset, int lengthFieldLength, int lengthAdjustment,
            int initialBytesToStrip)
    {
        super(maxFrameLength, lengthFieldOffset, lengthFieldLength, lengthAdjustment, initialBytesToStrip);
    }

    @Override
    protected Object decode(ChannelHandlerContext ctx, ByteBuf in) throws Exception
    {
        //父类先进行解析，处理粘包拆包工作
        ByteBuf frame = (ByteBuf) super.decode(ctx,in);
        //如果不足一个数据包，则不作处理
        if (null == frame){
            return frame;
        }
        //数据解码
        MyMessage myMessage = new MyMessage();
        Header header = new Header();
        header.setVersion(frame.readInt());
        header.setLength(frame.readInt());
        header.setSessionID(frame.readLong());
        header.setType(frame.readByte());

        int attachmentsSize = frame.readInt();
        if (attachmentsSize == 0){
            //如果没有附件信息，则不用处理
            header.setAttachments(new HashMap<String, Object>());
        }else {
            ByteBuf buf = frame.readBytes(attachmentsSize);
            byte[] bytes = new byte[attachmentsSize];
            buf.readBytes(bytes);
            Map<String, Object> attachs = (Map<String,Object>)toObject(bytes);
            header.setAttachments(attachs);
        }
        myMessage.setHeader(header);
        int bodyLength = frame.readInt();
        if (bodyLength != 0){
            byte[] bytes = new byte[bodyLength];
            frame.readBytes(bytes);
            Object obj = toObject(bytes);
            myMessage.setBody(obj);
        }
        return myMessage;
    }

    private Object toObject(byte[] bytes)
    {
        Object obj = null;
        ByteArrayInputStream bis = null;
        ObjectInputStream ois = null;
        try
        {
            bis = new ByteArrayInputStream(bytes);
            ois = new ObjectInputStream(bis);
            obj = ois.readObject();
            return obj;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }
        catch (ClassNotFoundException e)
        {
            e.printStackTrace();
        }
        finally
        {
            if (null != ois){
                try
                {
                    ois.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
            if (null != bis){
                try
                {
                    bis.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
        return null;
    }
}
```

### 4、业务处理Handler

#### 4.1、`LoginAuthRespHandler`：客户端登录请求响应处理类

```java
public class LoginAuthRespHandler extends ChannelInboundHandlerAdapter
{
    @Override public void channelActive(ChannelHandlerContext ctx) throws Exception
    {
        MyMessage msg = buildLoginReq();
        ctx.writeAndFlush(msg);
    }

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        MyMessage myMessage = (MyMessage) msg;
        if (null != myMessage.getHeader() && MessageType.LONGIN_RESP.getValue() == myMessage.getHeader()
                .getType()){
            //只处理登录请求
            byte loginResult = (byte) myMessage.getBody();
            if (loginResult != 0){
                ctx.close();//登录失败
            }else {
                System.out.println("登录成功");
                ctx.fireChannelRead(msg);
            }
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        cause.printStackTrace();
        ctx.fireExceptionCaught(cause);
    }

    private MyMessage buildLoginReq(){
        MyMessage myMessage = new MyMessage();
        Header header = new Header();
        header.setType(MessageType.LOGIN_REQ.getValue());
        myMessage.setHeader(header);
        return myMessage;
    }
}
```

#### 4.2、`LoginAuthReqHandler`：服务端登录请求处理类

```java
public class LoginAuthReqHandler extends ChannelInboundHandlerAdapter
{
    //记录登录地址
    private Map<String, Boolean> node = new ConcurrentHashMap<>();

    //模拟IP白/黑名单
    private String[] whiteList = {"127.0.0.1","10.62.58.219"};

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        MyMessage myMessage = (MyMessage) msg;
        if (null != myMessage.getHeader() && MessageType.LOGIN_REQ.getValue() == myMessage.getHeader().getType
                ()){
            String ip = ctx.channel().remoteAddress().toString();
            MyMessage resp = null;
            if (node.containsKey(ip)){
                //如果是在白名单中
                resp = buildResp((byte) -1);
            }else {
                InetSocketAddress socketAddress = (InetSocketAddress) ctx.channel().remoteAddress();
                String rip = socketAddress.getAddress().getHostAddress();
                boolean isOk = false;
                for (String s : whiteList){
                    if (s.equals(rip)){
                        isOk = true;
                        break;
                    }
                }
                resp = isOk ? buildResp((byte)0) : buildResp((byte)-1);
                if (isOk){
                    node.put(ip, true);
                }
            }
            System.out.println("the login response is " + resp.toString() + " ; body is " + resp.getBody());
            ctx.writeAndFlush(resp);
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        node.remove(ctx.channel().remoteAddress().toString());
        ctx.fireExceptionCaught(cause);
    }

    private MyMessage buildResp(byte type)
    {
        MyMessage myMessage = new MyMessage();

        Header header = new Header();
        header.setType(MessageType.LONGIN_RESP.getValue());
        myMessage.setHeader(header);
        myMessage.setBody(type);
        return myMessage;
    }
}
```

#### 4.3、`HeartBeatRespHandler`：客户端心跳请求响应处理类

```java
public class HeartBeatRespHandler extends ChannelInboundHandlerAdapter
{
    private volatile ScheduledFuture<?> heartBeat;

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        MyMessage myMessage = (MyMessage) msg;
        if (null != myMessage.getHeader() && MessageType.LONGIN_RESP.getValue() == myMessage.getHeader().getType()){
            heartBeat = ctx.executor().scheduleAtFixedRate(new HeartBeatTask(ctx), 0 , 5, TimeUnit.SECONDS);
        }else if (null != myMessage.getHeader() && MessageType.HEART_RESP.getValue() == myMessage.getHeader().getType()){
            System.out.println("client received heart beat message -->>" + myMessage);
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        if (null != heartBeat){
            heartBeat.cancel(true);
            heartBeat = null;
        }
        ctx.fireExceptionCaught(cause);
    }

    private class HeartBeatTask implements Runnable{

        private final ChannelHandlerContext ctx;

        public HeartBeatTask(ChannelHandlerContext ctx)
        {
            this.ctx = ctx;
        }

        @Override public void run()
        {
            MyMessage myMessage = buildHeartBeat();
            System.out.println("client sent heart beat to server : >>> " + myMessage);
            ctx.writeAndFlush(myMessage);
        }
    }

    private MyMessage buildHeartBeat()
    {
        MyMessage myMessage = new MyMessage();

        Header header = new Header();
        header.setType(MessageType.HEART_REQ.getValue());

        myMessage.setHeader(header);

        return myMessage;
    }
}
```

#### 4.4、`HeartBeatReqHandler`：服务端心跳请求处理类

```java
public class HeartBeatReqHandler extends ChannelInboundHandlerAdapter
{

    @Override public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        MyMessage myMessage = (MyMessage) msg;
        if (null != myMessage.getHeader() && MessageType.HEART_REQ.getValue() == myMessage.getHeader().getType()){
            System.out.println("received heart beat message :--- >>>" + myMessage.toString());
            MyMessage heartBeat = buildHeartBeat();
            System.out.println("send heart beat response message to client: --->>>" + heartBeat.toString());
            ctx.writeAndFlush(heartBeat);
        }else {
            ctx.fireChannelRead(msg);
        }
    }

    private MyMessage buildHeartBeat()
    {
        MyMessage myMessage = new MyMessage();

        Header header = new Header();
        header.setType(MessageType.HEART_RESP.getValue());

        myMessage.setHeader(header);

        return myMessage;
    }
}
```

### 5、服务端

`server`

```java
public class Server
{
    private EventLoopGroup bossGroup = new NioEventLoopGroup();
    private EventLoopGroup workGroup = new NioEventLoopGroup();
    private int port;
    private String host;

    public Server(int port, String host)
    {
        this.port = port;
        this.host = host;
    }

    public static void main(String[] args)
    {
        Server server = new Server(8099,"localhost");
        server.bind();
    }

    public void bind(){
        ServerBootstrap serverBootstrap = new ServerBootstrap();
        serverBootstrap.group(bossGroup, workGroup)
                .channel(NioServerSocketChannel.class)
                .option(ChannelOption.SO_BACKLOG,256)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>()
                {
                    @Override protected void initChannel(SocketChannel channel) throws Exception
                    {
                        channel.pipeline().addLast(new MyMessageDecoder(1024 * 1024,4 , 4 ,-8,0));
                        channel.pipeline().addLast(new MyMessageEncoder());
                        channel.pipeline().addLast("readTimeOutHandler",new ReadTimeoutHandler(50));
                        channel.pipeline().addLast("loginAuthReqHandler",new LoginAuthRespHandler());
                        channel.pipeline().addLast("heartBeatReqHandler",new HeartBeatRespHandler());
                    }
                });
        try
        {
            ChannelFuture future = serverBootstrap.bind(port).sync();
            future.channel().closeFuture().sync();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }finally
        {
            bossGroup.shutdownGracefully();
            workGroup.shutdownGracefully();
        }
    }
}
```

### 6、客户端

`Client`

```java
public class Client
{
    private int port;
    private String host;
    private ScheduledExecutorService service = Executors.newScheduledThreadPool(1);

    public Client(int port, String host)
    {
        this.port = port;
        this.host = host;
    }

    public static void main(String[] args)
    {
        Client client = new Client(8099,"localhost");
        client.start();
    }

    private void start(){
        NioEventLoopGroup group = new NioEventLoopGroup();
        Bootstrap bootstrap = new Bootstrap();
        try
        {
            bootstrap.group(group)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.TCP_NODELAY,true)
                    .handler(new ChannelInitializer<SocketChannel>()
                    {
                        @Override protected void initChannel(SocketChannel channel) throws Exception
                        {
                            channel.pipeline().addLast(new MyMessageDecoder(1024 * 1024, 4, 4, -8, 0));
                            channel.pipeline().addLast("messageEncoder",new MyMessageEncoder());
                            channel.pipeline().addLast("readTimeOutHandler", new ReadTimeoutHandler(50));
                            channel.pipeline().addLast("loginAuthHandler", new LoginAuthRespHandler());
                            channel.pipeline().addLast("headtBeatHandler", new HeartBeatRespHandler());
                        }
                    });
            bootstrap.remoteAddress(host,port);
            ChannelFuture future = bootstrap.connect().sync();
            future.channel().closeFuture().sync();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }finally
        {
            service.execute(new Runnable()
            {
                @Override public void run()
                {
                    try
                    {
                        TimeUnit.SECONDS.sleep(5);
                        start();
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                    }
                }
            });
        }
    }
}
```



运行结果：

客户端：

```
登录成功
client sent heart beat to server : >>> MyMessage{header=Header{version=-1410399999, length=0, sessionID=0, type=5, attachments={}}, body=null}
client received heart beat message -->>MyMessage{header=Header{version=-1410399999, length=107, sessionID=0, type=6, attachments={}}, body=null}
```

服务端：

```
the login response is MyMessage{header=Header{version=-1410399999, length=0, sessionID=0, type=4, attachments={}}, body=0} ; body is 0
received heart beat message :--- >>>MyMessage{header=Header{version=-1410399999, length=107, sessionID=0, type=5, attachments={}}, body=null}
send heart beat response message to client: --->>>MyMessage{header=Header{version=-1410399999, length=0, sessionID=0, type=6, attachments={}}, body=null}
```

每隔5s发送一个心跳包，一切正常
