package com.example.starter;

import java.util.Arrays;
import java.io.File;
import java.util.Scanner;

import java.io.FileNotFoundException;

import io.vertx.core.Launcher;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.handler.*;
import io.vertx.core.AbstractVerticle;
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

    @Override
    public void start() throws Exception {

        Router router = Router.router(vertx);
        
        router.route().handler(BodyHandler.create());
        router.route().handler(SessionHandler.create(LocalSessionStore.create(vertx)));

        PropertyFileAuthentication authn = PropertyFileAuthentication.create(vertx, "vertx-users.properties");

        router.route("/private/*").handler(RedirectAuthHandler.create(authn, "/login.html"));

        router.route("/private/*").handler(StaticHandler.create("src/main/resources/private").setCachingEnabled(false));

        router.route("/loginhandler").handler(FormLoginHandler.create(authn).setDirectLoggedInOKURL("/private/main.html"))
            .failureHandler(context -> {
                context.response()
                    .putHeader("location", "/login.html")
                    .setStatusCode(302)
                    .end();
            }
        );

        router.route("/forgotpassword").handler(context -> {

            try{
                Scanner scanner = new Scanner(new File("src/main/resources/emails/password.html"));
                String text = scanner.useDelimiter("\\A").next();
                scanner.close();

                MailMessage email = new MailMessage()
                .setFrom("gem-labo-physique@gem-labo.com")
                .setTo(Arrays.asList(
                    "test@user.com",
                    "admin@gem-labo.com"))
                .setBounceAddress("gem-labo-physique@gem-labo.com")
                .setSubject("GEM LABO PHYSIQUE : Mot de passe mis à jour")
                .setHtml(text);

                resultsMail(email);

            }catch(FileNotFoundException e) {
                System.out.println(e);
            }

            context.response().putHeader("location", "/login.html").setStatusCode(302).end();
        });

        router.route("/signuphandler").handler(context -> {

            try{
                Scanner scanner = new Scanner(new File("src/main/resources/emails/setup.html"));
                String text = scanner.useDelimiter("\\A").next();
                scanner.close();

                MailMessage email = new MailMessage()
                .setFrom("gem-labo-physique@gem-labo.com")
                .setTo(Arrays.asList(
                    "test@user.com",
                    "admin@gem-labo.com"))
                .setBounceAddress("gem-labo-physique@gem-labo.com")
                .setSubject("GEM LABO PHYSIQUE : Votre compte a été créé")
                .setHtml(text);

                resultsMail(email);

            }catch(FileNotFoundException e) {
                System.out.println(e);
            }

            String body = context.getBodyAsString();
            System.out.println(body);
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
}
