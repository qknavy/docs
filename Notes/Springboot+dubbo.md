1、项目结构：

- users-service
  - user-api
  - user-provider
  - user-web

users-service为父工程，下面分为三个模块：

- user-api：接口定义模块
- user-provider：服务提供者
- user-web：服务消费者



2、项目依赖：

父pom.xml：

    <?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <modelVersion>4.0.0</modelVersion>
        <groupId>com.qknavy</groupId>
        <artifactId>users-service</artifactId>
        <version>1.0-SNAPSHOT</version>
        <packaging>pom</packaging>
    
        <modules>
            <module>user-api</module>
            <module>user-provider</module>
            <module>user-web</module>
        </modules>
    
        <properties>
            <project.build.sourceEncoding>UTF-8</project.build.sourceEncoding>
            <maven.compiler.source>1.8</maven.compiler.source>
            <maven.compiler.target>1.8</maven.compiler.target>
        </properties>
    
        <dependencyManagement>
            <dependencies>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter</artifactId>
                    <version>2.0.4.RELEASE</version>
                </dependency>
                <dependency>
                    <groupId>org.springframework.boot</groupId>
                    <artifactId>spring-boot-starter-web</artifactId>
                    <version>2.0.4.RELEASE</version>
                </dependency>
                <dependency>
                    <groupId>io.dubbo.springboot</groupId>
                    <artifactId>spring-boot-starter-dubbo</artifactId>
                    <version>1.0.0</version>
                </dependency>
                <dependency>
                    <groupId>org.apache.zookeeper</groupId>
                    <artifactId>zookeeper</artifactId>
                    <version>3.4.6</version>
                </dependency>
            </dependencies>
        </dependencyManagement>
    </project>



3、user-api模块

定义普通接口UserService：

    public interface IUserService
    {
        public User userLogin(String userName, String password);
    }

其中User为普通的一个实体bean



4、user-provider模块

pom.xml导入依赖：

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <parent>
            <artifactId>users-service</artifactId>
            <groupId>com.qknavy</groupId>
            <version>1.0-SNAPSHOT</version>
        </parent>
        <modelVersion>4.0.0</modelVersion>
        <artifactId>user-provider</artifactId>
    
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
                <groupId>com.qknavy</groupId>
                <artifactId>user-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>io.dubbo.springboot</groupId>
                <artifactId>spring-boot-starter-dubbo</artifactId>
            </dependency>
            <dependency>
                <groupId>org.apache.zookeeper</groupId>
                <artifactId>zookeeper</artifactId>
            </dependency>
        </dependencies>
    </project>



application.properties配置dubbo相关配置项：

    ## Dubbo 服务提供者配置
    spring.dubbo.application.name=user-service-provider
    spring.dubbo.registry.address=zookeeper://127.0.0.1:2181
    spring.dubbo.registry.group=dev
    spring.dubbo.protocol.name=dubbo
    spring.dubbo.protocol.port=20880
    spring.dubbo.scan=com.qknavy.users

定义user-api中自定义接口的实现UserService：

    @Service
    public class UserService implements IUserService
    {
        @Override
        public User userLogin(String userName, String password)
        {
            User user = new User();
            user.setPassword(password);
            user.setUserName(userName);
            user.setTel("135");
            user.setUserId(10011);
            return user;
        }
    }

其中@Service注解为dubbo的注解：com.alibaba.dubbo.config.annotation.Service

另：user-provider为普通的springboot项目



5、user-web模块：

user-web模块为消费者，同样依赖user-api

pom.xml导入依赖：

    <?xml version="1.0" encoding="UTF-8"?>
    <project xmlns="http://maven.apache.org/POM/4.0.0"
             xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
             xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
        <parent>
            <artifactId>users-service</artifactId>
            <groupId>com.qknavy</groupId>
            <version>1.0-SNAPSHOT</version>
        </parent>
        <modelVersion>4.0.0</modelVersion>
        <artifactId>user-web</artifactId>
    
        <dependencies>
            <dependency>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-starter-web</artifactId>
            </dependency>
            <dependency>
                <groupId>com.qknavy</groupId>
                <artifactId>user-api</artifactId>
                <version>${project.version}</version>
            </dependency>
            <dependency>
                <groupId>io.dubbo.springboot</groupId>
                <artifactId>spring-boot-starter-dubbo</artifactId>
            </dependency>
            <dependency>
                <groupId>org.apache.zookeeper</groupId>
                <artifactId>zookeeper</artifactId>
            </dependency>
        </dependencies>
    </project>

application.properties文件中配置dubbo相关的配置：

    ## Dubbo 服务消费者配置
    spring.dubbo.application.name=user-service-consumer
    spring.dubbo.registry.address=zookeeper://127.0.0.1:2181
    spring.dubbo.registry.group=dev
    spring.dubbo.scan=com.qknavy.users

在需要调用服务的地方使用duubo的注解@Reference注入对应接口的实例，即可像调用本地方法一样直接调用远程服务：

    @RestController
    public class UserController
    {
        @Reference
        private IUserService userService;
    
        @PostMapping(value = "/login")
        public User userLogin(String userName, String password){
            User user = userService.userLogin(userName, password);
            return  user;
        }
    }

@Reference注解支持多种属性，包括出错重试次数、超时、容错处理、负载均衡、集群、版本管理...等等

    @Documented
    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.FIELD, ElementType.METHOD})
    public @interface Reference {
        Class<?> interfaceClass() default void.class;
        String interfaceName() default "";
        String version() default "";
        String group() default "";
        String url() default "";
        String client() default "";
        boolean generic() default false;
        boolean injvm() default false;
        boolean check() default false;
        boolean init() default false;
        boolean lazy() default false;
        boolean stubevent() default false;
        String reconnect() default "";
        boolean sticky() default false;
        String proxy() default "";
        String stub() default "";
        String cluster() default "";
        int connections() default 0;
        int callbacks() default 0;
        String onconnect() default "";
        String ondisconnect() default "";
        String owner() default "";
        String layer() default "";
        int retries() default 0;
        String loadbalance() default "";
        boolean async() default false;
        int actives() default 0;
        boolean sent() default false;
        String mock() default "";
        String validation() default "";
        int timeout() default 0;
        String cache() default "";
        String[] filter() default {};
        String[] listener() default {};
        String[] parameters() default {};
        String application() default "";
        String module() default "";
        String consumer() default "";
        String monitor() default "";
        String[] registry() default {};
    }

例：如果我们要做客户端容错，则可以自定义user-api中自定义接口的实现：

    public class UserServiceMock implements IUserService
    {
        @Override
        public User userLogin(String userName, String password)
        {
            System.out.println("服务端调用失败");
            return null;
        }
    }

然后再在@Reference注解中通过mock指定调用出错的时候的处理方式

    @Reference(timeout = 100,mock = "com.qknavy.users.service.UserServiceMock")
    private IUserService userService;



6、测试

启动zk，启动服务提供者，启动消费者，通过postman调用：

    {
        "userId": 10011,
        "userName": "Navy",
        "password": "Passwod",
        "tel": "135"
    }



