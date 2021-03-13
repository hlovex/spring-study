package com.hlovex.demo;

import com.hlovex.demo.service.UserService;
import com.hlovex.spring.HlovexApplicationContext;

public class Test {
    public static void main(String[] args) throws IllegalAccessException, InstantiationException, ClassNotFoundException {

        // 启动spring 创建单例bean
        HlovexApplicationContext context = new HlovexApplicationContext(AppConfig.class);

        UserService userService = (UserService) context.getBean("userService");
        userService.test();
    }
}
