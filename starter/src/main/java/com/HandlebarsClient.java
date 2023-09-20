package com.example.starter;

import com.example.starter.EmailClient;

import java.util.*;
import java.lang.String;

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
        JsonObject data,
        String hbspath,
        String location,
        String setSubject,
        String userEmail
    ){
        HandlebarsTemplateEngine.create(vertx).render(data, hbspath, res -> {
            if(res.succeeded()){ 
                List emailList = Arrays.asList(userEmail,"admin@gem-labo.com");
                MailMessage message = EmailClient.create(emailList, setSubject, res.result().toString());
                EmailClient.resultsMail(message, vertx);

                context.response().putHeader("location", location).setStatusCode(302).end();
            }else{
                context.fail(res.cause());
            }
        });
    }

    public static void timerRender(
        Vertx vertx,
        JsonObject data,
        String hbspath,
        String setSubject,
        String userEmail
    ){
        HandlebarsTemplateEngine.create(vertx).render(data, hbspath, res -> {
            if(res.succeeded()){
                List emailList = Arrays.asList(userEmail,"admin@gem-labo.com");
                MailMessage message = EmailClient.create(emailList, setSubject, res.result().toString());
                EmailClient.resultsMail(message, vertx);
            }else{
                System.out.println(res.cause());
            }
        });
    }
}
