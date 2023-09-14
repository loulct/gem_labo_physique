package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnectOptions;
import org.testcontainers.containers.PostgreSQLContainer;

public class SqlClient extends AbstractVerticle {
    
    public static void launch(Vertx vertx) {
        //PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>();
        //postgres.start();
        PgConnectOptions options = new PgConnectOptions()
            /*
            .setPort(postgres.getMappedPort(5432))
            .setHost(postgres.getContainerIpAddress())
            .setDatabase(postgres.getDatabaseName())
            .setUser(postgres.getUsername())
            .setPassword(postgres.getPassword());
            */
            .setPort(5432)
            .setHost("localhost")
            .setDatabase("gemlabo")
            .setUser("postgres")
            .setPassword("root");

        Pool pool = Pool.pool(vertx, options, new PoolOptions().setMaxSize(5));

        pool.getConnection(ar -> {
            if (ar.succeeded()) {
                System.out.println("Connected to the PostgreSQL server successfully.");
                ar.result().close();
            } else {
                System.out.println(ar.cause().getMessage());
            }
        });

        pool.query("select * from public.users").execute().onSuccess(rows -> {
            for(Row row : rows){
                System.out.println("row = " + row.toJson());
            }
        }).onFailure(Throwable::printStackTrace);
    }
}