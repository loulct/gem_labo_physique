package com.example.starter;

import com.example.starter.SqlClient;
import com.example.starter.HandlebarsClient;
import com.example.starter.EmailClient;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.nio.file.*;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.math.BigInteger;

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
import io.vertx.ext.web.handler.sockjs.SockJSBridgeOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.bridge.PermittedOptions;

import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.Row;

public class Main extends AbstractVerticle{
    public static void main(String[] args) {
        Launcher.executeCommand("run", Main.class.getName());
    }

    JsonObject header = new JsonObject().put("column1", "id")
            .put("column2", "Marque")
            .put("column3", "Modèle")
            .put("column4", "Description")
            .put("column5", "Identification ISEP")
            .put("column6", "Disponible ?")
            .put("column7", "Emprunté par")
            .put("column8", "Date de retour");

    @Override
    public void start() throws Exception {

        Pool pool = SqlClient.launch(vertx);

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
            BigInteger id = new BigInteger(edit.getString("id"));
            CompletableFuture<JsonObject> future = SqlClient.getTool(pool, id);

            future.thenAccept(jsonObject -> {
                SqlClient.editTool(pool, edit.getString("field"), edit.getString("value"), id);
            }).exceptionally(ex -> {
                return null;
            });
        });

        router
            .route("/eventbus/*")
            .subRouter(sockJSHandler.bridge(options));

        PropertyFileAuthentication authn = PropertyFileAuthentication.create(vertx, "vertx-users.properties");
        PropertyFileAuthorization authorizationProvider = PropertyFileAuthorization.create(vertx, "vertx-roles.properties");

        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                System.out.println("Server function called!");
                LocalDate today = LocalDate.now();
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

                SqlClient.getAllBorrowed(pool)
                .thenAccept(result -> {
                    result.stream().forEach(e -> {
                        JsonObject entry = (JsonObject) e;
                        LocalDate date = LocalDate.parse(entry.getString("returnDate"), formatter);
                        if(!date.isAfter(today)){
                            if(!Boolean.parseBoolean(entry.getString("toValidate"))){
                                JsonObject data = new JsonObject().put("tool", entry);
                                HandlebarsClient.timerRender(
                                    vertx, 
                                    data, 
                                    "private/hbs/emails/expired.hbs", 
                                    "Délai d'emprunt expiré !", 
                                    entry.getString("email")
                                );
                            }
                        }
                    });
                });
            }
        }, 0, 24 * 60 * 60 * 1000);

        router.route("/private/*").handler(RedirectAuthHandler.create(authn, "/login.html"));

        router.route("/private/*").handler(StaticHandler.create("src/main/resources/private").setCachingEnabled(false));

        router.route("/private/admin").handler(context -> {
            if(context.user() == null){
                context.response().setStatusCode(302).putHeader("Location", "/private/tools").end();
            }
            authorizationProvider.getAuthorizations(context.user()).onSuccess(v -> {
                if (RoleBasedAuthorization.create("admin").match(context.user())){
                    System.out.println("User is admin");
                    String username = context.user().principal().getString("username");
                    SqlClient.getUserInfo(pool, username)
                    .thenCompose(user -> {
                        return SqlClient.adminView(pool)
                            .thenApply(tools -> {
                                return new JsonObject().put("user", user).put("tools", tools);
                            });
                    }).thenAccept(result -> {
                        result.put("header", header);

                        int total_counter = result.getJsonArray("tools").stream()
                        .map(e -> {
                            return ((JsonObject) e).getInteger("counter");
                        })
                        .collect(Collectors.summingInt(Integer::intValue));

                        if(total_counter != 0){
                            result.getJsonArray("tools").stream()
                            .forEach(e -> ((JsonObject) e).put("percentages", (((JsonObject) e).getInteger("counter")*100)/total_counter));
                        }

                        HandlebarsClient.simpleRender(vertx, context, result, "private/hbs/main/admin.hbs");
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

        router.route("/private/tools/:toolID").handler(context -> handleGetTool(context, pool));
        router.route("/private/admin/del/:toolID").handler(context -> handleDelTool(context, pool));
        router.route("/private/unborrow/:toolID").handler(context -> handleUnborrowTool(context, pool));
        router.route("/private/admin/validate/:toolID").handler(context -> handleValidateTool(context, pool));
        router.route("/private/add").handler(context -> handleAddTool(context, pool));
        router.route("/private/tools").handler(context -> handleListTool(context, pool));

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

                    HandlebarsClient.redirectRender(
                        vertx,
                        context,
                        data,
                        "private/hbs/emails/password.hbs",
                        "/login.html",
                        "Mot de passe mis à jour",
                        userEmail
                    );
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

                    HandlebarsClient.redirectRender(
                        vertx,
                        context,
                        data,
                        "private/hbs/emails/setup.hbs",
                        "/login.html",
                        "Votre compte a été créé",
                        userEmail
                    );
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

    private void handleGetTool(RoutingContext context, Pool pool){
        String toolID = context.request().getParam("toolID");
        String date = context.request().getParam("returnDate");
        String username = context.user().principal().getString("username");

        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            SqlClient.getUser(pool, username)
            .thenCompose(userResult -> {
                return SqlClient.borrowTool(pool, new BigInteger(userResult.getString("id")), date, new BigInteger(toolID))
                    .thenApply(tool -> {
                        return new JsonObject().put("tool", tool);
                    });
            }).thenAccept(result -> {
                HandlebarsClient.redirectRender(
                    vertx,
                    context,
                    result,
                    "private/hbs/emails/borrowed.hbs",
                    "/private/tools",
                    "Matériel emprunté !",
                    username
                );
            });
        }
    }

    private void handleUnborrowTool(RoutingContext context, Pool pool){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            SqlClient.unborrowTool(pool, new BigInteger(toolID));
            response.putHeader("location", "/private/tools").setStatusCode(302).end();
        }
    }

    private void handleValidateTool(RoutingContext context, Pool pool){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            SqlClient.getTool(pool, new BigInteger(toolID))
            .thenCompose(tool -> {
                return SqlClient.getOwner(pool, new BigInteger(toolID))
                    .thenApply(owner -> {
                        return new JsonObject().put("tool", tool).put("email", owner.getString("email"));
                    });
            }).thenAccept(result -> {
                System.out.println(result);
                SqlClient.validateTool(pool, new BigInteger(toolID));

                HandlebarsClient.redirectRender(
                    vertx,
                    context,
                    result,
                    "private/hbs/emails/validated.hbs",
                    "/private/admin",
                    "Retour du matériel validé !",
                    result.getString("email")
                );
            });
        }
    }

    private void handleAddTool(RoutingContext context, Pool pool){
        String brand = context.request().getParam("brand");
        String model = context.request().getParam("model");
        String descro = context.request().getParam("descro");
        String idisep = context.request().getParam("idisep");
        
        SqlClient.addTool(pool, new String[]{brand, model, descro, idisep});

        HttpServerResponse response = context.response();
        response.putHeader("location", "/private/tools").setStatusCode(302).end();
    }

    private void handleDelTool(RoutingContext context, Pool pool){
        String toolID = context.request().getParam("toolID");
        HttpServerResponse response = context.response();
        if(toolID == null){
            sendError(400, response);
        }else{
            SqlClient.delTool(pool, new BigInteger(toolID));
            response.putHeader("location", "/private/admin").setStatusCode(302).end();
        }
    }

    private void handleListTool(RoutingContext context, Pool pool){
        String username = context.user().principal().getString("username");
        SqlClient.getUserInfo(pool, username)
        .thenCompose(user -> {
            return SqlClient.listTool(pool, username)
                .thenApply(tool -> {
                    return new JsonObject().put("user", user).put("tools", tool);
                });
        }).thenAccept(result -> {
            result.put("header", header);
            HandlebarsClient.simpleRender(
                vertx, 
                context, 
                result, 
                "private/hbs/main/user.hbs"
            );
        });
    }

    private void sendError(int statusCode, HttpServerResponse response){
        response.setStatusCode(statusCode).end();
    }
}
