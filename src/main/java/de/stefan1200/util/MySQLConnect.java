/*
 * Decompiled with CFR 0_110.
 */
package de.stefan1200.util;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

public class MySQLConnect {
    private Connection dbConnection = null;
    private String url = null;
    private String username = null;
    private String password = null;

    public MySQLConnect(String mysqlHost, int mysqlPort, String mysqlDB, String mysqlUser, String mysqlPW) throws ClassNotFoundException {
        if (mysqlPort < 0 || mysqlPort > 65535) {
            mysqlPort = 3306;
        }
        if (mysqlHost == null) {
            mysqlHost = "localhost";
        }
        if (mysqlDB == null) {
            mysqlDB = "";
        }
        if (mysqlUser == null) {
            mysqlUser = "root";
        }
        if (mysqlPW == null) {
            mysqlPW = "";
        }
        this.url = "jdbc:mysql://" + mysqlHost + ":" + Integer.toString(mysqlPort) + "/" + mysqlDB;
        this.username = mysqlUser;
        this.password = mysqlPW;
        Class.forName("com.mysql.jdbc.Driver");
    }

    public void close() {
        try {
            if (!this.dbConnection.isClosed()) {
                this.dbConnection.close();
            }
        }
        catch (Exception var1_1) {
            // empty catch block
        }
    }

    public void connect() throws SQLException {
        if (this.dbConnection == null || this.dbConnection.isClosed()) {
            this.dbConnection = DriverManager.getConnection(this.url, this.username, this.password);
        }
    }

    public Connection getConnection() {
        return this.dbConnection;
    }

    public PreparedStatement getPreparedStatement(String sql) throws SQLException {
        return this.dbConnection.prepareStatement(sql);
    }

    public Statement getStatement() throws SQLException {
        return this.dbConnection.createStatement();
    }
}

