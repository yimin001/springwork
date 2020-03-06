package com.theodore.factory;

import com.alibaba.druid.util.StringUtils;
import com.theodore.sterotype.*;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.io.SAXReader;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLDecoder;
import java.util.*;

public class BeanFactory {


    private static Map<String, Object> beanMap = new HashMap<String, Object>();


     static {
        // 任务一 加载
        InputStream resourceAsStream = BeanFactory.class.getClassLoader().getResourceAsStream("beans.xml");
        // 解析
        try {
            Document read = new SAXReader().read(resourceAsStream);
            Element rootElement = read.getRootElement();//<beans>

            List<Element> beanList = rootElement.selectNodes("//bean");
            for (int i = 0; i < beanList.size(); i++) {
                Element element =  beanList.get(i);
                // 处理每个bean元素，获取到该元素的id 和 class 属性
                String id = element.attributeValue("id");        //
                String clazz = element.attributeValue("class");  //
                // 通过反射技术实例化对象
                Class<?> aClass = Class.forName(clazz);
                Object o = aClass.newInstance();  // 实例化之后的对象
                // 存储到map中待用
                beanMap.put(id,o);
            }

         // 实例化完成之后维护对象的依赖关系，检查哪些对象需要传值进入，根据它的配置，我们传入相应的值
            // 有property子元素的bean就有传值需求
            List<Element> propertyList = rootElement.selectNodes("//property");
            // 解析property，获取父元素
            for (int i = 0; i < propertyList.size(); i++) {
                Element element =  propertyList.get(i);   //<property name="AccountDao" ref="accountDao"></property>
                String name = element.attributeValue("name");
                String ref = element.attributeValue("ref");

                // 找到当前需要被处理依赖关系的bean
                Element parent = element.getParent();

                // 调用父元素对象的反射功能
                String parentId = parent.attributeValue("id");
                Object parentObject = beanMap.get(parentId);
                // 遍历父对象中的所有方法，找到"set" + name
                Method[] methods = parentObject.getClass().getMethods();
                for (int j = 0; j < methods.length; j++) {
                    Method method = methods[j];
                    if(method.getName().equalsIgnoreCase("set" + name)) {  // 该方法就是 setAccountDao(AccountDao accountDao)
                        method.invoke(parentObject,beanMap.get(ref));
                    }
                }
                // 把处理之后的parentObject重新放到map中
                beanMap.put(parentId,parentObject);

            }
            // 获取扫描包
            List<Element> componentScan = rootElement.selectNodes("//component-scan");
            Enumeration<URL> dirs = null;
            Set<Class<?>> classes = new LinkedHashSet<Class<?>>();
            for (int i = 0; i < componentScan.size(); i++) {
                Element element = componentScan.get(i);
                //<component-scan base-package="com.theodore"/>
                String basePackage = element.attributeValue("base-package");
                // 获取包下面的所有类
                List<Class<?>> classsFromPackage = getClassFromPackage(basePackage);
                for (Class<?> aClass : classsFromPackage) {
                    // 找到service
                    if (aClass.isAnnotationPresent(Service.class)){
                        Object o = aClass.newInstance();
                        Field[] declaredFields = aClass.getDeclaredFields();
                        for (Field declaredField : declaredFields) {
                            if (declaredField.isAnnotationPresent(Autowired.class)){
                                String autName = null;
                                if (declaredField.isAnnotationPresent(Repository.class)){
                                    autName = declaredField.getAnnotation(Repository.class).value();
                                }else {
                                    autName = declaredField.getName();
                                }
                                declaredField.setAccessible(true);
                                declaredField.set(o, beanMap.get(autName));
                            }
                        }

                        if (aClass.isAnnotationPresent(Transactional.class)){
                            // 反射
                            ProxyFactory proxyFactory = (ProxyFactory) BeanFactory.getBean("proxyFactory");
                            Class<?>[] interfaces = aClass.getInterfaces();
                            // 是否实现接口
                            if (interfaces != null && interfaces.length >=1){
                                o = proxyFactory.getJdkProxy(o);
                            }else {
                                o = proxyFactory.getCglibProxy(o);
                            }

                        }

                        Service annotation = aClass.getAnnotation(Service.class);
                        String name = aClass.getSimpleName();
                        // beanId 头字母大小写转换
                        if (!Character.isLowerCase(name.charAt(0))) {
                            name = (new StringBuilder()).append(Character.toLowerCase(name.charAt(0))).append(name.substring(1)).toString();
                        }
                        if (StringUtils.isEmpty(annotation.value())){
                            beanMap.put(name, o);
                        }else {
                            beanMap.put(annotation.value(), o);
                        }

                    }

                }

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 通过包名获取下面所有类
     * @param packageName 包名
     * @return
     */
    public static List<Class<?>> getClassFromPackage(String packageName) {
        List<Class<?>> clazzs = new ArrayList<Class<?>>();
        // 是否循环搜索子包
        boolean recursive = true;
        // 包名对应的路径名称
        String packageDirName = packageName.replace('.', '/');
        Enumeration<URL> dirs;
        try {
            dirs = Thread.currentThread().getContextClassLoader().getResources(packageDirName);
            while (dirs.hasMoreElements()) {
                URL url = dirs.nextElement();
                String protocol = url.getProtocol();
                if ("file".equals(protocol)) {
                    String filePath = URLDecoder.decode(url.getFile(), "UTF-8");
                    findClassInPackageByFile(packageName, filePath, recursive, clazzs);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return clazzs;
    }

    /**
     * 在package对应的路径下找到所有的class
     * packageName 包名
     * filePath filePath文件路径
     */
    public static void findClassInPackageByFile(String packageName, String filePath, final boolean recursive,
                                                List<Class<?>> clazzs) {
        File dir = new File(filePath);
        if (!dir.exists() || !dir.isDirectory()) {
            return;
        }
        // 在给定的目录下找到所有的文件，并且进行条件过滤
        File[] dirFiles = dir.listFiles(new FileFilter() {
            public boolean accept(File file) {
                boolean acceptDir = recursive && file.isDirectory();// 接受dir目录
                boolean acceptClass = file.getName().endsWith("class");// 接受class文件
                return acceptDir || acceptClass;
            }
        });

        for (File file : dirFiles) {
            if (file.isDirectory()) {
                findClassInPackageByFile(packageName + "." + file.getName(), file.getAbsolutePath(), recursive, clazzs);
            } else {
                String className = file.getName().substring(0, file.getName().length() - 6);
                try {
                    clazzs.add(Thread.currentThread().getContextClassLoader().loadClass(packageName + "." + className));
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }


    public static Object getBean(String id) {

        return beanMap.get(id);
    }


}
