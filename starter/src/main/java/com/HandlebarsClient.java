package com.example.starter;

import com.example.starter.EmailClient;
import com.example.starter.SqlClient;
import io.vertx.sqlclient.Pool;
import io.vertx.core.json.JsonArray;
import java.util.concurrent.CompletableFuture;

import java.util.*;
import java.lang.String;
import java.util.stream.Collectors;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.mail.MailMessage;
import io.vertx.ext.web.templ.handlebars.HandlebarsTemplateEngine;

public class HandlebarsClient {

    public static void simpleRender(
            Vertx vertx,
            RoutingContext context,
            JsonObject data,
            String hbspath) {
            HandlebarsTemplateEngine.create(vertx).render(data, hbspath, res -> {
            if (res.succeeded()) {
                context.response().end(res.result());
            } else {
                context.fail(res.cause());
            }
        });
    }

    public static void redirectRender(
        Vertx vertx,
        RoutingContext context,
        Pool pool,
        JsonObject data,
        String hbspath,
        String location,
        String setSubject,
        String userEmail
    ){
        CompletableFuture<JsonArray> future = SqlClient.getAdmin(pool);

        future.thenAccept(jsonArray -> {
            List<String> adminList = jsonArray.stream().map(e -> {return ((JsonObject) e).getString("email");}).collect(Collectors.toList());

            HandlebarsTemplateEngine.create(vertx).render(data, hbspath, res -> {
                if(res.succeeded()){ 
                    List<String> emailList = new ArrayList<String>(Arrays.asList(userEmail));
                    emailList.addAll(adminList);
                    MailMessage message = EmailClient.create(emailList, setSubject, res.result().toString());
                    EmailClient.resultsMail(message, vertx);
    
                    context.response().putHeader("location", location).setStatusCode(302).end();
                }else{
                    context.fail(res.cause());
                }
            });
        }).exceptionally(ex -> {
            return null;
        });
    }

    public static void timerRender(
        Vertx vertx,
        Pool pool,
        JsonObject data,
        String hbspath,
        String setSubject,
        String userEmail
    ){
        CompletableFuture<JsonArray> future = SqlClient.getAdmin(pool);

        future.thenAccept(jsonArray -> {
            List<String> adminList = jsonArray.stream().map(e -> {return ((JsonObject) e).getString("email");}).collect(Collectors.toList());

            HandlebarsTemplateEngine.create(vertx).render(data, hbspath, res -> {
                if(res.succeeded()){
                    List<String> emailList = new ArrayList<String>(Arrays.asList(userEmail));
                    emailList.addAll(adminList);
                    MailMessage message = EmailClient.create(emailList, setSubject, res.result().toString());
                    EmailClient.resultsMail(message, vertx);
                }else{
                    System.out.println(res.cause());
                }
            });
        }).exceptionally(ex -> {
            return null;
        });
    }
}
