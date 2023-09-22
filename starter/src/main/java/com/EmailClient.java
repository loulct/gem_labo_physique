package com.example.starter;

import java.util.List;
import java.lang.String;

import io.vertx.core.Vertx;
import io.vertx.ext.mail.MailClient;
import io.vertx.ext.mail.MailConfig;
import io.vertx.ext.mail.MailMessage;

public class EmailClient {
    public static MailMessage create(List setTo, String setSubject, String res) {
        return new MailMessage()
                .setFrom("gem-labo-physique@gem-labo.com")
                .setTo(setTo)
                .setBounceAddress("gem-labo-physique@gem-labo.com")
                .setSubject("GEM LABO PHYSIQUE : " + setSubject)
                .setHtml(res);
    }

    public static void resultsMail(MailMessage mail, Vertx vertx) {
        MailClient mailClient = MailClient.createShared(vertx, new MailConfig().setPort(25));

        mailClient.sendMail(mail, result -> {
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
