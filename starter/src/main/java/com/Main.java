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
import java.util.Date;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.lang.Boolean;
import java.util.Timer;
import java.util.TimerTask;

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
import io.vertx.ext.auth.properties.PropertyFileAuthorization;
import io.vertx.ext.auth.authorization.RoleBasedAuthorization;
import io.vertx.ext.auth.User;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.bridge.PermittedOptions;

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

        HandlebarsTemplateEngine engine = HandlebarsTemplateEngine.create(vertx);

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Server function called!");
                LocalDate today = LocalDate.now();

                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                tools.entrySet().stream().forEach(e -> {
                    if(e.getValue().getString("returnDate") != null){
                        if(e.getValue().getString("owner") != null){
                            LocalDate date = LocalDate.parse(e.getValue().getString("returnDate"), formatter);
                            if(!date.isAfter(today)){
                                if(!Boolean.parseBoolean(e.getValue().getString("toValidate"))){
                                    JsonObject tool = tools.get(e.getKey());
                                    
                                    JsonObject data = new JsonObject().put("tool", tool);
                                    String username = e.getValue().getString("owner");

                                    engine.render(data, "private/expired.hbs", res -> {
                                        if(res.succeeded()){
                                            MailMessage email = new MailMessage()
                                                .setFrom("gem-labo-physique@gem-labo.com")
                                                .setTo(Arrays.asList(
                                                    username,
                                                    "admin@gem-labo.com"))
                                                .setBounceAddress("gem-labo-physique@gem-labo.com")
                                                .setSubject("GEM LABO PHYSIQUE : Délai d'emprunt expiré !")
                                                .setHtml(res.result().toString());
                    
                                            resultsMail(email);    
                                        }else{
                                            System.out.println(res.cause());
                                        }
                                    });
                                }
                            }
                        }
                    }
                });
            }
        }, 0, 24 * 60 * 60 * 1000);

        Router router = Router.router(vertx);
        
        router.route().handler(BodyHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        SessionHandler sessionHandler = SessionHandler.create(LocalSessionStore.create(vertx));

        sessionHandler.setSessionTimeout(60000L);

        router.route().handler(sessionHandler);

        SockJSHandler sockJSHandler = SockJSHandler.create(vertx);

        PermittedOptions inboundPermitted = new PermittedOptions()
            .setAddress("admin.edit");

        SockJSBridgeOptions options = new SockJSBridgeOptions()
            .addInboundPermitted(inboundPermitted);

        vertx.eventBus().consumer("admin.edit", message -> {
            JsonObject edit = (JsonObject) message.body();
            JsonObject tool = tools.get(edit.getString("uid"));
            tool.put(edit.getString("field"), edit.getString("value"));
        });

        router
            .route("/eventbus/*")
            .subRouter(sockJSHandler.bridge(options));

        PropertyFileAuthentication authn = PropertyFileAuthentication.create(vertx, "vertx-users.properties");

        PropertyFileAuthorization authorizationProvider = PropertyFileAuthorization.create(vertx, "vertx-roles.properties");

        router.route("/private/*").handler(RedirectAuthHandler.create(authn, "/login.html"));

        router.route("/private/*").handler(StaticHandler.create("src/main/resources/private").setCachingEnabled(false));

        router.route("/private/admin").handler(context -> {
            if(context.user() == null){
                context.response().setStatusCode(302).putHeader("Location", "/private/tools").end();
            }
            authorizationProvider.getAuthorizations(context.user()).onSuccess(v -> {
                if (RoleBasedAuthorization.create("admin").match(context.user())){
                    System.out.println("User is admin");
                    JsonObject data = new JsonObject().put("column1", "id")
                        .put("column2", "Marque")
                        .put("column3", "Modèle")
                        .put("column4", "Description")
                        .put("column5", "Identification ISEP")
                        .put("column6", "Disponible ?")
                        .put("column7", "Emprunté par")
                        .put("column8", "Date de retour");

                    data.put("table", tools);

                    data.put("user", context.user().principal().getString("username"));

                    engine.render(data, "private/admin.hbs", res -> {
                        if(res.succeeded()){
                            context.response().end(res.result());
                        }else{
                            context.fail(res.cause());
                        }
                    });
                }else{
                    System.out.println("User isn't admin");
                    context.response().setStatusCode(302).putHeader("Location", "/private/tools").end();
                }
            }).onFailure(err -> {
                System.out.println("User isn't listed in PropertyFileAuthorization file");
                context.response().setStatusCode(302).putHeader("Location", "/private/tools").end();
            });
        });

        router.errorHandler(404, routingContext -> {
            routingContext.response().setStatusCode(302).putHeader("Location", "/private/tools").end();
        });

        router.route("/private/tools/:toolID").handler(context -> handleGetTool(context, engine));
        router.route("/private/admin/del/:toolID").handler(this::handleDelTool);
        router.route("/private/unborrow/:toolID").handler(this::handleUnborrowTool);
        router.route("/private/admin/validate/:toolID").handler(context -> handleValidateTool(context, engine));
        router.route("/private/add").handler(this::handleAddTool);
        router.route("/private/tools").handler(context -> handleListTool(context, engine));

        router.route("/loginhandler").handler(FormLoginHandler.create(authn).setDirectLoggedInOKURL("/private/admin"))
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
                    properties.put("user." + userEmail, uuid);
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
                    properties.put("user." + userEmail, uuid);
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
        String date = context.request().getParam("returnDate");

        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = tools.get(toolID);
            if(tool == null){
                sendError(404, response);
            }else{
                String username = context.user().principal().getString("username");

                Integer counter = tool.getInteger("counter");
                tool.put("isAvailable", false).put("owner", username).put("returnDate", date).put("counter", counter+1);

                System.out.println(tool.getInteger("counter"));

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

    private void handleUnborrowTool(RoutingContext context){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = tools.get(toolID);
            if(tool == null){
                sendError(404, response);
            }else{
                tool.put("toValidate", true);
                response.putHeader("location", "/private/tools").setStatusCode(302).end();
            }
        }
    }

    private void handleValidateTool(RoutingContext context, HandlebarsTemplateEngine engine){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = tools.get(toolID);
            if(tool == null){
                sendError(404, response);
            }else{
                String owner = tool.getString("owner");
                
                tool.put("isAvailable", true).put("owner", null).put("returnDate", null).put("toValidate", false);
                
                JsonObject data = new JsonObject().put("tool", tool);

                engine.render(data, "private/validated.hbs", res -> {
                    if(res.succeeded()){
                        MailMessage email = new MailMessage()
                            .setFrom("gem-labo-physique@gem-labo.com")
                            .setTo(Arrays.asList(
                                owner,
                                "admin@gem-labo.com"))
                            .setBounceAddress("gem-labo-physique@gem-labo.com")
                            .setSubject("GEM LABO PHYSIQUE : Retour du matériel validé !")
                            .setHtml(res.result().toString());

                        resultsMail(email);

                        response.putHeader("location", "/private/admin").setStatusCode(302).end();

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
        String idISEP = context.request().getParam("idISEP");
        Integer uid = Integer.parseInt(tools.lastEntry().getKey()) + 1;

        addTool(new JsonObject().put("uid", uid).put("brand", brand).put("model", model).put("desc", desc).put("idISEP", idISEP).put("isAvailable", true).put("owner", null).put("returnDate", null));

        HttpServerResponse response = context.response();
        response.putHeader("location", "/private/tools").setStatusCode(302).end();
    }

    private void handleDelTool(RoutingContext context){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            JsonObject tool = tools.get(toolID);
            if(tool == null){
                sendError(404, response);
            }else{
                tools.remove(toolID);
                response.putHeader("location", "/private/admin").setStatusCode(302).end();
            }
        }
    }

    private void handleListTool(RoutingContext context, HandlebarsTemplateEngine engine){
        JsonObject data = new JsonObject().put("column1", "id")
            .put("column2", "Marque")
            .put("column3", "Modèle")
            .put("column4", "Description")
            .put("column5", "Identification ISEP")
            .put("column6", "Disponible ?")
            .put("column7", "Emprunté par")
            .put("column8", "Date de retour");


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
        addTool(new JsonObject().put("uid", 1).put("brand", "Steinberg").put("model", "SBS-LZ-4000/20-12").put("desc", "Centrifugeuse").put("idISEP", "C1").put("isAvailable", true).put("owner", null).put("returnDate", null).put("borrowedDate", null).put("counter", 0));
        addTool(new JsonObject().put("uid", 2).put("brand", "Stamos Soldering").put("model", "S-LS-28").put("desc", "Alimentation double").put("idISEP", "Alim1").put("isAvailable", true).put("owner", null).put("returnDate", null).put("borrowedDate", null).put("counter", 0));
        addTool(new JsonObject().put("uid", 3).put("brand", "Steinberg").put("model", "SBS-ER-3000").put("desc", "Agitateur électrique").put("idISEP", "AgitElec1").put("isAvailable", true).put("owner", null).put("returnDate", null).put("borrowedDate", null).put("counter", 0));
    }

    private void addTool(JsonObject tool){
        tools.put(tool.getString("uid"), tool);
    }
}
