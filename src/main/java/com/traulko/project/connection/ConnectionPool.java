package com.traulko.project.connection;

import com.traulko.project.exception.ConnectionDatabaseException;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

public class ConnectionPool {
    private static final Logger LOGGER = LogManager.getLogger(ConnectionPool.class);
    private static final String DRIVER_NAME = "com.mysql.jdbc.Driver";
    private static final String LOGIN = "root";
    private static final String PASSWORD = "1234";
    private static final String URL = "jdbc:mysql://localhost:3306/project";
    private static final int POOL_SIZE = 32;
    private static ConnectionPool instance;
    private static volatile boolean instanceIsCreated;

    private BlockingQueue<ProxyConnection> freeConnections;
    private Queue<ProxyConnection> proxyConnections;

    public static ConnectionPool getInstance() {
        if (!instanceIsCreated) {
            synchronized (ConnectionPool.class) {
                if (!instanceIsCreated) {
                    instance = new ConnectionPool();
                    instanceIsCreated = true;
                }
            }
        }
        return instance;
    }

    private ConnectionPool() {
        try {
            Class.forName(DRIVER_NAME);
            freeConnections = new LinkedBlockingDeque<>(POOL_SIZE);
            proxyConnections = new ArrayDeque<>(POOL_SIZE);

            for (int i = 0; i < POOL_SIZE; i++) {
                freeConnections.offer(new ProxyConnection(DriverManager.getConnection(URL, LOGIN, PASSWORD)));
            }
            LOGGER.log(Level.INFO, "Connection pool has been filled");
        } catch (ClassNotFoundException | SQLException e) {
            LOGGER.log(Level.FATAL, "Error during connection pool creating");
            System.out.println("Error while connection pool creating " + e);
        }
    }

    public Connection getConnection() throws ConnectionDatabaseException {
        ProxyConnection connection = null;
        try {
            connection = freeConnections.take();
            proxyConnections.offer(connection);
            LOGGER.log(Level.DEBUG, "Connection has been given");
        } catch (InterruptedException e) {
            LOGGER.log(Level.ERROR, e);
            throw new ConnectionDatabaseException(e);
        }
        return connection;
    }

    public void releaseConnection(Connection connection) {
        if (connection.getClass() == ProxyConnection.class) {
            proxyConnections.remove(connection);
            freeConnections.offer((ProxyConnection) connection);
            LOGGER.log(Level.DEBUG, "Connection has been released");
        } else {
            LOGGER.log(Level.WARN, "Invalid connection to release");
        }
    }

    public void destroyPool() throws ConnectionDatabaseException {
        try {
            for (int i = 0; i < POOL_SIZE; i++) {
                freeConnections.take().reallyClose();
            }
            LOGGER.log(Level.INFO, "Connection pool has been destroyed");
            deregisterDrivers();
        } catch (SQLException | InterruptedException e) {
            LOGGER.log(Level.ERROR, e);
            throw new ConnectionDatabaseException("Error while close connection pool", e);
        }
    }

    private void deregisterDrivers() throws ConnectionDatabaseException {
        try {
            while(DriverManager.getDrivers().hasMoreElements()){
                DriverManager.deregisterDriver(DriverManager.getDrivers().nextElement());
            }
            LOGGER.log(Level.INFO, "Drivers have been deregistered");
        } catch (SQLException e) {
            throw new ConnectionDatabaseException("Error while deregister drivers ", e);
        }
    }
}
