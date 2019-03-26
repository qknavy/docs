# Kafka

## 1、Kafka介绍

> Kafka产生的背景
>
> Kafka的架构
>
> Kafka的安装部署及集群部署
>
> Kafka的基本操作
>
> Kafka的应用

Kafka是一款分布式消息及订阅系统。（领英公司，scale语言编写，基于jvm但不是基于jmx规范实现）

### 1.1、Kafka的特点和应用场景

**特点：**

* 高性能
* 高吞吐量

内置分区、实现集群

**应用场景：**

> * 用户行为跟踪
> * 日志收集（ELK）



### 1.2、Kafka的架构

生产者发送消息到broker，消费者主动从broker拉取消息

而amq是主动推送到消费端



### 1.3、基本概念：

topic：主题，Kafka中没有queue的概念（类似一张大的数据表）

partition：数据分区。（类似一张分表）

group：分组。每个消息都有一个所属的分组



### 1.4、基本操作：

#### 1.4.1、启动

Kafka-server-start.sh ../conf/server.properties



#### 1.4.2、创建主题：

```
sh kafka-topics.sh --create --zookeeper localhost:2181 --replication-factor 1 --partitions 1 --topic test
```

上面命令创建了一个名称为test的主题

#### 1.4.3、查看当前topic列表

```
sh kafka-topic.sh --list --zookeeper localhost:2181
```

#### 1.4.4、发送消息

```
sh kafka-console-producer.sh --broker-list localhost:9092 --topic test
```

上面命令通过控制台忘9092的broker节点上的主题名为test的主题发送一条消息

#### 1.4.5、开启一个consumer接收消息

```
sh kafka-console-consumer.sh --bootstrap-server localhost:9092 --topic test --from-beginning
```

上面的命令可以接收localhost:9092这个broker从头开始的所有消息，包括test主题的历史消息和后续的所有消息

```
D:\toos\kafka-standalone\bin\windows>kafka-console-producer --broker-list localhost:9092 --topic test
>hello,my name is Navy
```

消费端：

```
D:\toos\kafka-standalone\bin\windows>kafka-console-consumer --bootstrap-server localhost:9092 --topic test --from-beginning
hello,my name is Navy
```

可以看到正常接收消息



## 2、**集群搭建**：

* 修改config目录下server.properties中的`zookeeper.connect`地址

  > 如果是zk集群的话写多个zk地址，逗号隔开

* 修改config目录下server.properties中的`broker.id`

* 配置`server.properties`的listeners

```
listeners=PLAINTEXT://10.62.58.219:9092
```

这里必须要写当前主机的IP，而不能写成localhost，因为如果节点不在同一个主机的话就无法完成通信。

启动集群的节点，就可以在zk上看到自动生成了一些zk节点信息

* brokers：该节点下保存了ids、topics、seqid等

  * ids：集群存活的所有的broker.id
  * topics：主题
  * seqid

* controller：通过zk的get命令可以查看该节点信息

  ```
  [zk: localhost:2181(CONNECTED) 19] get /controller
  {"version":1,"brokerid":0,"timestamp":"1553588687912"}
  cZxid = 0x522
  ctime = Tue Mar 26 16:24:47 GMT+08:00 2019
  mZxid = 0x522
  mtime = Tue Mar 26 16:24:47 GMT+08:00 2019
  pZxid = 0x522
  cversion = 0
  dataVersion = 0
  aclVersion = 0
  ephemeralOwner = 0x169b919deac0000
  dataLength = 54
  numChildren = 0
  ```

  这里的brokerid就是集群leader节点的id





## 3、API的使用（demo）：

#### 3.1、引入依赖

```xml
<dependency>
    <groupId>org.apache.kafka</groupId>
    <artifactId>kafka-clients</artifactId>
	<version>2.1.0</version>
</dependency>
```

3.2、producer

```java
public class Producer extends Thread
{
    private final KafkaProducer<Integer, String> producer;
    private final String topic;
    public Producer(String topic)
    {
        this.topic = topic;
        Properties properties = new Properties();
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG,"10.62.58.219:9090,10.62.58.219:9091,10.62.58.219:9092");
        properties.put(ProducerConfig.CLIENT_ID_CONFIG,"KafkaProducerDemo");
        properties.put(ProducerConfig.ACKS_CONFIG,"-1");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.IntegerSerializer");
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization"
                + ".StringSerializer");
        producer = new KafkaProducer<Integer, String>(properties);
    }

    public static void main(String[] args)
    {
        new Thread(new Producer("test")).start();
    }

    @Override public void run()
    {
        int num = 0;
        while (num < 50){
            String msg = "message_" + num;
            System.out.println("begin sending msg[ " + msg + " ]");
            producer.send(new ProducerRecord<>(topic,msg));
            num ++;
            try
            {
                TimeUnit.SECONDS.sleep(1);
            }
            catch (InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }
}
```



#### 3.2、Consumer

```java
public class Consumer extends Thread
{
    private final KafkaConsumer<Integer, String> consumer;
    public Consumer(String topic)
    {
        Properties properties = new Properties();
        properties.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,"10.62.58.219:9090,10.62.58.219:9091,10.62.58.219:9092");
        properties.put(ConsumerConfig.GROUP_ID_CONFIG,"KafkaConsumerDemo");
        properties.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG,"true");
        properties.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG,"1000");
        properties.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization.IntegerDeserializer");
        properties.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,"org.apache.kafka.common.serialization"
                + ".StringDeserializer");
        properties.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG,"earliest");
        consumer = new KafkaConsumer<Integer, String>(properties);
        consumer.subscribe(Collections.singleton(topic));
    }

    public static void main(String[] args)
    {
        new Consumer("test").start();
    }

    @Override public void run()
    {
        while (true){
            ConsumerRecords<Integer, String> records = consumer.poll(1000);
            for (ConsumerRecord consumerRecord : records)
            {
                System.out.println("message received : [ " + consumerRecord.value() +" ]");
            }
        }
    }
}
```

运行`producer`发送消息，然后运行`consumer`接收消息

```
begin sending msg[ message_0 ]
begin sending msg[ message_1 ]
...
```

```
message received : [ message_0 ]
message received : [ message_1 ]
...
```

一切正常
