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

### 3.1、引入依赖

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



### 3.2、Consumer

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

### 3.3、异步发送消息

KafkaProducer类的发送消息有几个重载的方法：

* public Future<RecordMetadata> send(ProducerRecord<K, V> record)
* public Future<RecordMetadata> send(ProducerRecord<K, V> record, Callback callback)

在新版本的kafka中，默认发送消息采用异步的方式发送，如果我们需要采用同步的方式，则可以利用Future个get方法阻塞完成同步。

### 3.4、参数说明

#### 3.4.1、生产者参数

```java
public static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";//kafka集群地址
public static final String CLIENT_DNS_LOOKUP_CONFIG = "client.dns.lookup";
public static final String METADATA_MAX_AGE_CONFIG = "metadata.max.age.ms";
private static final String METADATA_MAX_AGE_DOC = "The period of time in milliseconds after which we force a refresh of metadata even if we haven't seen any partition leadership changes to proactively discover any new brokers or partitions.";
public static final String BATCH_SIZE_CONFIG = "batch.size";
private static final String BATCH_SIZE_DOC = "The producer will attempt to batch records together into fewer requests whenever multiple records are being sent to the same partition. This helps performance on both the client and the server. This configuration controls the default batch size in bytes. <p>No attempt will be made to batch records larger than this size. <p>Requests sent to brokers will contain multiple batches, one for each partition with data available to be sent. <p>A small batch size will make batching less common and may reduce throughput (a batch size of zero will disable batching entirely). A very large batch size may use memory a bit more wastefully as we will always allocate a buffer of the specified batch size in anticipation of additional records.";
public static final String ACKS_CONFIG = "acks";
private static final String ACKS_DOC = "The number of acknowledgments the producer requires the leader to have received before considering a request complete. This controls the  durability of records that are sent. The following settings are allowed:  <ul> <li><code>acks=0</code> If set to zero then the producer will not wait for any acknowledgment from the server at all. The record will be immediately added to the socket buffer and considered sent. No guarantee can be made that the server has received the record in this case, and the <code>retries</code> configuration will not take effect (as the client won't generally know of any failures). The offset given back for each record will always be set to -1. <li><code>acks=1</code> This will mean the leader will write the record to its local log but will respond without awaiting full acknowledgement from all followers. In this case should the leader fail immediately after acknowledging the record but before the followers have replicated it then the record will be lost. <li><code>acks=all</code> This means the leader will wait for the full set of in-sync replicas to acknowledge the record. This guarantees that the record will not be lost as long as at least one in-sync replica remains alive. This is the strongest available guarantee. This is equivalent to the acks=-1 setting.";
public static final String LINGER_MS_CONFIG = "linger.ms";
private static final String LINGER_MS_DOC = "The producer groups together any records that arrive in between request transmissions into a single batched request. Normally this occurs only under load when records arrive faster than they can be sent out. However in some circumstances the client may want to reduce the number of requests even under moderate load. This setting accomplishes this by adding a small amount of artificial delay&mdash;that is, rather than immediately sending out a record the producer will wait for up to the given delay to allow other records to be sent so that the sends can be batched together. This can be thought of as analogous to Nagle's algorithm in TCP. This setting gives the upper bound on the delay for batching: once we get <code>batch.size</code> worth of records for a partition it will be sent immediately regardless of this setting, however if we have fewer than this many bytes accumulated for this partition we will 'linger' for the specified time waiting for more records to show up. This setting defaults to 0 (i.e. no delay). Setting <code>linger.ms=5</code>, for example, would have the effect of reducing the number of requests sent but would add up to 5ms of latency to records sent in the absence of load.";
public static final String REQUEST_TIMEOUT_MS_CONFIG = "request.timeout.ms";
private static final String REQUEST_TIMEOUT_MS_DOC = "The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted. This should be larger than <code>replica.lag.time.max.ms</code> (a broker configuration) to reduce the possibility of message duplication due to unnecessary producer retries.";
public static final String DELIVERY_TIMEOUT_MS_CONFIG = "delivery.timeout.ms";
private static final String DELIVERY_TIMEOUT_MS_DOC = "An upper bound on the time to report success or failure after a call to <code>send()</code> returns. This limits the total time that a record will be delayed prior to sending, the time to await acknowledgement from the broker (if expected), and the time allowed for retriable send failures. The producer may report failure to send a record earlier than this config if either an unrecoverable error is encountered, the retries have been exhausted, or the record is added to a batch which reached an earlier delivery expiration deadline. The value of this config should be greater than or equal to the sum of <code>request.timeout.ms</code> and <code>linger.ms</code>.";
public static final String CLIENT_ID_CONFIG = "client.id";
public static final String SEND_BUFFER_CONFIG = "send.buffer.bytes";
public static final String RECEIVE_BUFFER_CONFIG = "receive.buffer.bytes";
public static final String MAX_REQUEST_SIZE_CONFIG = "max.request.size";
private static final String MAX_REQUEST_SIZE_DOC = "The maximum size of a request in bytes. This setting will limit the number of record batches the producer will send in a single request to avoid sending huge requests. This is also effectively a cap on the maximum record batch size. Note that the server has its own cap on record batch size which may be different from this.";
public static final String RECONNECT_BACKOFF_MS_CONFIG = "reconnect.backoff.ms";
public static final String RECONNECT_BACKOFF_MAX_MS_CONFIG = "reconnect.backoff.max.ms";
public static final String MAX_BLOCK_MS_CONFIG = "max.block.ms";
private static final String MAX_BLOCK_MS_DOC = "The configuration controls how long <code>KafkaProducer.send()</code> and <code>KafkaProducer.partitionsFor()</code> will block.These methods can be blocked either because the buffer is full or metadata unavailable.Blocking in the user-supplied serializers or partitioner will not be counted against this timeout.";
public static final String BUFFER_MEMORY_CONFIG = "buffer.memory";
private static final String BUFFER_MEMORY_DOC = "The total bytes of memory the producer can use to buffer records waiting to be sent to the server. If records are sent faster than they can be delivered to the server the producer will block for <code>max.block.ms</code> after which it will throw an exception.<p>This setting should correspond roughly to the total memory the producer will use, but is not a hard bound since not all memory the producer uses is used for buffering. Some additional memory will be used for compression (if compression is enabled) as well as for maintaining in-flight requests.";
public static final String RETRY_BACKOFF_MS_CONFIG = "retry.backoff.ms";
public static final String COMPRESSION_TYPE_CONFIG = "compression.type";
private static final String COMPRESSION_TYPE_DOC = "The compression type for all data generated by the producer. The default is none (i.e. no compression). Valid  values are <code>none</code>, <code>gzip</code>, <code>snappy</code>, <code>lz4</code>, or <code>zstd</code>. Compression is of full batches of data, so the efficacy of batching will also impact the compression ratio (more batching means better compression).";
public static final String METRICS_SAMPLE_WINDOW_MS_CONFIG = "metrics.sample.window.ms";
public static final String METRICS_NUM_SAMPLES_CONFIG = "metrics.num.samples";
public static final String METRICS_RECORDING_LEVEL_CONFIG = "metrics.recording.level";
public static final String METRIC_REPORTER_CLASSES_CONFIG = "metric.reporters";
public static final String MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION = "max.in.flight.requests.per.connection";
private static final String MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION_DOC = "The maximum number of unacknowledged requests the client will send on a single connection before blocking. Note that if this setting is set to be greater than 1 and there are failed sends, there is a risk of message re-ordering due to retries (i.e., if retries are enabled).";
public static final String RETRIES_CONFIG = "retries";
private static final String RETRIES_DOC = "Setting a value greater than zero will cause the client to resend any record whose send fails with a potentially transient error. Note that this retry is no different than if the client resent the record upon receiving the error. Allowing retries without setting <code>max.in.flight.requests.per.connection</code> to 1 will potentially change the ordering of records because if two batches are sent to a single partition, and the first fails and is retried but the second succeeds, then the records in the second batch may appear first. Note additionall that produce requests will be failed before the number of retries has been exhausted if the timeout configured by <code>delivery.timeout.ms</code> expires first before successful acknowledgement. Users should generally prefer to leave this config unset and instead use <code>delivery.timeout.ms</code> to control retry behavior.";
public static final String KEY_SERIALIZER_CLASS_CONFIG = "key.serializer";
public static final String KEY_SERIALIZER_CLASS_DOC = "Serializer class for key that implements the <code>org.apache.kafka.common.serialization.Serializer</code> interface.";
public static final String VALUE_SERIALIZER_CLASS_CONFIG = "value.serializer";
public static final String VALUE_SERIALIZER_CLASS_DOC = "Serializer class for value that implements the <code>org.apache.kafka.common.serialization.Serializer</code> interface.";
public static final String CONNECTIONS_MAX_IDLE_MS_CONFIG = "connections.max.idle.ms";
public static final String PARTITIONER_CLASS_CONFIG = "partitioner.class";
private static final String PARTITIONER_CLASS_DOC = "Partitioner class that implements the <code>org.apache.kafka.clients.producer.Partitioner</code> interface.";
public static final String INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes";
public static final String INTERCEPTOR_CLASSES_DOC = "A list of classes to use as interceptors. Implementing the <code>org.apache.kafka.clients.producer.ProducerInterceptor</code> interface allows you to intercept (and possibly mutate) the records received by the producer before they are published to the Kafka cluster. By default, there are no interceptors.";
public static final String ENABLE_IDEMPOTENCE_CONFIG = "enable.idempotence";
public static final String ENABLE_IDEMPOTENCE_DOC = "When set to 'true', the producer will ensure that exactly one copy of each message is written in the stream. If 'false', producer retries due to broker failures, etc., may write duplicates of the retried message in the stream. Note that enabling idempotence requires <code>max.in.flight.requests.per.connection</code> to be less than or equal to 5, <code>retries</code> to be greater than 0 and <code>acks</code> must be 'all'. If these values are not explicitly set by the user, suitable values will be chosen. If incompatible values are set, a <code>ConfigException</code> will be thrown.";
public static final String TRANSACTION_TIMEOUT_CONFIG = "transaction.timeout.ms";
public static final String TRANSACTION_TIMEOUT_DOC = "The maximum amount of time in ms that the transaction coordinator will wait for a transaction status update from the producer before proactively aborting the ongoing transaction.If this value is larger than the transaction.max.timeout.ms setting in the broker, the request will fail with a <code>InvalidTransactionTimeout</code> error.";
public static final String TRANSACTIONAL_ID_CONFIG = "transactional.id";
public static final String TRANSACTIONAL_ID_DOC = "The TransactionalId to use for transactional delivery. This enables reliability semantics which span multiple producer sessions since it allows the client to guarantee that transactions using the same TransactionalId have been completed prior to starting any new transactions. If no TransactionalId is provided, then the producer is limited to idempotent delivery. Note that <code>enable.idempotence</code> must be enabled if a TransactionalId is configured. The default is <code>null</code>, which means transactions cannot be used. Note that, by default, transactions require a cluster of at least three brokers which is the recommended setting for production; for development you can change this, by adjusting broker setting <code>transaction.state.log.replication.factor</code>.";
```

#### 3.4.2、消费端参数

```java
public static final String GROUP_ID_CONFIG = "group.id";
private static final String GROUP_ID_DOC = "A unique string that identifies the consumer group this consumer belongs to. This property is required if the consumer uses either the group management functionality by using <code>subscribe(topic)</code> or the Kafka-based offset management strategy.";
public static final String MAX_POLL_RECORDS_CONFIG = "max.poll.records";
private static final String MAX_POLL_RECORDS_DOC = "The maximum number of records returned in a single call to poll().";
public static final String MAX_POLL_INTERVAL_MS_CONFIG = "max.poll.interval.ms";
private static final String MAX_POLL_INTERVAL_MS_DOC = "The maximum delay between invocations of poll() when using consumer group management. This places an upper bound on the amount of time that the consumer can be idle before fetching more records. If poll() is not called before expiration of this timeout, then the consumer is considered failed and the group will rebalance in order to reassign the partitions to another member. ";
public static final String SESSION_TIMEOUT_MS_CONFIG = "session.timeout.ms";
private static final String SESSION_TIMEOUT_MS_DOC = "The timeout used to detect consumer failures when using Kafka's group management facility. The consumer sends periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by the broker before the expiration of this session timeout, then the broker will remove this consumer from the group and initiate a rebalance. Note that the value must be in the allowable range as configured in the broker configuration by <code>group.min.session.timeout.ms</code> and <code>group.max.session.timeout.ms</code>.";
public static final String HEARTBEAT_INTERVAL_MS_CONFIG = "heartbeat.interval.ms";
private static final String HEARTBEAT_INTERVAL_MS_DOC = "The expected time between heartbeats to the consumer coordinator when using Kafka's group management facilities. Heartbeats are used to ensure that the consumer's session stays active and to facilitate rebalancing when new consumers join or leave the group. The value must be set lower than <code>session.timeout.ms</code>, but typically should be set no higher than 1/3 of that value. It can be adjusted even lower to control the expected time for normal rebalances.";
public static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";
public static final String CLIENT_DNS_LOOKUP_CONFIG = "client.dns.lookup";
public static final String ENABLE_AUTO_COMMIT_CONFIG = "enable.auto.commit";
private static final String ENABLE_AUTO_COMMIT_DOC = "If true the consumer's offset will be periodically committed in the background.";
public static final String AUTO_COMMIT_INTERVAL_MS_CONFIG = "auto.commit.interval.ms";
private static final String AUTO_COMMIT_INTERVAL_MS_DOC = "The frequency in milliseconds that the consumer offsets are auto-committed to Kafka if <code>enable.auto.commit</code> is set to <code>true</code>.";
public static final String PARTITION_ASSIGNMENT_STRATEGY_CONFIG = "partition.assignment.strategy";
private static final String PARTITION_ASSIGNMENT_STRATEGY_DOC = "The class name of the partition assignment strategy that the client will use to distribute partition ownership amongst consumer instances when group management is used";
public static final String AUTO_OFFSET_RESET_CONFIG = "auto.offset.reset";
public static final String AUTO_OFFSET_RESET_DOC = "What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted): <ul><li>earliest: automatically reset the offset to the earliest offset<li>latest: automatically reset the offset to the latest offset</li><li>none: throw exception to the consumer if no previous offset is found for the consumer's group</li><li>anything else: throw exception to the consumer.</li></ul>";
public static final String FETCH_MIN_BYTES_CONFIG = "fetch.min.bytes";
private static final String FETCH_MIN_BYTES_DOC = "The minimum amount of data the server should return for a fetch request. If insufficient data is available the request will wait for that much data to accumulate before answering the request. The default setting of 1 byte means that fetch requests are answered as soon as a single byte of data is available or the fetch request times out waiting for data to arrive. Setting this to something greater than 1 will cause the server to wait for larger amounts of data to accumulate which can improve server throughput a bit at the cost of some additional latency.";
public static final String FETCH_MAX_BYTES_CONFIG = "fetch.max.bytes";
private static final String FETCH_MAX_BYTES_DOC = "The maximum amount of data the server should return for a fetch request. Records are fetched in batches by the consumer, and if the first record batch in the first non-empty partition of the fetch is larger than this value, the record batch will still be returned to ensure that the consumer can make progress. As such, this is not a absolute maximum. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). Note that the consumer performs multiple fetches in parallel.";
public static final int DEFAULT_FETCH_MAX_BYTES = 52428800;
public static final String FETCH_MAX_WAIT_MS_CONFIG = "fetch.max.wait.ms";
private static final String FETCH_MAX_WAIT_MS_DOC = "The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes.";
public static final String METADATA_MAX_AGE_CONFIG = "metadata.max.age.ms";
public static final String MAX_PARTITION_FETCH_BYTES_CONFIG = "max.partition.fetch.bytes";
private static final String MAX_PARTITION_FETCH_BYTES_DOC = "The maximum amount of data per-partition the server will return. Records are fetched in batches by the consumer. If the first record batch in the first non-empty partition of the fetch is larger than this limit, the batch will still be returned to ensure that the consumer can make progress. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). See fetch.max.bytes for limiting the consumer request size.";
public static final int DEFAULT_MAX_PARTITION_FETCH_BYTES = 1048576;
public static final String SEND_BUFFER_CONFIG = "send.buffer.bytes";
public static final String RECEIVE_BUFFER_CONFIG = "receive.buffer.bytes";
public static final String CLIENT_ID_CONFIG = "client.id";
public static final String RECONNECT_BACKOFF_MS_CONFIG = "reconnect.backoff.ms";
public static final String RECONNECT_BACKOFF_MAX_MS_CONFIG = "reconnect.backoff.max.ms";
public static final String RETRY_BACKOFF_MS_CONFIG = "retry.backoff.ms";
public static final String METRICS_SAMPLE_WINDOW_MS_CONFIG = "metrics.sample.window.ms";
public static final String METRICS_NUM_SAMPLES_CONFIG = "metrics.num.samples";
public static final String METRICS_RECORDING_LEVEL_CONFIG = "metrics.recording.level";
public static final String METRIC_REPORTER_CLASSES_CONFIG = "metric.reporters";
public static final String CHECK_CRCS_CONFIG = "check.crcs";
private static final String CHECK_CRCS_DOC = "Automatically check the CRC32 of the records consumed. This ensures no on-the-wire or on-disk corruption to the messages occurred. This check adds some overhead, so it may be disabled in cases seeking extreme performance.";
public static final String KEY_DESERIALIZER_CLASS_CONFIG = "key.deserializer";
public static final String KEY_DESERIALIZER_CLASS_DOC = "Deserializer class for key that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.";
public static final String VALUE_DESERIALIZER_CLASS_CONFIG = "value.deserializer";
public static final String VALUE_DESERIALIZER_CLASS_DOC = "Deserializer class for value that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.";
public static final String CONNECTIONS_MAX_IDLE_MS_CONFIG = "connections.max.idle.ms";
public static final String REQUEST_TIMEOUT_MS_CONFIG = "request.timeout.ms";
public static final String GROUP_ID_CONFIG = "group.id";
private static final String GROUP_ID_DOC = "A unique string that identifies the consumer group this consumer belongs to. This property is required if the consumer uses either the group management functionality by using <code>subscribe(topic)</code> or the Kafka-based offset management strategy.";
public static final String MAX_POLL_RECORDS_CONFIG = "max.poll.records";
private static final String MAX_POLL_RECORDS_DOC = "The maximum number of records returned in a single call to poll().";
public static final String MAX_POLL_INTERVAL_MS_CONFIG = "max.poll.interval.ms";
private static final String MAX_POLL_INTERVAL_MS_DOC = "The maximum delay between invocations of poll() when using consumer group management. This places an upper bound on the amount of time that the consumer can be idle before fetching more records. If poll() is not called before expiration of this timeout, then the consumer is considered failed and the group will rebalance in order to reassign the partitions to another member. ";
public static final String SESSION_TIMEOUT_MS_CONFIG = "session.timeout.ms";
private static final String SESSION_TIMEOUT_MS_DOC = "The timeout used to detect consumer failures when using Kafka's group management facility. The consumer sends periodic heartbeats to indicate its liveness to the broker. If no heartbeats are received by the broker before the expiration of this session timeout, then the broker will remove this consumer from the group and initiate a rebalance. Note that the value must be in the allowable range as configured in the broker configuration by <code>group.min.session.timeout.ms</code> and <code>group.max.session.timeout.ms</code>.";
public static final String HEARTBEAT_INTERVAL_MS_CONFIG = "heartbeat.interval.ms";
private static final String HEARTBEAT_INTERVAL_MS_DOC = "The expected time between heartbeats to the consumer coordinator when using Kafka's group management facilities. Heartbeats are used to ensure that the consumer's session stays active and to facilitate rebalancing when new consumers join or leave the group. The value must be set lower than <code>session.timeout.ms</code>, but typically should be set no higher than 1/3 of that value. It can be adjusted even lower to control the expected time for normal rebalances.";
public static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";
public static final String CLIENT_DNS_LOOKUP_CONFIG = "client.dns.lookup";
public static final String ENABLE_AUTO_COMMIT_CONFIG = "enable.auto.commit";
private static final String ENABLE_AUTO_COMMIT_DOC = "If true the consumer's offset will be periodically committed in the background.";
public static final String AUTO_COMMIT_INTERVAL_MS_CONFIG = "auto.commit.interval.ms";
private static final String AUTO_COMMIT_INTERVAL_MS_DOC = "The frequency in milliseconds that the consumer offsets are auto-committed to Kafka if <code>enable.auto.commit</code> is set to <code>true</code>.";
public static final String PARTITION_ASSIGNMENT_STRATEGY_CONFIG = "partition.assignment.strategy";
private static final String PARTITION_ASSIGNMENT_STRATEGY_DOC = "The class name of the partition assignment strategy that the client will use to distribute partition ownership amongst consumer instances when group management is used";
public static final String AUTO_OFFSET_RESET_CONFIG = "auto.offset.reset";
public static final String AUTO_OFFSET_RESET_DOC = "What to do when there is no initial offset in Kafka or if the current offset does not exist any more on the server (e.g. because that data has been deleted): <ul><li>earliest: automatically reset the offset to the earliest offset<li>latest: automatically reset the offset to the latest offset</li><li>none: throw exception to the consumer if no previous offset is found for the consumer's group</li><li>anything else: throw exception to the consumer.</li></ul>";
public static final String FETCH_MIN_BYTES_CONFIG = "fetch.min.bytes";
private static final String FETCH_MIN_BYTES_DOC = "The minimum amount of data the server should return for a fetch request. If insufficient data is available the request will wait for that much data to accumulate before answering the request. The default setting of 1 byte means that fetch requests are answered as soon as a single byte of data is available or the fetch request times out waiting for data to arrive. Setting this to something greater than 1 will cause the server to wait for larger amounts of data to accumulate which can improve server throughput a bit at the cost of some additional latency.";
public static final String FETCH_MAX_BYTES_CONFIG = "fetch.max.bytes";
private static final String FETCH_MAX_BYTES_DOC = "The maximum amount of data the server should return for a fetch request. Records are fetched in batches by the consumer, and if the first record batch in the first non-empty partition of the fetch is larger than this value, the record batch will still be returned to ensure that the consumer can make progress. As such, this is not a absolute maximum. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). Note that the consumer performs multiple fetches in parallel.";
public static final int DEFAULT_FETCH_MAX_BYTES = 52428800;
public static final String FETCH_MAX_WAIT_MS_CONFIG = "fetch.max.wait.ms";
private static final String FETCH_MAX_WAIT_MS_DOC = "The maximum amount of time the server will block before answering the fetch request if there isn't sufficient data to immediately satisfy the requirement given by fetch.min.bytes.";
public static final String METADATA_MAX_AGE_CONFIG = "metadata.max.age.ms";
public static final String MAX_PARTITION_FETCH_BYTES_CONFIG = "max.partition.fetch.bytes";
private static final String MAX_PARTITION_FETCH_BYTES_DOC = "The maximum amount of data per-partition the server will return. Records are fetched in batches by the consumer. If the first record batch in the first non-empty partition of the fetch is larger than this limit, the batch will still be returned to ensure that the consumer can make progress. The maximum record batch size accepted by the broker is defined via <code>message.max.bytes</code> (broker config) or <code>max.message.bytes</code> (topic config). See fetch.max.bytes for limiting the consumer request size.";
public static final int DEFAULT_MAX_PARTITION_FETCH_BYTES = 1048576;
public static final String SEND_BUFFER_CONFIG = "send.buffer.bytes";
public static final String RECEIVE_BUFFER_CONFIG = "receive.buffer.bytes";
public static final String CLIENT_ID_CONFIG = "client.id";
public static final String RECONNECT_BACKOFF_MS_CONFIG = "reconnect.backoff.ms";
public static final String RECONNECT_BACKOFF_MAX_MS_CONFIG = "reconnect.backoff.max.ms";
public static final String RETRY_BACKOFF_MS_CONFIG = "retry.backoff.ms";
public static final String METRICS_SAMPLE_WINDOW_MS_CONFIG = "metrics.sample.window.ms";
public static final String METRICS_NUM_SAMPLES_CONFIG = "metrics.num.samples";
public static final String METRICS_RECORDING_LEVEL_CONFIG = "metrics.recording.level";
public static final String METRIC_REPORTER_CLASSES_CONFIG = "metric.reporters";
public static final String CHECK_CRCS_CONFIG = "check.crcs";
private static final String CHECK_CRCS_DOC = "Automatically check the CRC32 of the records consumed. This ensures no on-the-wire or on-disk corruption to the messages occurred. This check adds some overhead, so it may be disabled in cases seeking extreme performance.";
public static final String KEY_DESERIALIZER_CLASS_CONFIG = "key.deserializer";
public static final String KEY_DESERIALIZER_CLASS_DOC = "Deserializer class for key that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.";
public static final String VALUE_DESERIALIZER_CLASS_CONFIG = "value.deserializer";
public static final String VALUE_DESERIALIZER_CLASS_DOC = "Deserializer class for value that implements the <code>org.apache.kafka.common.serialization.Deserializer</code> interface.";
public static final String CONNECTIONS_MAX_IDLE_MS_CONFIG = "connections.max.idle.ms";
public static final String REQUEST_TIMEOUT_MS_CONFIG = "request.timeout.ms";
private static final String REQUEST_TIMEOUT_MS_DOC = "The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.";
public static final String DEFAULT_API_TIMEOUT_MS_CONFIG = "default.api.timeout.ms";
public static final String DEFAULT_API_TIMEOUT_MS_DOC = "Specifies the timeout (in milliseconds) for consumer APIs that could block. This configuration is used as the default timeout for all consumer operations that do not explicitly accept a <code>timeout</code> parameter.";
public static final String INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes";
public static final String INTERCEPTOR_CLASSES_DOC = "A list of classes to use as interceptors. Implementing the <code>org.apache.kafka.clients.consumer.ConsumerInterceptor</code> interface allows you to intercept (and possibly mutate) records received by the consumer. By default, there are no interceptors.";
public static final String EXCLUDE_INTERNAL_TOPICS_CONFIG = "exclude.internal.topics";
private static final String EXCLUDE_INTERNAL_TOPICS_DOC = "Whether records from internal topics (such as offsets) should be exposed to the consumer. If set to <code>true</code> the only way to receive records from an internal topic is subscribing to it.";
public static final boolean DEFAULT_EXCLUDE_INTERNAL_TOPICS = true;
static final String LEAVE_GROUP_ON_CLOSE_CONFIG = "internal.leave.group.on.close";
public static final String ISOLATION_LEVEL_CONFIG = "isolation.level";
public static final String ISOLATION_LEVEL_DOC = "<p>Controls how to read messages written transactionally. If set to <code>read_committed</code>, consumer.poll() will only return transactional messages which have been committed. If set to <code>read_uncommitted</code>' (the default), consumer.poll() will return all messages, even transactional messages which have been aborted. Non-transactional messages will be returned unconditionally in either mode.</p> <p>Messages will always be returned in offset order. Hence, in  <code>read_committed</code> mode, consumer.poll() will only return messages up to the last stable offset (LSO), which is the one less than the offset of the first open transaction. In particular any messages appearing after messages belonging to ongoing transactions will be withheld until the relevant transaction has been completed. As a result, <code>read_committed</code> consumers will not be able to read up to the high watermark when there are in flight transactions.</p><p> Further, when in <code>read_committed</code> the seekToEnd method will return the LSO"private static final String REQUEST_TIMEOUT_MS_DOC = "The configuration controls the maximum amount of time the client will wait for the response of a request. If the response is not received before the timeout elapses the client will resend the request if necessary or fail the request if retries are exhausted.";
public static final String DEFAULT_API_TIMEOUT_MS_CONFIG = "default.api.timeout.ms";
public static final String DEFAULT_API_TIMEOUT_MS_DOC = "Specifies the timeout (in milliseconds) for consumer APIs that could block. This configuration is used as the default timeout for all consumer operations that do not explicitly accept a <code>timeout</code> parameter.";
public static final String INTERCEPTOR_CLASSES_CONFIG = "interceptor.classes";
public static final String INTERCEPTOR_CLASSES_DOC = "A list of classes to use as interceptors. Implementing the <code>org.apache.kafka.clients.consumer.ConsumerInterceptor</code> interface allows you to intercept (and possibly mutate) records received by the consumer. By default, there are no interceptors.";
public static final String EXCLUDE_INTERNAL_TOPICS_CONFIG = "exclude.internal.topics";
private static final String EXCLUDE_INTERNAL_TOPICS_DOC = "Whether records from internal topics (such as offsets) should be exposed to the consumer. If set to <code>true</code> the only way to receive records from an internal topic is subscribing to it.";
public static final boolean DEFAULT_EXCLUDE_INTERNAL_TOPICS = true;
static final String LEAVE_GROUP_ON_CLOSE_CONFIG = "internal.leave.group.on.close";
public static final String ISOLATION_LEVEL_CONFIG = "isolation.level";
public static final String ISOLATION_LEVEL_DOC = "<p>Controls how to read messages written transactionally. If set to <code>read_committed</code>, consumer.poll() will only return transactional messages which have been committed. If set to <code>read_uncommitted</code>' (the default), consumer.poll() will return all messages, even transactional messages which have been aborted. Non-transactional messages will be returned unconditionally in either mode.</p> <p>Messages will always be returned in offset order. Hence, in  <code>read_committed</code> mode, consumer.poll() will only return messages up to the last stable offset (LSO), which is the one less than the offset of the first open transaction. In particular any messages appearing after messages belonging to ongoing transactions will be withheld until the relevant transaction has been completed. As a result, <code>read_committed</code> consumers will not be able to read up to the high watermark when there are in flight transactions.</p><p> Further, when in <code>read_committed</code> the seekToEnd method will return the LSO";
```

## 4、SpringBoot整合kafka

### 4.1、在springboot项目的基础上添加kafka的依赖

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
	<artifactId>spring-kafka</artifactId>
	<version>2.1.7.RELEASE</version>
</dependency>
```

### 4.2、kafka配置

在application.properties文件中添加kafka的配置

```properties
spring.kafka.producer.acks=-1
spring.kafka.producer.bootstrap-servers=ubuntu1.qknavy.com:9092,ubuntu2.qknavy.com:9092,ubuntu3.qknavy.com:9092
spring.kafka.producer.client-id=spring-demo-producer
spring.kafka.producer.key-serializer=org.apache.kafka.common.serialization.IntegerSerializer
spring.kafka.producer.value-serializer=org.apache.kafka.common.serialization.StringSerializer
```

4.3、注入KafkaTemplate实例

```java
@Autowired
private KafkaTemplate<Integer, String> kafkaTemplate;
```

> springboot会自动组装kafkaTemplate

### 4.4、直接调用kafkaTemplate的发送消息的方法

```java
kafkaTemplate.send("test",msg);
```

> 其中send有多个重载的方法，这里我们直接使用发送字符串消息。上面的例子中的第一个参数是表示“主题”

### 4.5、测试结果

运行消费端，然后运行生产者发送消息，可以看到消费端正常接收到消息

```
consumer received msg[ hha ]
```

> 从上面的操作可以看到，springboot项目集成kafka是非常简单的，只需要添加上kafka的配置然后注入kafka的模板对象就可以了。

