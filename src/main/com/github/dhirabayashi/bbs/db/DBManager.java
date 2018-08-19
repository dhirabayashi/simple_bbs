package com.github.dhirabayashi.bbs.db;

import org.hsqldb.Server;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBManager {
    private DBManager(){}

    public static void startDB() {
        var file = DBManager.class.getClassLoader()
                .getResource("db.script")
                .toString()
                .replace("db.script", "db");

        Server db = new Server();
        db.setDatabasePath(0, file);
        db.setDatabaseName(0, "db");
        db.start();
    }

    public static Connection getConnection() throws SQLException {
        // 本当はコネクションプーリングを使うべきだが
        return DriverManager.getConnection("jdbc:hsqldb:hsql://localhost/db", "SA", "");
    }
}
