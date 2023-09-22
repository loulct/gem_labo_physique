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
import io.vertx.core.json.JsonObject;
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

    public static CompletableFuture<JsonObject> getTool(Pool pool, BigInteger id){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pool.query(String.format("SELECT * FROM public.tools WHERE id = %d LIMIT 1;", id))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    JsonObject result = row.toJson();
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static CompletableFuture<JsonArray> listTool(Pool pool, String username){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query(String.format("SELECT tools.id, tools.brand, tools.model, tools.descro, tools.idisep, tools.userid, tools.isavailable, tools.\"returndate\", tools.\"tovalidate\", tools.counter, u.email from public.tools AS tools LEFT JOIN public.users AS u on u.id = tools.userid WHERE userid is null or u.email = '%s' ORDER BY tools.id ASC", username))
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

    public static CompletableFuture<JsonObject> borrowTool(Pool pool, BigInteger userid, String date, BigInteger id){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pool.query(String.format("UPDATE public.tools SET isavailable = false, userid = %d, returndate = '%s', counter = counter + 1 WHERE id = %d;", userid, date, id))
            .execute()
            .onFailure(Throwable::printStackTrace);

        pool.query(String.format("SELECT * FROM public.tools WHERE id = %d LIMIT 1", id))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    JsonObject result = row.toJson();
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static void delTool(Pool pool, BigInteger id){
        pool.query(String.format("DELETE FROM public.history WHERE toolid = %d", id))
            .execute()
            .onFailure(Throwable::printStackTrace);
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
        pool.query(String.format("UPDATE public.tools SET isavailable = true, userid = null, returndate = null, tovalidate = false WHERE id = %d", id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static void unborrowTool(Pool pool, BigInteger id){
        pool.query(String.format("UPDATE public.tools SET tovalidate = true WHERE id = %d", id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static CompletableFuture<JsonObject> getOwner(Pool pool, BigInteger id){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pool.query(String.format("SELECT * FROM public.users AS u INNER JOIN public.tools AS tools ON u.id = tools.userid WHERE tools.id = %d LIMIT 1", id))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    JsonObject result = row.toJson();
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
        pool.query("SELECT tool.*, u.email FROM public.tools AS tool LEFT JOIN public.users AS u on u.id = tool.userid ORDER BY counter DESC;")
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

    public static CompletableFuture<JsonArray> getAdmin(Pool pool){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query("SELECT email FROM public.users WHERE role = 'admin' ORDER BY users.id ASC;")
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

    public static CompletableFuture<JsonObject> getUser(Pool pool, String email){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pool.query(String.format("SELECT id FROM public.users WHERE email = '%s' LIMIT 1;", email))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    JsonObject result = row.toJson();
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static CompletableFuture<JsonObject> getUserInfo(Pool pool, String email){
        CompletableFuture<JsonObject> future = new CompletableFuture<>();
        pool.query(String.format("SELECT * FROM public.users WHERE email = '%s' LIMIT 1;", email))
            .execute()
            .onSuccess(rows -> {
                for(Row row : rows){
                    JsonObject result = row.toJson();
                    future.complete(result);
                }
            })
            .onFailure(Throwable::printStackTrace);

        return future;
    }

    public static CompletableFuture<JsonArray> getAllBorrowed(Pool pool){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query("SELECT * FROM public.tools AS tools INNER JOIN public.users AS u ON tools.userid = u.id;")
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

    public static void addUser(Pool pool, String[] args){
        pool.query(String.format("INSERT INTO public.users(firstname, lastname, phone, \"class\", email, \"role\") VALUES('%s', '%s', '%s', '%s', '%s', '%s');", args[0], args[1], args[2], args[3], args[4], args[5]))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }

    public static CompletableFuture<JsonArray> getHistory(Pool pool){
        CompletableFuture<JsonArray> future = new CompletableFuture<>();
        JsonArray result = new JsonArray();
        pool.query("SELECT t.id, t.idisep, ROUND(AVG(h.returndate - h.borrowdate), 0) AS avgdays FROM public.history AS h LEFT JOIN public.tools AS t ON t.id = h.toolid GROUP BY t.id, t.idisep ORDER BY avgdays ASC;")
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

    public static void addHistory(Pool pool, String[] args, BigInteger id){
        pool.query(String.format("INSERT INTO public.history(returndate, borrowdate, toolid) VALUES('%s', '%s', '%d');", args[0], args[1], id))
            .execute()
            .onFailure(Throwable::printStackTrace);
    }
}