# Netty+WebSocket

### 1、引入Netty依赖

```xml
<dependency>
    <groupId>io.netty</groupId>
    <artifactId>netty-all</artifactId>
	<version>5.0.0.Alpha2</version>
</dependency>
```



### 2、服务端代码

##### `启动类`

```java
public class WebSocketServer
{
    public static void main(String[] args)
    {
        new WebSocketServer().bind(8099);
    }

    private void bind(int port)
    {
        EventLoopGroup bossGroup = new NioEventLoopGroup();
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        ServerBootstrap server = new ServerBootstrap();

        server.group(bossGroup,workerGroup)
                .option(ChannelOption.SO_BACKLOG,1024)
                .channel(NioServerSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .childHandler(new ChannelInitializer<SocketChannel>()
                {
                    @Override
                    protected void initChannel(SocketChannel channel) throws Exception
                    {
                        ChannelPipeline pipeline = channel.pipeline();
                        pipeline.addLast("http-codec",new HttpServerCodec());
                        pipeline.addLast("aggregator",new HttpObjectAggregator(65536));
                        pipeline.addLast("http-chunked",new ChunkedWriteHandler());
                        channel.pipeline().addLast("handler", new WebSocketServerHandler(port));
                    }
                });
        try
        {
            ChannelFuture future = server.bind(port).sync();
            System.out.println("Web socket server started at port : " + port + ".");
            System.out.println("Open your browser and navigate to http://localhost:" + port + "/");
            future.channel().closeFuture().sync();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }finally
        {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
```

##### `处理类`

```java
public class WebSocketServerHandler extends SimpleChannelInboundHandler<Object>
{

    private int port;

    private WebSocketServerHandshaker handshaker;

    public WebSocketServerHandler(int port){
        this.port = port;
    }
    @Override
    protected void messageReceived(ChannelHandlerContext ctx, Object msg) throws Exception
    {
        if (msg instanceof FullHttpRequest){
            //传统的http请求
            handleHttpRequest(ctx,(FullHttpRequest)msg);
        }else if (msg instanceof WebSocketFrame){
            //websocket请求
            handleWebSocketFrame(ctx,(WebSocketFrame)msg);
        }
    }

    /**
     * 处理WebSocket请求
     * @param ctx
     * @param frame
     */
    private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame)
    {
        //首先判断是否是关闭链路的指令
        if (frame instanceof CloseWebSocketFrame){
            handshaker.close(ctx.channel(),(CloseWebSocketFrame)frame.retain());
            return;
        }
        //判断是否是Ping消息
        if (frame instanceof PingWebSocketFrame){
            ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
            return;
        }
        if (!(frame instanceof TextWebSocketFrame)){
            //demo只支持文本消息，不支持二进制
            throw new UnsupportedOperationException(String.format("%s frame types not supported",frame.getClass()
                    .getName()));
        }
        //返回应答消息
        String request = ((TextWebSocketFrame)frame).text();
        System.out.println(String.format("%s received %s",ctx.channel(),request));
        ctx.channel().write(new TextWebSocketFrame(request + " ，欢迎使用Netty WebSocket服务，现在时刻：" + new Date().toString()));
    }

    /**
     * 处理传统的http请求
     * @param ctx
     * @param msg
     */
    private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest msg)
    {
        if (!msg.decoderResult().isSuccess() || (!"websocket".equals(msg.headers().get("Upgrade")))){
            //如果http解码失败，返回http异常
            sendHttpResponse(ctx,msg,new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
            return;
        }
        //构造握手响应返回。
        WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory
                ("ws://localhost:"+port+"/websocket",
                null,
                false);

        handshaker = wsFactory.newHandshaker(msg);
        if (null == handshaker){
            WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
        }else {
            handshaker.handshake(ctx.channel(),msg);
        }
    }

    /**
     * 返回响应信息
     * @param ctx
     * @param request
     * @param response
     */
    private void sendHttpResponse(ChannelHandlerContext ctx, FullHttpRequest request,
            DefaultFullHttpResponse response)
    {
        if (200 != response.status().code()){
            ByteBuf buf = Unpooled.copiedBuffer(response.status().toString(), CharsetUtil.UTF_8);
            response.content().writeBytes(buf);
            buf.release();
            setContentLength(response,response.content().readableBytes());
        }
        //如果是非长连接，则关闭连接
        ChannelFuture future = ctx.channel().writeAndFlush(response);
        if (! isKeepAlive(request) || response.status().code() != 200){
            future.addListener(ChannelFutureListener.CLOSE);
        }
    }

    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception
    {
        ctx.flush();
    }

    @Override public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception
    {
        System.err.println("出错啦！！！");
        ctx.close();
    }
}
```



### 3、客户端代码（页面）

```html
<html>
	<head>
		<meta charset="utf-8"/>
		<title>Netty WebSocket 时间服务器</title>
		
		<script type="text/javascript">
			var socket;
			if(!window.WebSocket){
				window.WebSocket = window.MozWebSocket;
			}
			if(window.WebSocket){
				socket = new WebSocket("ws://localhost:8099/websocket");
				socket.onmessage=function(event){
					var ta = document.getElementById('responseText');
					ta.value="";
					ta.value=event.data;
				};
				socket.onopen = function(event){
					var ta = document.getElementById('responseText');
					ta.value = "打开WebSocket服务正常，浏览器支持WebSocket!";
				};
				socket.onclose = function(event){
					var ta = document.getElementById('responseText');
					ta.value = "";
					ta.value = "WebSocket 关闭!";
				};
			}else {
				alert("抱歉，您的浏览器不支持WebSocket协议");
			}
			
			
			function send(message){
				if(!window.WebSocket){
					return;
				}
				if(socket.readyState == WebSocket.OPEN){
					socket.send(message);
				}else{
					alert("WebSocket 连接没有建立成功");
				}
			}
		</script>
	</head>
	<body>
		
		
		<form onsubmit="return false;">
			<input type="text" name="message" value="Netty 最佳实战"/>
			<br/><br/>
			<input type="button" value="发送Websocket请求消息" onclick="send(this.form.message.value);"/>
			<hr color="blue"/>
			
			<h3>服务端返回的应答消息</h3>
			<textarea id="responseText" style="width:500px;height:300px;">
			</textarea>
		</form>
	</body>
</html>
```

### 4、启动服务，然后打开页面就可以看到效果
