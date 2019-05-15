## Springboot+redis单机版

#### 1、导包、pom依赖

```xml
<dependency>
  	<groupId>org.springframework.boot</groupId>
   	<artifactId>spring-boot-starter-redis</artifactId>
   	<version>1.4.5.RELEASE</version>
</dependency>
```



#### 2、配置Springboot配置

```properties
#redis
spring.redis.database=0
spring.redis.host=10.62.58.219
spring.redis.port=26379
spring.redis.pool.max-active=8
spring.redis.pool.max-wait=-1
spring.redis.pool.max-idle=8
spring.redis.pool.min-idle=0
spring.redis.timeout=0
```



#### 3、修改RedisTemplate默认的序列化（或者自定义自己的序列化）

```
@Configuration
public class RedisTemplateConfig
{
    @Bean
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory);
        // 使用Jackson2JsonRedisSerialize 替换默认序列化
        Jackson2JsonRedisSerializer<Object> jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer<Object>(Object.class);

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);

        jackson2JsonRedisSerializer.setObjectMapper(objectMapper);

        // 设置value的序列化规则和 key的序列化规则
        redisTemplate.setValueSerializer(jackson2JsonRedisSerializer);
        redisTemplate.setKeySerializer(new StringRedisSerializer());
        redisTemplate.afterPropertiesSet();
        return redisTemplate;
    }
}
```
#### 4、调用    

```java
String key = String.valueOf(System.currentTimeMillis());
String value = selectedLinkIdsStr;

logger.debug("key = " + key + ";value = " + value);
redisTemplate.opsForValue().set(key,value);
```




3、springboot中使用redis缓存

- 开启缓存

注解的方式开启缓存@EnableCaching

- 方法里添加缓存配置
  - @Cacheable
    应用到读取数据的方法上，先从缓存中读取，如果没有再从DB获取数据，然后把数据添加到缓存中
  - @CacheEvict
    应用到删除数据的方法上，调用方法时会从缓存中删除对应key的数据 
  - @CachePut
    应用到写数据的方法上，如新增/修改方法，调用方法时会自动把相应的数据放入缓存 

具体的每个注解的参数：




