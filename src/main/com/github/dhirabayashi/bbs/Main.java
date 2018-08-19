package com.github.dhirabayashi.bbs;

import com.github.dhirabayashi.bbs.dao.LogDAO;
import com.github.dhirabayashi.bbs.dao.UserDAO;
import com.github.dhirabayashi.bbs.db.DBManager;
import com.github.dhirabayashi.bbs.dto.LogDTO;
import com.github.dhirabayashi.bbs.dto.UserDTO;
import org.apache.commons.lang3.StringUtils;
import spark.ModelAndView;
import spark.Session;
import spark.template.thymeleaf.ThymeleafTemplateEngine;

import java.sql.Timestamp;
import java.util.HashMap;
import java.util.Map;

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
            String userName = req.queryParams("userName");
            String url = req.queryParams("url");
            String message = req.queryParams("message");
            String password = req.queryParams("password");

            LogDTO log = new LogDTO();
            log.setName(userName);
            log.setUrl("".equals(url) ? null : url);
            log.setMessage(message);
            log.setPassword(password);
            log.setWriteTime(new Timestamp(new java.util.Date().getTime()));

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

}
