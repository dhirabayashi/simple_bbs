package com.github.dhirabayashi.bbs.dto;

import org.apache.commons.lang3.StringUtils;

import java.sql.Blob;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class LogDTO {
    private int logId;
    private String name;
    private String url;
    private String message;
    private String password;
    private Timestamp writeTime;

    public int getLogId() {
        return logId;
    }

    public void setLogId(int logId) {
        this.logId = logId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Timestamp getWriteTime() {
        return writeTime;
    }

    public void setWriteTime(Timestamp writeTime) {
        this.writeTime = writeTime;
    }

    public List<String> validate() {
        List<String> errorMessage = new ArrayList<>();
        if(StringUtils.isBlank(message)) {
            errorMessage.add("本文を入力してください。");
        }

        if(StringUtils.isBlank(password)) {
            errorMessage.add("削除パスワードを入力してください。");
        }

        if(!StringUtils.isBlank(url) && !url.matches("^https?://.*")) {
            errorMessage.add("URLの形式が正しくありません。");
        }

        return errorMessage;
    }
}
