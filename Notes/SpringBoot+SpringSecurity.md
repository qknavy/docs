# Spring Security权限控制

### 1、导入依赖包：

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <groupId>com.qknavy</groupId>
    <artifactId>spring-security-demo</artifactId>
    <version>1.0-SNAPSHOT</version>


    <dependencies>
        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-config</artifactId>
            <version>5.1.4.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-web</artifactId>
            <version>5.1.4.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.1.2.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-thymeleaf</artifactId>
            <version>2.1.2.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.thymeleaf.extras</groupId>
            <artifactId>thymeleaf-extras-springsecurity4</artifactId>
            <version>3.0.4.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.security</groupId>
            <artifactId>spring-security-test</artifactId>
            <version>5.1.4.RELEASE</version>
        </dependency>

        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <version>2.1.2.RELEASE</version>
        </dependency>


        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
            <version>2.1.2.RELEASE</version>
        </dependency>
    </dependencies>
</project>
```

### 2、目录结构：

root

​	|- user

​		|- index.html

​	|- index.html

​	|- login.html

> 首页是index.html，登录页面login.html，user目录下的内容为私有内容，需要鉴权

##### 首页`index.html`

```html
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:th="https://www.thymeleaf.org" xmlns:sec="https://www.thymeleaf.org/thymeleaf-extras-springsecurity5">
    <head>
        <title>Hello Spring Security</title>
        <meta charset="utf-8" />
        <link rel="stylesheet" href="/css/main.css" th:href="@{/css/main.css}" />
    </head>
    <body>
        <div th:fragment="logout" class="logout" sec:authorize="isAuthenticated()">
            Logged in user: <span sec:authentication="name"></span> |
            Roles: <span sec:authentication="principal.authorities"></span>
            <div>
                <form action="#" th:action="@{/logout}" method="post">
                    <input type="submit" value="Logout" />
                </form>
            </div>
        </div>
        <h1>Hello Spring Security</h1>
        <p>This is an unsecured page, but you can access the secured pages after authenticating.</p>
        <ul>
            <li>Go to the <a href="/user/index" th:href="@{/user/index}">secured pages</a></li>
        </ul>
    </body>
</html>

```

##### `登录页面login.html`

```html
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:th="https://www.thymeleaf.org">
    <head>
        <title>Login page</title>
        <meta charset="utf-8" />
        <link rel="stylesheet" href="/css/main.css" th:href="@{/css/main.css}" />
	</head>
    <body>
        <h1>Login page</h1>
        <p>Example user: user / password</p>
        <p th:if="${loginError}" class="error">Wrong user or password</p>
        <form th:action="@{/login}" method="post">
            <label for="username">Username</label>:
            <input type="text" id="username" name="username" autofocus="autofocus" /> <br />
            <label for="password">Password</label>:
            <input type="password" id="password" name="password" /> <br />
            <input type="submit" value="Log in" />
        </form>
        <p><a href="/index" th:href="@{/index}">Back to home page</a></p>
    </body>
</html>

```

##### `登录进去的首页index.html`

```html
<!DOCTYPE html>
<html xmlns="http://www.w3.org/1999/xhtml" xmlns:th="https://www.thymeleaf.org">
    <head>
        <title>Hello Spring Security</title>
        <meta charset="utf-8" />
        <link rel="stylesheet" href="/css/main.css" th:href="@{/css/main.css}" />
    </head>
    <body>
        <div th:substituteby="index::logout"></div>
        <h1>This is a secured page!</h1>
        <p><a href="/index" th:href="@{/index}">Back to home page</a></p>
    </body>
</html>

```



##### `Spring配置文件application.yarm`

```yaml
server:
  port: 8080

logging:
  level:
    root: WARN
    org.springframework.web: INFO
    org.springframework.security: INFO

spring:
  thymeleaf:
    cache: false

```



##### SpringSecurity的配置SecurityConfig.java

```java
@EnableWebSecurity
public class SecurityConfig extends WebSecurityConfigurerAdapter {

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.authorizeRequests()
					.antMatchers("/css/**", "/index").permitAll()
					.antMatchers("/user/**").hasRole("USER")
					.and()
				.formLogin().loginPage("/login").failureUrl("/login-error");
	}

	@Autowired
	public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
		auth.inMemoryAuthentication()
				.withUser(User.withDefaultPasswordEncoder().username("user").password("password").roles("USER"));
	}
}
```



##### `MainController.java`

```java
@Controller
public class MainController {

	@RequestMapping("/")
	public String root() {
		return "redirect:/index";
	}

	@RequestMapping("/index")
	public String index() {
		return "index";
	}

	@RequestMapping("/user/index")
	public String userIndex() {
		return "user/index";
	}

	@RequestMapping("/login")
	public String login() {
		return "login";
	}

	@RequestMapping("/login-error")
	public String loginError(Model model) {
		model.addAttribute("loginError", true);
		return "login";
	}
}
```



##### SpringBoot的启动类

```java
@SpringBootApplication
public class HelloWorldApplication {

	public static void main(String[] args) {
		SpringApplication.run(HelloWorldApplication.class, args);
	}
}
```



### 3、启动项目

访问localhost:8080，可以看到进入到首页，点击链接去访问的时候因为没有登录，所以需要先登录，密码在配置的Java类中可以看到，分别是user/password，输入，登录，进去了，成功！



* 剩下的就是写自己的逻辑了
