# SpringBoot整合Mybatis-plus

> springboot是当前后端Java开发的主流技术框架，开发效率高，而作为时下流程的orm框架mybatis在使用的时候也是非常灵活。所以springboot+mybatis也是我们开发的过程中经常首先考虑的一种模式。
>
> 然而，我们在对单表操作的时候经常也会写一些重复性非常高的crud，为了简化开发提升开发效率，mybatis-plus很好地解决了这个问题

[官网](<http://mp.baomidou.com/> )

### 1、引入依赖`pom.xml`

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.qknavy</groupId>
    <artifactId>springboot-mybatisplus-demo</artifactId>
    <version>1.0-SNAPSHOT</version>

    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>2.0.2.RELEASE</version>
    </parent>

    <dependencies>
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
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <version>1.18.6</version>
        </dependency>

        <!--mybatis-plus-->
        <dependency>
            <groupId>com.baomidou</groupId>
            <artifactId>mybatis-plus-boot-starter</artifactId>
            <version>3.0.7.1</version>
        </dependency>
    </dependencies>
</project>
```

> 上面的依赖是我们在springboot环境下的引入方式，如果是普通的springmvc模式下，我们需要引入的就是mybatis-plus
>
> ```xml
> <dependency>
>     <groupId>com.baomidou</groupId>
>     <artifactId>mybatis-plus</artifactId>
>     <version>3.0.7.1</version>
> </dependency>
> ```



### 2、添加数据库的配置`application.properties`

```properties
server.port=8989
spring.datasource.url=jdbc:mysql://localhost:3306/springboot_mybatis_demo?characterEncoding=utf8
spring.datasource.username=root
spring.datasource.password=P@ssw0rd
spring.datasource.driver-class-name=com.mysql.jdbc.Driver
```



### 3、数据库、表

```sql
CREATE TABLE `user` (
  `id` int(13) NOT NULL AUTO_INCREMENT COMMENT '主键',
  `name` varchar(33) DEFAULT NULL COMMENT '姓名',
  `age` int(3) DEFAULT NULL COMMENT '年龄',
  `money` double DEFAULT NULL COMMENT '账户余额',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8
```



### 4、实体类`User.java`

```java
@Data
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable
{
    private static final long serialVersionUID = -5738590165229910140L;
    private int id;
    private String name;
    private int age;
    private double money;
}
```

> 这里采用了lombok方式简化开发，所以省去了getter、setter方法

### 5、编写我们自己的mapper接口

`UserMapper.java`:

```java
public interface UserMapper extends BaseMapper<User>
{
}
```

> 我们可以一个方法都不用写就能实现基础的功能，只需要继承BaseMapper这个接口即可

### 6、添加mapper路径的配置

要想程序能够找到我们定义的mapper并实现后续的注入，我们需要在springboot的启动类中添加mapper路径配置，启动类`Bootstrap.java`如下：

```java
@MapperScan("com.qknavy.*.dao")
@SpringBootApplication
public class Bootstrap
{
    public static void main(String[] args)
    {
        SpringApplication.run(Bootstrap.class, args);
    }
}
```

至此，我们就实现了基础的单表操作（看似没有写一个方法一条sql语句，就是这么神奇！！！）

### 7、测试

```java
@RunWith(SpringRunner.class)
@SpringBootTest
public class UserMapperTest
{
    @Autowired
    private UserMapper userMapper;

    @Test
    public void testInsert(){
        User user = new User();
        user.setAge(33);
        user.setMoney(8888);
        user.setName("Lucy");
        int result = userMapper.insert(user);
        System.out.println(result);
    }

    @Test
    public void testUpdate(){
        User user = userMapper.selectById(5);
        user.setName("刘元坤");
        int result = userMapper.updateById(user);
        System.out.println(result);
    }

    @Test
    public void testQuery(){
        User user = new User();
        user.setName("a");
        Wrapper<User> wrapper = new QueryWrapper<User>().like("name","a");
        List<User> list  = userMapper.selectList(wrapper);
        System.out.println(list);
    }

    @Test
    public void testGetOne(){
        User user = userMapper.selectById(5);
        System.out.println(user);
    }

    @Test
    public void testDelete(){
        int result = userMapper.deleteById(10);
        System.out.println(result);
    }
}
```

所有测试方法都通过大功告成

注意上面的查询方法`testQuery`用到了一个Wrapper，这个查询器非常强大，基本上支持我们常见的所有操作，比如order，le，gt，eq，in，or，and，between...




### 8、分页

##### 添加分页依赖

```xml
<!--分页-->
<dependency>
    <groupId>com.github.pagehelper</groupId>
    <artifactId>pagehelper-spring-boot-starter</artifactId>
    <version>1.2.7</version>
</dependency>
```


##### springboot配置

```java
@EnableTransactionManagement
@Configuration
@MapperScan("com.qknavy.*.dao")
public class MybatisPlusConfig
{
    @Bean
    public PaginationInterceptor paginationInterceptor(){
        PaginationInterceptor page =  new PaginationInterceptor();
        page.setDialectType("mysql");
        return page;
    }

    @Bean
    public Object testBean(PlatformTransactionManager platformTransactionManager){
        System.out.println(">>>>>>>>>>" + platformTransactionManager.getClass().getName());
        return new Object();
    }


    @Bean
    public PerformanceInterceptor performanceInterceptor() {
        PerformanceInterceptor page = new PerformanceInterceptor();
        page.setFormat(true);
        return page;
    }

    @Bean
    public PageHelper pageHelper(){
        PageHelper pageHelper = new PageHelper();
        Properties properties = new Properties();
        properties.setProperty("offsetAsPageNum","true");
        properties.setProperty("rowBoundsWithCount","true");
        properties.setProperty("reasonable","true");
        properties.setProperty("dialect","mysql");    //配置mysql数据库的方言
        pageHelper.setProperties(properties);
        return pageHelper;
    }
}
```




##### 测试

```java
@Test
public void testPage(){
    Wrapper<User> wrapper = new QueryWrapper<User>().like("name","a");
    IPage<User> page = new Page<>(1,2);
    IPage<User> p = userService.page(page, wrapper);
    System.out.println(p);
}
```






### 9、自动生成代码

##### 引入依赖

```
<dependency>
  <groupId>com.baomidou</groupId>
  <artifactId>mybatis-plus-generator</artifactId>
  <version>3.0.5</version>
</dependency>
<dependency>
  <groupId>org.freemarker</groupId>
  <artifactId>freemarker</artifactId>
  <version>2.3.28</version>
</dependency>
```

##### 自定义自动生成代码的方法

`CodeGenerator.java`

```java
public class CodeGenerator
{
    public static String scanner(String tip){
        Scanner scanner = new Scanner(System.in);
        StringBuilder help = new StringBuilder();
        help.append("请输入" + tip + ":");
        System.out.println(help.toString());
        if (scanner.hasNext()){
            String ipt = scanner.next();
            if (!StringUtils.isEmpty(ipt)){
                return ipt;
            }
        }
        throw  new MybatisPlusException("请输入正确的" + tip +"!");
    }

    public static void main(String[] args)
    {
        //代码生成器
        AutoGenerator mpg = new AutoGenerator();
        //全局配置
        GlobalConfig gc = new GlobalConfig();
        String projectPath = System.getProperty("user.dir");
        gc.setOutputDir(projectPath + "/src/main/java");
        gc.setAuthor("Navy");
        gc.setOpen(true);
        mpg.setGlobalConfig(gc);

        //数据源配置
        DataSourceConfig dsc = new DataSourceConfig();
        dsc.setUrl("jdbc:mysql://localhost:3306/springboot_mybatis_demo?characterEncoding=utf8");
        dsc.setDriverName("com.mysql.jdbc.Driver");
        dsc.setUsername("root");
        dsc.setPassword("P@ssw0rd");
        mpg.setDataSource(dsc);

        //包配置
        PackageConfig pc = new PackageConfig();
        pc.setModuleName(scanner("模块名"));
        pc.setParent("com.qknavy.test");
        mpg.setPackageInfo(pc);

        //自定义配置
        InjectionConfig cfg = new InjectionConfig()
        {
            @Override public void initMap()
            { }
        };
        //如果是模板引擎是 freemarker
        String templatePath = "/templates/mapper.xml.ftl";

        //自定义输出配置
        List<FileOutConfig> focList = new ArrayList<>();
        //自定义配置会被有限输出
        focList.add(new FileOutConfig(templatePath){
            @Override public String outputFile(TableInfo tableInfo)
            {
                return projectPath + "/src/main/resources/mapper/" + pc.getModuleName()+"/"+tableInfo.getEntityName()
                        +"Mapper"+ StringPool.DOT_XML;
            }
        });
        cfg.setFileOutConfigList(focList);
        mpg.setCfg(cfg);

        //配置模板
        TemplateConfig templateConfig = new TemplateConfig();
        templateConfig.setXml(null);
        mpg.setTemplate(templateConfig);

        //策略配置
        StrategyConfig strategyConfig = new StrategyConfig();
        strategyConfig.setNaming(NamingStrategy.underline_to_camel);
        strategyConfig.setColumnNaming(NamingStrategy.underline_to_camel);
        strategyConfig.setEntityLombokModel(true);
        strategyConfig.setRestControllerStyle(true);//true-生成restcontroller，false-生成controller
        strategyConfig.setInclude(scanner("表名，多个英文逗号分隔").split(","));
        strategyConfig.setControllerMappingHyphenStyle(true);
        //strategyConfig.setTablePrefix(pc.getModuleName()+"_");//在rest路径上加上表的前缀

        mpg.setStrategy(strategyConfig);
        mpg.setTemplateEngine(new FreemarkerTemplateEngine());
        mpg.execute();
    }
}
```

> 首先需要保证数据库的表先存在，然后修改对应的文件路径和包路径，运行上面代码就会自动生成mapper、service、controller以及entity


