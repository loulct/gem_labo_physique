package com.example.starter;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.io.File;
import java.util.Scanner;
import java.nio.file.Path;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

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

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;


public class Main extends AbstractVerticle{
    public static void main(String[] args) {
        Launcher.executeCommand("run", Main.class.getName());
    }

    private Map<String, JsonObject> tools = new HashMap<>();

    @Override
    public void start() throws Exception {

        setUpInitialData();

        Router router = Router.router(vertx);
        
        router.route().handler(BodyHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        PropertyFileAuthentication authn = PropertyFileAuthentication.create(vertx, "vertx-users.properties");

        router.route("/private/*").handler(RedirectAuthHandler.create(authn, "/login.html"));

        router.route("/private/*").handler(StaticHandler.create("src/main/resources/private").setCachingEnabled(false));

        router.route("/private/tools/:toolID").handler(this::handleGetTool);
        router.route("/private/tools/:toolID").handler(this::handleAddTool);
        router.route("/private/tools").handler(this::handleListTool);

        router.route("/loginhandler").handler(FormLoginHandler.create(authn).setDirectLoggedInOKURL("/private/main.html"))
            .failureHandler(context -> {
                context.response()
                    .putHeader("location", "/login.html")
                    .setStatusCode(302)
                    .end();
            }
        );

        router.route("/forgotpassword").handler(context -> {

            String userEmail = context.request().getParam("username").toLowerCase();

            Path path = Paths.get("src/main/resources/emails/password.html");
            Charset charset = StandardCharsets.UTF_8;

            try{
                String content = new String(Files.readAllBytes(path), charset);
                String uuid = UUID.randomUUID().toString();

                content = content.replaceAll("<p>Mot de passe : </p>", "<p>Mot de passe : <strong>"+ uuid + "</strong></p>");

                MailMessage email = new MailMessage()
                .setFrom("gem-labo-physique@gem-labo.com")
                .setTo(Arrays.asList(
                    userEmail,
                    "admin@gem-labo.com"))
                .setBounceAddress("gem-labo-physique@gem-labo.com")
                .setSubject("GEM LABO PHYSIQUE : Mot de passe mis à jour")
                .setHtml(content);

                resultsMail(email);

            }catch(IOException e) {
                System.out.println(e);
            }

            context.response().putHeader("location", "/login.html").setStatusCode(302).end();
        });

        router.route("/signuphandler").handler(context -> {

            String userEmail = context.request().getParam("lastname").toLowerCase() + 
                "." + 
                context.request().getParam("firstname").toLowerCase() +
                "@gem-labo.com";
            
            Path path = Paths.get("src/main/resources/emails/setup.html");
            Charset charset = StandardCharsets.UTF_8;

            try{
                String content = new String(Files.readAllBytes(path), charset);
                String uuid = UUID.randomUUID().toString();

                content = content.replaceAll("<p>Adresse e-mail : </p>", "<p>Adresse e-mail : <strong>"+ userEmail + "</strong></p>");
                content = content.replaceAll("<p>Mot de passe : </p>", "<p>Mot de passe : <strong>"+ uuid + "</strong></p>");
                
                
                MailMessage email = new MailMessage()
                .setFrom("gem-labo-physique@gem-labo.com")
                .setTo(Arrays.asList(
                    userEmail,
                    "admin@gem-labo.com"))
                .setBounceAddress("gem-labo-physique@gem-labo.com")
                .setSubject("GEM LABO PHYSIQUE : Votre compte a été créé")
                .setHtml(content);

                resultsMail(email);
            }catch(IOException e){
                System.out.println(e);
            }

            context.response().putHeader("location", "/login.html").setStatusCode(302).end();
        });

        router.route("/logout").handler(context -> {
            context.clearUser();

            context.response().putHeader("location", "/").setStatusCode(302).end();
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

    private void handleGetTool(RoutingContext context){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = tools.get(toolID);
            if(tool == null){
                sendError(404, response);
            }else{
                response.putHeader("content-type", "application/json").end(tool.encodePrettily());
            }
        }
    }

    private void handleAddTool(RoutingContext context){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = context.getBodyAsJson();
            if(tool == null){
                sendError(400, response);
            }else{
                tools.put(toolID, tool);
                response.end();
            }
        }
    }

    private void handleListTool(RoutingContext context){
        JsonArray arr = new JsonArray();
        tools.forEach((k, v) -> arr.add(v));
        context.response().putHeader("content-type", "application/json").end(arr.encodePrettily());
    }

    private void sendError(int statusCode, HttpServerResponse response){
        response.setStatusCode(statusCode).end();
    }

    private void setUpInitialData(){
        addTool(new JsonObject().put("idISEP", "001").put("brand", "Steinberg").put("model", "SBS-LZ-4000/20-12").put("desc", "Centrifugeuse").put("isAvailable", true).put("returnDate", null));
        addTool(new JsonObject().put("idISEP", "002").put("brand", "Stamos Soldering").put("model", "S-LS-28").put("desc", "Alimentation double").put("isAvailable", true).put("returnDate", null));
        addTool(new JsonObject().put("idISEP", "003").put("brand", "Steinberg").put("model", "SBS-ER-3000").put("desc", "Agitateur électrique").put("isAvailable", true).put("returnDate", null));
    }

    private void addTool(JsonObject tool){
        tools.put(tool.getString("idISEP"), tool);
    }
}
