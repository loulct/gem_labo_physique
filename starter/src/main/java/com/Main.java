package com.example.starter;

import java.util.Arrays;
import java.util.TreeMap;
import java.util.NavigableMap;
import java.io.File;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.lang.Integer;
import java.util.Properties;
import java.io.OutputStream;
import java.io.FileOutputStream;
import java.io.FileInputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;

import java.io.FileNotFoundException;
import java.io.IOException;

import io.vertx.core.Launcher;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.ext.web.RoutingContext;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.sstore.LocalSessionStore;
import io.vertx.ext.auth.properties.PropertyFileAuthentication;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class Main extends AbstractVerticle{
    public static void main(String[] args) {
        Launcher.executeCommand("run", Main.class.getName());
    }

    private NavigableMap<String, JsonObject> tools = new TreeMap<>();

    @Override
    public void start() throws Exception {

        setUpInitialData();

        Router router = Router.router(vertx);

        HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);
        
        router.route().handler(BodyHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        PropertyFileAuthentication authn = PropertyFileAuthentication.create(vertx, "vertx-users.properties");

        router.route("/private/tools").handler(RedirectAuthHandler.create(authn, "/login.html"));

        router.route("/private/*").handler(StaticHandler.create("src/main/resources/private").setCachingEnabled(false));

        router.errorHandler(404, routingContext -> {
            routingContext.response().setStatusCode(302).putHeader("Location", "/private/tools").end();
        });

        router.route("/private/tools/:toolID").handler(context -> handleGetTool(context, engine));
        router.route("/private/add").handler(this::handleAddTool);
        router.route("/private/tools").handler(context -> handleListTool(context, engine));

        router.route("/loginhandler").handler(FormLoginHandler.create(authn).setDirectLoggedInOKURL("/private/tools"))
            .failureHandler(context -> {
                context.response()
                    .putHeader("location", "/login.html")
                    .setStatusCode(302)
                    .end();
            }
        );

        router.route("/forgotpassword").handler(context -> {

            String userEmail = context.request().getParam("username").toLowerCase();
            String uuid = UUID.randomUUID().toString();

            JsonObject data = new JsonObject().put("username", userEmail).put("password", uuid);

            Properties properties = new Properties();
            try{
                properties.load(new FileInputStream("src/main/resources/vertx-users.properties"));
                if(properties.getProperty("user." + userEmail) != null){
                    properties.put("user." + userEmail, uuid + ",user");
                    FileOutputStream outputStream = new FileOutputStream("src/main/resources/vertx-users.properties");
                    properties.store(outputStream, null);

                    engine.render(data, "private/password.hbs", res -> {
                        if(res.succeeded()){
                            MailMessage email = new MailMessage()
                                .setFrom("gem-labo-physique@gem-labo.com")
                                .setTo(Arrays.asList(
                                    userEmail,
                                    "admin@gem-labo.com"))
                                .setBounceAddress("gem-labo-physique@gem-labo.com")
                                .setSubject("GEM LABO PHYSIQUE : Mot de passe mis à jour")
                                .setHtml(res.result().toString());

                            resultsMail(email);

                            context.response().putHeader("location", "/login.html").setStatusCode(302).end();
                        }else{
                            context.fail(res.cause());
                        }
                    });
                }else{
                    System.out.println("Ce compte n'existe pas");
                    context.response()
                        .putHeader("location", "/forgotpassword.html")
                        .setStatusCode(302)
                        .end();
                    // TODO add alert message in html
                }
            }catch(IOException e){
                System.out.println(e);
            }
        });

        router.route("/signuphandler").handler(context -> {

            String userEmail = context.request().getParam("lastname").toLowerCase() + 
                "." + 
                context.request().getParam("firstname").toLowerCase() +
                "@gem-labo.com";

            String uuid = UUID.randomUUID().toString();

            JsonObject data = new JsonObject().put("username", userEmail).put("password", uuid);

            Properties properties = new Properties();
            try{
                properties.load(new FileInputStream("src/main/resources/vertx-users.properties"));
                if(properties.getProperty("user." + userEmail) == null){
                    properties.put("user." + userEmail, uuid + ",user");
                    FileOutputStream outputStream = new FileOutputStream("src/main/resources/vertx-users.properties");
                    properties.store(outputStream, null);

                    engine.render(data, "private/setup.hbs", res -> {
                        if(res.succeeded()){
                            MailMessage email = new MailMessage()
                                .setFrom("gem-labo-physique@gem-labo.com")
                                .setTo(Arrays.asList(
                                    userEmail,
                                    "admin@gem-labo.com"))
                                .setBounceAddress("gem-labo-physique@gem-labo.com")
                                .setSubject("GEM LABO PHYSIQUE : Votre compte a été créé")
                                .setHtml(res.result().toString());
        
                            resultsMail(email);
        
                            context.response().putHeader("location", "/login.html").setStatusCode(302).end();
                        }else{
                            context.fail(res.cause());
                        }
                    });
                }else{
                    System.out.println("Ce compte existe déjà !");
                    context.response()
                        .putHeader("location", "/signup.html")
                        .setStatusCode(302)
                        .end();
                    // TODO add alert message in html
                }
            }catch(IOException e){
                System.out.println(e);
            }
        });

        router.route("/logout").handler(context -> {
            context.clearUser();

            context.response().putHeader("location", "/login.html").setStatusCode(302).end();
        });

        router.route().handler(StaticHandler.create("src/main/resources/webroot"));

        vertx.createHttpServer().requestHandler(router).listen(8888);
    }

    public void resultsMail(MailMessage email) {
        MailClient mailClient = MailClient.createShared(vertx, new MailConfig().setPort(25));

        mailClient.sendMail(email, result -> {
            if (result.succeeded()) {
                System.out.println(result.result());
                System.out.println("Mail sent");
            } else {
                System.out.println("got exception");
                result.cause().printStackTrace();
            }
        });
    }

    private void handleGetTool(RoutingContext context, HandlebarsTemplateEngine engine){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = tools.get(toolID);
            if(tool == null){
                sendError(404, response);
            }else{
                String username = context.user().principal().getString("username");

                tool.put("isAvailable", false).put("owner", username).put("returnDate", "test");

                JsonObject data = new JsonObject().put("tool", tool);

                engine.render(data, "private/borrowed.hbs", res -> {
                    if(res.succeeded()){
                        MailMessage email = new MailMessage()
                            .setFrom("gem-labo-physique@gem-labo.com")
                            .setTo(Arrays.asList(
                                username,
                                "admin@gem-labo.com"))
                            .setBounceAddress("gem-labo-physique@gem-labo.com")
                            .setSubject("GEM LABO PHYSIQUE : Matériel emprunté !")
                            .setHtml(res.result().toString());

                        resultsMail(email);

                        response.putHeader("location", "/private/tools").setStatusCode(302).end();

                    }else{
                        context.fail(res.cause());
                    }
                });
            }
        }
    }

    private void handleAddTool(RoutingContext context){
        String brand = context.request().getParam("brand");
        String model = context.request().getParam("model");
        String desc = context.request().getParam("desc");
        Integer idISEP = Integer.parseInt(tools.lastEntry().getKey()) + 1;

        addTool(new JsonObject().put("idISEP", idISEP).put("brand", brand).put("model", model).put("desc", desc).put("isAvailable", true).put("owner", context.user().principal().getString("username")).put("returnDate", null));

        HttpServerResponse response = context.response();
        response.putHeader("location", "/private/tools").setStatusCode(302).end();
    }

    private void handleListTool(RoutingContext context, HandlebarsTemplateEngine engine){
        JsonObject data = new JsonObject().put("column1", "id")
            .put("column2", "Marque")
            .put("column3", "Modèle")
            .put("column4", "Description")
            .put("column5", "Disponible?")
            .put("column6", "Emprunté par")
            .put("column7", "Date de retour");


        Map<String, JsonObject> arr = tools.entrySet().stream()
            .filter(e -> (e.getValue().getString("owner") == null || e.getValue().getString("owner") == context.user().principal().getString("username")))
            .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));

        data.put("table", arr);

        data.put("user", context.user().principal().getString("username"));

        engine.render(data, "private/main.hbs", res -> {
            if(res.succeeded()){
                context.response().end(res.result());
            }else{
                context.fail(res.cause());
            }
        });
    }

    private void sendError(int statusCode, HttpServerResponse response){
        response.setStatusCode(statusCode).end();
    }

    private void setUpInitialData(){
        addTool(new JsonObject().put("idISEP", 1).put("brand", "Steinberg").put("model", "SBS-LZ-4000/20-12").put("desc", "Centrifugeuse").put("isAvailable", true).put("owner", null).put("returnDate", null));
        addTool(new JsonObject().put("idISEP", 2).put("brand", "Stamos Soldering").put("model", "S-LS-28").put("desc", "Alimentation double").put("isAvailable", true).put("owner", null).put("returnDate", null));
        addTool(new JsonObject().put("idISEP", 3).put("brand", "Steinberg").put("model", "SBS-ER-3000").put("desc", "Agitateur électrique").put("isAvailable", true).put("owner", null).put("returnDate", null));
        addTool(new JsonObject().put("idISEP", 4).put("brand", "Steinberg").put("model", "SBS-ER-3000").put("desc", "Agitateur électrique").put("isAvailable", false).put("owner", null).put("returnDate", "test"));
    }

    private void addTool(JsonObject tool){
        tools.put(tool.getString("idISEP"), tool);
        System.out.println(tool);
    }
}
