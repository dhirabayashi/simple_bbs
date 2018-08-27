package com.github.dhirabayashi.bbs;

import com.github.dhirabayashi.bbs.dao.LogDAO;
import com.github.dhirabayashi.bbs.dao.UserDAO;
import com.github.dhirabayashi.bbs.db.DBManager;
import com.github.dhirabayashi.bbs.dto.LogDTO;
import com.github.dhirabayashi.bbs.dto.UserDTO;
import com.github.dhirabayashi.bbs.util.Utils;
import org.apache.commons.lang3.StringUtils;
import spark.ModelAndView;
import spark.Request;
import spark.Session;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import javax.servlet.MultipartConfigElement;
import javax.sql.rowset.serial.SerialBlob;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static spark.Spark.*;

public class Main {
    public static void main(String[] args) {
        DBManager.startDB();

        // セッションのチェック
        before("/session/*", (req, res) -> {
            Session session = req.session(false);
            if(session == null) {
                halt(401, "ログインしていないか、タイムアウトしました。");
            }
        });

        // ログインページの表示
        get("/login", (req, res) -> forward("login"));

        // ログイン処理
        post("/login", (req, res) -> {
            String userName = req.queryParams("userName");
            String password = req.queryParams("password");

            // 入力チェック
            if(StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", "ユーザ名またはパスワードが入力されていません。");
                return forward("login", model);
            }

            // ログイン判定
            var dao = new UserDAO();
            var user = dao.select(userName);

            if(user == null || !user.getPassword().equals(password)) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", "ユーザ名またはパスワードが違います。");
                return forward("login", model);
            }

            Session session = req.session();
            session.attribute("__user_name__", userName);

            return forward("login_complete");
        });

        // 投稿表示
        get("/session/index", (req, res) -> {
            var dao = new LogDAO();
            var logs = dao.selectAll();

            // 画像処理
            for(LogDTO log : logs) {
                var image = log.getImageByte();
                var imageName = log.getImageName();

                Session session = req.session(false);
                session.attribute(imageName, image);
            }

            Map<String, Object> model = new HashMap<>();
            model.put("logs", logs);
            return forward("index", model);
        });

        // 投稿ページ遷移
        get("/session/post", (req, res) -> {
            Session session = req.session(false);
            String userName = session.attribute("__user_name__");
            Map<String, Object> model = new HashMap<>();
            model.put("userName", userName);

            return forward("post", model);
        });

        // 投稿処理
        post("/session/post", (req, res) -> {
            req.attribute("org.eclipse.jetty.multipartConfig", new MultipartConfigElement(System.getProperty("java.io.tmpdir")));

            // enctype="multipart/form-data"だと普通の方法ではリクエストパラメータを取得できない
            String userName = getString(req,"userName");
            String url = getString(req, "url");
            String message = getString(req, "message");
            String password = getString(req, "password");

            LogDTO log = new LogDTO();
            log.setName(userName);
            log.setUrl("".equals(url) ? null : url);
            log.setMessage(message);
            log.setPassword(password);
            log.setWriteTime(new Timestamp(new java.util.Date().getTime()));

            try(var input = req.raw().getPart("image").getInputStream()) {
                log.setImage(new SerialBlob(Utils.inputStreamToBytes(input)));
            }

            // 画像ファイル名
            // 拡張子の取得
            var header = req.raw().getPart("image").getHeader("Content-Disposition");
            var pattern = Pattern.compile("filename=\".*\\.(.*)\"");
            var m = pattern.matcher(header);

            var filename = String.valueOf(System.nanoTime());
            if(m.find()) {
                var extension = m.group(1);
                filename = filename + "." + extension;
            }
            log.setImageName(filename);

            // バリデーション
            var errorMessage = log.validate();
            if(!errorMessage.isEmpty()) {
                Map<String, Object> model = new HashMap<>();
                model.put("errorMessage", errorMessage);
                model.put("userName", userName);

                return forward("post", model);
            }

            // 登録
            var dao = new LogDAO();
            dao.insert(log);

            return forward("post_complete");
        });

        // 削除ページ遷移
        get("/session/delete/:id", (req, res) -> {
            var id = Integer.parseInt(req.params("id"));
            Map<String, Object> model = new HashMap<>();
            model.put("id", id);

            return forward("delete", model);
        });

        // 削除処理
        post("/session/delete", (req, res) -> {
            String password = req.queryParams("password");
            var id = Integer.parseInt(req.queryParams("id"));

            // 入力チェック
            if(StringUtils.isBlank(password)) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", "パスワードが入力されていません。");
                model.put("id", id);

                return forward("delete", model);
            }

            // パスワード照合
            var dao = new LogDAO();
            var log = dao.select(id);
            if(!password.equals(log.getPassword())) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", "パスワードが違います。");
                model.put("id", id);

                return forward("delete", model);
            }

            // 削除
            dao.delete(id);

            return forward("delete_complete");
        });

        // ユーザ登録ページへ遷移
        get("/user_add", (req, res) -> forward("user_add"));

        // ユーザ登録処理
        post("/user_add", (req, res) -> {
            String userName = req.queryParams("userName");
            String password = req.queryParams("password");

            // 入力チェック
            if(StringUtils.isBlank(userName) || StringUtils.isBlank(password)) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", "ユーザ名またはパスワードが入力されていません。");
                return forward("user_add", model);
            }

            var dao = new UserDAO();
            var dbUser = dao.select(userName);
            if(dbUser != null) {
                Map<String, Object> model = new HashMap<>();
                model.put("message", "既に使用されているユーザ名です。");
                return forward("user_add", model);
            }

            // 登録
            var user = new UserDTO();
            user.setName(userName);
            user.setPassword(password);

            dao.insert(user);

            return forward("user_add_complete");
        });

        // 画像取得
        get("/session/*", (req, res) -> {
            var filename = req.splat()[0];
            var out = res.raw().getOutputStream();

            var session = req.session(false);
            var bytes = (byte[])session.attribute(filename);
            out.write(bytes);
            return res;
        });
    }

    private static String forward(String viewName) {
        Map<String, Object> model = new HashMap<>();
        return forward(viewName, model);
    }

    private static String forward(String viewName, Map<String, Object> model) {
        return new ThymeleafTemplateEngine().render(
                new ModelAndView(model, viewName)
        );
    }

    private static String getString(Request req, String name) throws Exception {
        List<String> list = new ArrayList<>();
        try(BufferedReader br = new BufferedReader(new InputStreamReader(req.raw().getPart(name).getInputStream()))) {
            String line;
            while((line = br.readLine()) != null) {
                list.add(line);
            }
        }
        return String.join(System.getProperty("line.separator"), list);
    }
}
