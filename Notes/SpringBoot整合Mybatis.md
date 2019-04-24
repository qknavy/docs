# SpringBoot整合Mybatis

### 1、导入依赖

```xml
<dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.0.2.RELEASE</version>
        </dependency>
        <dependency>
            <groupId>org.mybatis.spring.boot</groupId>
            <artifactId>mybatis-spring-boot-starter</artifactId>
            <version>2.0.1</version>
        </dependency>
        <dependency>
            <groupId>mysql</groupId>
            <artifactId>mysql-connector-java</artifactId>
            <version>5.1.9</version>
            <scope>runtime</scope>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>2.0.2.RELEASE</version>
            <scope>test</scope>
        </dependency>
```



### 2、实体类`User.java`

```java
public class User implements Serializable
{
    private static final long serialVersionUID = -5738590165229910140L;
    private int id;
    private String name;
    private int age;
    private double money;

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public int getAge()
    {
        return age;
    }

    public void setAge(int age)
    {
        this.age = age;
    }

    public double getMoney()
    {
        return money;
    }

    public void setMoney(double money)
    {
        this.money = money;
    }

    @Override public String toString()
    {
        return "User{" + "id=" + id + ", name='" + name + '\'' + ", age=" + age + ", money=" + money + '}';
    }
```



### 3、dao接口`UserDao.java`

```java
public interface UserDao
{
    public void insertUser(User user);

    public List<User> findUserList(String name);
}
```



### 4、Service服务类`UserService.java`

```java
@Service
public class UserService {
    @Autowired
    private UserDao userDao;

    public List<User> findUsers(String name){
        return userDao.findUserList(name);
    }

    public void insertUser(User user){
        userDao.insertUser(user);
    }
}
```

### 5、控制器`UserController.java`

```java
@RestController
@RequestMapping("/user")
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/query")
    public List<User> query(String name) {
        name = StringUtils.isEmpty(name)?"":name;
        return userService.findUsers(name);
    }

    @PostMapping("/add")
    public String insertUser(String name, int age, double money){
        User user = new User();
        user.setAge(age);
        user.setMoney(money);
        user.setName(name);
        userService.insertUser(user);
        return "success";
    }
}
```



### 6、启动类`Bootstrap.java`

```java
@SpringBootApplication
@MapperScan("com.qknavy.*.dao")
public class Bootstrap
{
    public static void main(String[] args)
    {
        SpringApplication.run(Bootstrap.class, args);
    }
}
```



### 7、springboot配置文件`application.properties`

```properties
server.port=8989
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_mybatis_demo?characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=P@ssw0rd
spring.datasource.driver-class-name=com.mysql.jdbc.Driver

mybatis.mapper-locations=classpath:mapper/*.xml
```

### 8、mybatis的mapper配置文件

```xml
<?xml version="1.0" encoding="UTF-8"?>
<!DOCTYPE mapper PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<mapper namespace="com.qknavy.users.dao.UserDao">

    <insert id="insertUser" parameterType="com.qknavy.users.entity.User">
        insert into user(name,age,money) VALUES (#{name}, #{age}, #{money});
    </insert>

    <select id="findUserList" parameterType="java.lang.String" resultType="com.qknavy.users.entity.User">
      SELECT * from user where name LIKE CONCAT('%',#{name},'%')
    </select>
</mapper>
```

### 9、新建用户表

```sql
CREATE TABLE `user` (
  `id` int(13) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(33) DEFAULT NULL COMMENT '姓名',
  `age` int(3) DEFAULT NULL COMMENT '年龄',
  `money` double DEFAULT NULL COMMENT '账户余额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8
```



### 10、单元测试

```java
@SpringBootTest
@RunWith(SpringRunner.class)
public class UserControllerTest
{
    @Autowired
    private UserController userController;

    @Test
    public void testInsert(){
        userController.insertUser("yuankunliu",20,3800);
    }

    @Test
    public void testQuery(){
        List<User> list = userController.query("");
        System.out.println(list);
    }
}
```





### 11、添加分页支持

##### a、添加依赖

```xml
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper</artifactId>
    <version>5.1.6</version>
</dependency>
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-autoconfigure</artifactId>
    <version>1.2.7</version>
</dependency>
```

> `pagehelper-spring-boot-autoconfigure`必须导入，否则分页不生效



##### b、分页配置`PageHelperConfig.java`

```java
@Configuration
public class PageHelperConfig {
    @Bean
    public PageHelper pageHelper(){
        PageHelper pageHelper = new PageHelper();
        Properties p = new Properties();
        //1.offsetAsPageNum:设置为true时，会将RowBounds第一个参数offset当成pageNum页码使用.
        p.setProperty("offsetAsPageNum", "true");
        //2.rowBoundsWithCount:设置为true时，使用RowBounds分页会进行count查询.
        p.setProperty("rowBoundsWithCount", "true");
        //3.reasonable：启用合理化时，如果pageNum<1会查询第一页，如果pageNum>pages会查询最后一页。
        p.setProperty("reasonable", "true");
        pageHelper.setProperties(p);
        return pageHelper;
    }
}
```



##### c、分页

> PageHelper.startPage方法

常用的有以下几个重载方法：

* 简单分页

  ```
  public static <E> Page<E> startPage(int pageNum, int pageSize)
  ```

* 带排序的分页

  ```
  public static <E> Page<E> startPage(int pageNum, int pageSize, String orderBy)
  ```

  > 下面带排序的分页方式以及其它参数也可以通过`Page`的setOrderBy单独设置

d、示例

```java
@GetMapping("/query")
    public PageInfo query(@RequestParam(defaultValue = "1", name = "pageNum") int pageNum,
            @RequestParam(defaultValue = "2", name = "pageSize") int pageSize, String name) {
        name = StringUtils.isEmpty(name)?"":name;
        PageHelper.startPage(pageNum,pageSize,"id asc");
        List<User> userList = userService.findUsers(name);
        PageInfo<User> page = new PageInfo<User>(userList);
        return page;
    }
```

