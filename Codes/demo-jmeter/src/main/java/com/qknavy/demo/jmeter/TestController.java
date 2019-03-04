package com.qknavy.demo.jmeter;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class TestController {

    @GetMapping(value = "/say")
    public String sayHello(String userName){
        System.out.println(userName + "来啦");
        return "你好，" + userName;
    }
}