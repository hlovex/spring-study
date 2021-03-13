package com.hlovex.spring;

import java.io.File;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HlovexApplicationContext {

    private Class configClass;

    private Map<String, BeanDefinition> beanDefinitionMap = new ConcurrentHashMap<>();
    private Map<String, Object> singletonObjects = new ConcurrentHashMap<>();
    private List<BeanPostProcessor> listBeanPostProcessor = new ArrayList<>();

    public HlovexApplicationContext(Class configClass) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        this.configClass = configClass;

        // 扫描，得到class
        List<Class> listClass = scan(configClass);

        for (Class clazz : listClass) {
            parseClass(clazz);
        }

        // 基于class创建实例
        instanceSingletonBean();
    }

    private List<Class> scan(Class configClass) throws ClassNotFoundException {
        List<Class> listClass = new ArrayList<>();
        if (configClass.isAnnotationPresent(ComponentScan.class)) {
            ComponentScan componentScanAnnotation = (ComponentScan) configClass.getAnnotation(ComponentScan.class);
            String path = componentScanAnnotation.value(); // com.hlovex.demo.service
            path = path.replace(".", "/"); // com/hlovex/demo/service
            ClassLoader classLoader = HlovexApplicationContext.class.getClassLoader();
            URL url = classLoader.getResource(path);
            File dir = new File(url.getPath());
            if (dir.isDirectory()) {
                File[] files = dir.listFiles();
                for (File file : files) {
                    String absolutePath = file.getAbsolutePath();
                    absolutePath = absolutePath.substring(absolutePath.indexOf("com"), absolutePath.indexOf(".class"));
                    absolutePath = absolutePath.replaceAll("\\\\|/", ".");
                    Class<?> clazz = classLoader.loadClass(absolutePath);
                    listClass.add(clazz);
                }
            }
        }
        return listClass;
    }

    // 解析class --》 beanDefinition --》 放入beanDefinitionMap
    private void parseClass(Class clazz) throws IllegalAccessException, InstantiationException {
        if (clazz.isAnnotationPresent(Component.class)) {
            if (BeanPostProcessor.class.isAssignableFrom(clazz)) {
                BeanPostProcessor instance = (BeanPostProcessor) clazz.newInstance();
                listBeanPostProcessor.add(instance);
            }
            BeanDefinition beanDefinition = new BeanDefinition();
            beanDefinition.setClazz(clazz);

            Component componentAnnotation = (Component) clazz.getAnnotation(Component.class);
            String beanName = componentAnnotation.value();

            if (clazz.isAnnotationPresent(Scope.class)) {
                Scope scopeAnnotation = (Scope) clazz.getAnnotation(Scope.class);
                beanDefinition.setScope(scopeAnnotation.value());
            } else {
                beanDefinition.setScope("singleton");
            }
            beanDefinitionMap.put(beanName, beanDefinition);
        }
    }

    private void instanceSingletonBean() throws InstantiationException, IllegalAccessException {
        for (String beanName : beanDefinitionMap.keySet()) {
            BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);

            if (beanDefinition.getScope().equals("singleton")) {
                // 创建bean
                if (!singletonObjects.containsKey(beanName)) {
                    Object bean = doCreateBean(beanName, beanDefinition);
                    singletonObjects.put(beanName, bean);
                }
            }
        }
    }

    private Object doCreateBean(String beanName, BeanDefinition beanDefinition) throws IllegalAccessException, InstantiationException {
        // 实例化
        Class beanClass = beanDefinition.getClazz();
        Object bean = beanClass.newInstance();

        // 设置属性
        Field[] fields = beanClass.getDeclaredFields();
        for (Field field : fields) {
            if (field.isAnnotationPresent(Autowired.class)) {
                Object beanField = getBean(field.getName());
                if (null == beanField) {
                }
                field.setAccessible(true);
                field.set(bean, beanField);
            }
        }

        // aware
        if (bean instanceof BeanNameAware) {
            ((BeanNameAware) bean).setBeanName(beanName);
        }

        for (BeanPostProcessor beanPostProcessor : listBeanPostProcessor) {
            beanPostProcessor.postProcessBeforeInitialization(bean, beanName);
        }

        // 初始化
        if (bean instanceof InitializingBean) {
            ((InitializingBean) bean).afterPropertiesSet();
        }

        for (BeanPostProcessor beanPostProcessor : listBeanPostProcessor) {
            beanPostProcessor.postProcessAfterInitialization(bean, beanName);
        }
        return bean;
    }

    public Object getBean(String beanName) throws InstantiationException, IllegalAccessException {
        BeanDefinition beanDefinition = beanDefinitionMap.get(beanName);
        if (beanDefinition.getScope().equals("prototype")) {
            // 创建bean
            Object bean = doCreateBean(beanName, beanDefinition);
            return bean;
        } else if (beanDefinition.getScope().equals("singleton")) {
            // 单例池直接取
            Object bean = singletonObjects.get(beanName);
            if (null == bean) {
                bean = doCreateBean(beanName, beanDefinition);
                singletonObjects.put(beanName, bean);
            }
            return bean;
        }

        return null;
    }
}
