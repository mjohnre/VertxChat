package com.mjohnre.experiment.vertxchat;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.Session;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.CookieHandler;
import io.vertx.ext.web.handler.SessionHandler;
import io.vertx.ext.web.handler.StaticHandler;
import io.vertx.ext.web.handler.sockjs.BridgeEvent;
import io.vertx.ext.web.handler.sockjs.BridgeOptions;
import io.vertx.ext.web.handler.sockjs.PermittedOptions;
import io.vertx.ext.web.handler.sockjs.SockJSHandler;
import io.vertx.ext.web.sstore.LocalSessionStore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author JohnReMugar
 */
public class Server extends AbstractVerticle {

    private static LinkedHashMap<String, String> users = new LinkedHashMap<>();

    @Override
    public void start() throws Exception {
        Router router = Router.router(vertx);

        LocalSessionStore store = LocalSessionStore.create(vertx);

        // set web root
        StaticHandler staticHandler = StaticHandler.create("*")
                .setWebRoot("web/public")
                .setIndexPage("/index.html")
                .setCachingEnabled(false);

        // session handlers
        SessionHandler sessionHandler = SessionHandler.create(store);
        sessionHandler.setNagHttps(false);

        // handles js and css libraries
        router.route("/assets/*").handler(staticHandler);

        // Cookie handler
        router.route().handler(CookieHandler.create());

        // Session Handler
        router.route().handler(sessionHandler);

        // Body handler
        router.route().handler(BodyHandler.create().setBodyLimit(64 * 1024 * 1024));

        BridgeOptions options = new BridgeOptions();

        options.addInboundPermitted(new PermittedOptions().setAddress("client-to-server"));
        options.addInboundPermitted(new PermittedOptions().setAddress("client-to-server-list-users"));
        options.addOutboundPermitted(new PermittedOptions().setAddressRegex("server-to-client.*"));
        options.addOutboundPermitted(new PermittedOptions().setAddressRegex("server-to-client-list-users.*"));

        // create sockjs handler
//	SockJSHandler sockJSHandler = SockJSHandler.create(vertx).bridge(options);
        SockJSHandler sockJSHandler = SockJSHandler.create(vertx).bridge(options, s -> {
            String sessionId = s.socket().webSession().id();
            System.out.println("sockjs sessionId " + sessionId);
            if (s.type() == BridgeEvent.Type.SEND) {
                System.out.println("sockjs send");
                s.rawMessage().getJsonObject("headers").put("session_id", sessionId);
            }

            if (s.type() == BridgeEvent.Type.RECEIVE) {
                String address = s.rawMessage().getString("address");
                System.out.println("sockjs receive " + address);
                if (address.startsWith("server-to-client")) {
                    String requestSession = s.rawMessage().getJsonObject("body").getString("session_id");
                    if (requestSession != null) {
                        s.complete(!sessionId.equals(requestSession));
                        return;
                    }
                }
            }

            s.complete(Boolean.TRUE);
        });

        // url for event bus
        router.route("/eventbus/*").handler(sockJSHandler);

        // event bus
        EventBus eb = vertx.eventBus();

        // handles client-to-server event-bus
        eb.localConsumer("client-to-server").handler(sock -> {
            if (sock.body() instanceof JsonObject) {
                JsonObject body = (JsonObject) sock.body();
                System.out.println("client-to-server " + body.encode());

                JsonArray dest = body.getJsonArray("dest");
                String message = body.getString("message");
                body.put("session_id", sock.headers().get("session_id"));

                JsonObject resp = new JsonObject();
                resp.put("type", "message");
                resp.put("success", true);
                resp.put("origin", body.getString("origin"));
                resp.put("timestamp", body.getString("timestamp"));
                resp.put("message", body.getString("message"));

                for (int i = 0; i < dest.size(); i++) {
                    System.out.println("sending to " + dest.getString(i) + " " + resp);
                    eb.publish("server-to-client." + dest.getString(i), resp);
                }
            }
        });

        // handles client-to-server event-bus
        eb.localConsumer("client-to-server-list-users").handler(sock -> {
            System.out.println("client-to-server-list-users");
            JsonArray userArray = new JsonArray();

            for (Map.Entry<String, String> entry : users.entrySet()) {
                String key = entry.getKey();
                userArray.add(entry.getValue());
            }
            JsonObject data = new JsonObject();
            data.put("type", "update_users");
            data.put("success", true);
            data.put("message", userArray);

            for (Map.Entry<String, String> entry : users.entrySet()) {
                String key = entry.getKey();

                System.out.println("sending to " + entry.getValue() + " " + data);
                eb.publish("server-to-client." + entry.getValue(), data);
            }
        });

        // route signin check if username exist,
//	if username already exists prompt user to try another username
//	else redirect to chatbox
        router.route("/signin").handler(routingContext -> {
            System.out.println("signin ses " + routingContext.session().id());
            JsonObject body = routingContext.getBodyAsJson();

            System.out.println("signin body " + body.encode());
            String username = body.getString("username");

            JsonObject data = new JsonObject();

            if (username.length() > 0) {
                data.put("success", false);
                data.put("message", "Username already exists. Please try another username.");
                if (!users.containsValue(username)) {
                    Session session = routingContext.session();

                    users.put(session.id(), username);

                    data.put("success", true);
                    data.put("message", "Successfully registered.");
                    System.out.println("SIGNED IN: " + username);

//		    routingContext.response().putHeader("location", "/").setStatusCode(302).end();
                    routingContext.response().end(data.encode());
                } else {
                    routingContext.response().end(data.encode());
                }

            } else {
                data.put("success", false);
                data.put("message", "Username is empty. Please try again.");

                routingContext.response().end(data.encode());
            }

            System.out.println("usernames: " + users.toString());
            System.out.println("ses " + routingContext.session().data().toString());
        });

        router.route("/active-users").handler(routingContext -> {
            JsonObject data = new JsonObject();
            data.put("success", false);
            data.put("message", "failed to get active-users");

            Session session = routingContext.session();
            if (users.containsKey(session.id())) {
                JsonArray userArray = new JsonArray();

                for (Map.Entry<String, String> entry : users.entrySet()) {
                    String key = entry.getKey();
                    userArray.add(entry.getValue());
                }

                data.put("success", true);
                data.put("message", userArray);
                routingContext.response().end(data.encode());

                broadcastUpdatedUsers(eb);
            } else {
                routingContext.response().end(data.encode());
            }

            System.out.println("active-users " + data.encode());
        });

        router.post("/logout").handler(routingContext -> {
            System.out.println("logout ses " + routingContext.session().id());

            JsonObject data = new JsonObject();
            Session session = routingContext.session();

            if (users.containsKey(session.id())) {
                String username = users.get(session.id());
                System.out.println("username to logout " + username);
                users.remove(session.id());

                session.remove("username");

                data.put("success", true);
                data.put("message", "Logged out successfully.");
                System.out.println("LOGGED OUT: " + username);

                routingContext.response().end(data.encode());

                System.out.println("usernames: " + users.toString());

                System.out.println("ses " + routingContext.session().data().toString());

                broadcastUpdatedUsers(eb);
            } else {
                data.put("success", false);
                data.put("message", "User is not signed in");
            }
        });

        // route chatbox.html
        router.routeWithRegex("/chatbox.*").handler(routingContext -> {
            System.out.println("chatbox ses " + routingContext.session().id());
            boolean failed = true;
            if (routingContext.request().params().contains("username")) {
                String username = routingContext.request().getParam("username");
                if (username.length() > 0) {
                    failed = false;
                    if (users.containsValue(username)) {
                        routingContext.response().sendFile("web/public/chatbox.html");
                    }
                }
            }
            if (failed) {
                routingContext.response().sendFile("web/public/index.html");
            }
            System.out.println("ses " + routingContext.session().data().toString());
        });

        // routing all else to index.html
        router.route("/*").handler(routingContext -> {
            System.out.println("/* ses " + routingContext.session().id());
//	    if (routingContext.request().uri().equals("/")) {
            routingContext.response().sendFile("web/public/index.html");
//	    }
        });

        // creating port and host
        vertx.createHttpServer().requestHandler(router::accept).listen(9091, "192.168.1.114");

        System.out.println("Server is started.");
    }

    public void broadcastUpdatedUsers(EventBus eb) {
        JsonArray userArray = new JsonArray();

        for (Map.Entry<String, String> entry : users.entrySet()) {
            String key = entry.getKey();
            userArray.add(entry.getValue());
        }
        JsonObject data = new JsonObject();
        data.put("type", "update_users");
        data.put("success", true);
        data.put("message", userArray);

        for (Map.Entry<String, String> entry : users.entrySet()) {
            String key = entry.getKey();

            System.out.println("sending to " + entry.getValue() + " " + data);
            eb.publish("server-to-client." + entry.getValue(), data);
        }
    }

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(new Server());
    }

}
