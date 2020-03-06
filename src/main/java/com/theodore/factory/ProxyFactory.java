package com.theodore.factory;


import com.theodore.utils.TransactionManager;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.MethodInterceptor;
import net.sf.cglib.proxy.MethodProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

public class ProxyFactory {

    private TransactionManager transactionManager;

    public void setTransactionManager(TransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public Object getJdkProxy(final Object object) {
        return Proxy.newProxyInstance(object.getClass().getClassLoader(),
                object.getClass().getInterfaces(), new InvocationHandler() {
                    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                        Object result = null;
                        //事务控制
                        try {
                            transactionManager.beginTransaction();
                            result = method.invoke(object, args);
                            transactionManager.commit();
                        } catch (Exception e) {
                            e.printStackTrace();
                            transactionManager.rollback();
                            throw e;
                        }
                        return result;
                    }
                });
    }

    public Object getCglibProxy(final Object obj){
        return Enhancer.create(obj.getClass(), new MethodInterceptor() {
            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
                Object result = null;
                try{
                    transactionManager.beginTransaction();
                    result = method.invoke(obj,objects);
                    transactionManager.commit();
                }catch (Exception e){
                    e.printStackTrace();
                    transactionManager.rollback();
                    throw e;
                }
                return result;
            }
        });
    }

//    public Object getCglibProxy(final Object obj) {
//        return  Enhancer.create(obj.getClass(), new MethodInterceptor() {
//            @Override
//            public Object intercept(Object o, Method method, Object[] objects, MethodProxy methodProxy) throws Throwable {
//                Object result = null;
//                try{
//                    // 开启事务(关闭事务的自动提交)
//                    transactionManager.beginTransaction();
//                    result = method.invoke(obj,objects);
//                    // 提交事务
//                    transactionManager.commit();
//                }catch (Exception e) {
//                    e.printStackTrace();
//                    // 回滚事务
//                    transactionManager.rollback();
//                    // 抛出异常便于上层servlet捕获
//                    throw e;
//
//                }
//                return result;
//            }
//        });
//    }





}
