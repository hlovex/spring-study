package com.hlovex.demo.service;

import com.hlovex.spring.BeanPostProcessor;
import com.hlovex.spring.Component;

@Component
public class HlovexBeanPostProcessor implements BeanPostProcessor {
    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) {
        System.out.println(beanName + " 初始化之前");
        return null;
    }

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) {
        System.out.println(beanName + " 初始化之后");
        return null;
    }
}
