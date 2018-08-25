package com.github.dhirabayashi.bbs.dao;

import com.github.dhirabayashi.bbs.db.DBManager;
import com.github.dhirabayashi.bbs.dto.LogDTO;
import com.github.dhirabayashi.bbs.util.Utils;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class LogDAO {
    public void insert(LogDTO log) throws SQLException {
        var sql = "insert into log values (?, ?, ?, ?, ?, ?, ?)";
        try(Connection con = DBManager.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)) {
            con.setAutoCommit(true);

            pstmt.setInt(1, getLogIdFromSequence());
            pstmt.setString(2, log.getName());
            pstmt.setString(3, log.getUrl());
            pstmt.setString(4, log.getMessage());
            pstmt.setBlob(5, log.getImage());
            pstmt.setString(6, log.getPassword()); // 本当は暗号化すべきだが
            pstmt.setTimestamp(7, log.getWriteTime());

            pstmt.executeUpdate();
        }
    }

    public List<LogDTO> selectAll() throws SQLException, IOException {
        var sql = "select log_id, name, url, message, image, password, write_time from log order by write_time desc";

        List<LogDTO> logs = new ArrayList<>();
        try (Connection con = DBManager.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rset = pstmt.executeQuery()) {
            while (rset.next()) {
                var log = new LogDTO();
                log.setLogId(rset.getInt("log_id"));
                log.setName(rset.getString("name"));
                log.setUrl(rset.getString("url"));
                log.setMessage(rset.getString("message"));
                log.setImage(rset.getBlob("image"));
                log.setPassword(rset.getString("password"));
                log.setWriteTime(rset.getTimestamp("write_time"));

                log.setImageByte(Utils.inputStreamToBytes(log.getImage().getBinaryStream()));

                logs.add(log);
            }
        }
        return logs;
    }

    public LogDTO select(int logId) throws SQLException {
        var sql = "select log_id, name, url, message, password, write_time from log where log_id = ?";
        ResultSet rset = null;
        try(Connection con = DBManager.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, logId);
            rset = pstmt.executeQuery();

            if(rset.next()) {
                var log = new LogDTO();
                log.setLogId(rset.getInt("log_id"));
                log.setName(rset.getString("name"));
                log.setUrl(rset.getString("url"));
                log.setMessage(rset.getString("message"));
                log.setPassword(rset.getString("password"));
                log.setWriteTime(rset.getTimestamp("write_time"));
                return log;
            } else {
                return null;
            }
        } finally {
            if(rset != null) {
                rset.close();
            }
        }
    }

    public void delete(int logId) throws SQLException {
        var sql = "delete from log where log_id = ?";
        try(Connection con = DBManager.getConnection();
            PreparedStatement pstmt = con.prepareStatement(sql)) {
            con.setAutoCommit(true);

            pstmt.setInt(1, logId);
            pstmt.executeUpdate();
        }
    }

    private int getLogIdFromSequence() throws SQLException {
        var sql = "select next value for log_seq from dual";
        try (Connection con = DBManager.getConnection();
             PreparedStatement pstmt = con.prepareStatement(sql);
             ResultSet rset = pstmt.executeQuery()) {

            rset.next();
            return rset.getInt("C1");
        }
    }
}
