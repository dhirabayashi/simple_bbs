package com.github.dhirabayashi.bbs.dao;

import com.github.dhirabayashi.bbs.db.DBManager;
import com.github.dhirabayashi.bbs.dto.UserDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public void insert(UserDTO user) throws SQLException {
        var sql = "insert into user values (?, ?)";
        try(Connection con = DBManager.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)) {
            con.setAutoCommit(true);

            pstmt.setString(1, user.getName());
            pstmt.setString(2, user.getPassword()); // 本当は暗号化すべきだが

            pstmt.executeUpdate();
        }
    }

    public UserDTO select(String name) throws SQLException {
        var sql = "select name, password from user where name = ?";
        ResultSet rset = null;
        try(Connection con = DBManager.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, name);
            rset = pstmt.executeQuery();

            if(rset.next()) {
                var user = new UserDTO();
                user.setName(rset.getString("name"));
                user.setPassword(rset.getString("password"));
                return user;
            } else {
                return null;
            }

        } finally {
            if(rset != null) {
                rset.close();
            }
        }
    }
}
