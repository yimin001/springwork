package com.theodore.utils;

import java.sql.SQLException;

public class TransactionManager {

//    private TransactionManager(){
//
//    }
//
//    private static TransactionManager transactionManager = new TransactionManager();
//
//    public static TransactionManager getInstance(){
//        return transactionManager;
//    }

    private ConnectionUtils connectionUtils;

    public void setConnectionUtils(ConnectionUtils connectionUtils) {
        this.connectionUtils = connectionUtils;
    }

    //开启事务
    public void beginTransaction() throws SQLException {
        connectionUtils.getConnection().setAutoCommit(false);
    }

    //提交事务
    public void commit() throws SQLException {
        connectionUtils.getConnection().commit();
    }

    //回滚事务
    public void rollback() throws SQLException {
        connectionUtils.getConnection().rollback();
    }

}
