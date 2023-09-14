package com.example.starter;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.sqlclient.Pool;
import io.vertx.sqlclient.PoolOptions;
import io.vertx.sqlclient.Row;
import io.vertx.sqlclient.SqlConnectOptions;
import org.testcontainers.containers.PostgreSQLContainer;

import java.math.BigInteger;
import io.vertx.core.json.JsonArray;
import java.util.concurrent.CompletableFuture;

public class SqlClient extends AbstractVerticle {
    
    public static Pool launch(Vertx vertx) {
        PgConnectOptions options = new PgConnectOptions()
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

        return pool;
    }

    //TODO ADD ORDER BY id ASC IF NECESSARY

    public static CompletableFuture<JsonArray> getTool(Pool pool, BigInteger id){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query(String.format("SELECT * FROM public.tools WHERE id = %d;", id))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    result.add(row.toJson());
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static CompletableFuture<JsonArray> listTool(Pool pool, String username){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query(String.format("SELECT tools.id, tools.brand, tools.model, tools.descro, tools.idisep, tools.userid, tools.\"isAvailable\", tools.\"returnDate\", tools.\"toValidate\", tools.counter, u.email from public.tools AS tools LEFT JOIN public.users AS u on u.id = tools.userid WHERE userid is null or u.email = '%s'", username))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    result.add(row.toJson());
                }
                future.complete(result);
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static CompletableFuture<JsonArray> borrowTool(Pool pool, BigInteger userid, String date, BigInteger id){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query(String.format("UPDATE public.tools SET \"isAvailable\" = false, userid = %d, \"returnDate\" = '%s', counter = counter + 1 WHERE id = %d;", userid, date, id))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    result.add(row.toJson());
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static void delTool(Pool pool, BigInteger id){
        pool.query(String.format("DELETE FROM public.tools WHERE id = %d;", id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static void addTool(Pool pool, String[] args){
        pool.query(String.format("INSERT INTO public.tools(brand, model, descro, idisep) VALUES('%s', '%s', '%s', '%s');", args[0], args[1], args[2], args[3]))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static void validateTool(Pool pool, BigInteger id){
        pool.query(String.format("UPDATE public.tools SET \"isAvailable\" = true, userid = null, \"returnDate\" = null, \"toValidate\" = false WHERE id = %d", id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static void unborrowTool(Pool pool, BigInteger id){
        pool.query(String.format("UPDATE public.tools SET \"toValidate\" = true WHERE id = %d", id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static CompletableFuture<JsonArray> getOwner(Pool pool, BigInteger id){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query(String.format("SELECT * FROM public.users AS u INNER JOIN public.tools AS tools ON u.id = tools.userid WHERE tools.id = %d", id))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    result.add(row.toJson());
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static void editTool(Pool pool, String column, String value, BigInteger id){
        pool.query(String.format("UPDATE public.tools SET %s = '%s' WHERE id = %d", column, value, id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static CompletableFuture<JsonArray> adminView(Pool pool){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query("SELECT * FROM public.tools")
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    result.add(row.toJson());
                }
                future.complete(result);
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }
}