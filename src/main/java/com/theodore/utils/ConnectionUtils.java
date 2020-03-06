package com.theodore.utils;

import java.sql.Connection;
import java.sql.SQLException;

public class ConnectionUtils {

//    private ConnectionUtils(){
//
//    }
//
//    private static ConnectionUtils connectionUtils = new ConnectionUtils();
//
//    public static ConnectionUtils getInstance(){
//        return connectionUtils;
//    }

    private ThreadLocal<Connection> threadLocal = new ThreadLocal();

    public Connection getConnection(){
        Connection connection = threadLocal.get();
        if (connection == null){
            try {
                connection = DruidUtils.getInstance().getConnection();
                threadLocal.set(connection);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
        return connection;
    }
}
