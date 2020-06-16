import io.javalin.Javalin;
import io.javalin.websocket.WsContext;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static j2html.TagCreator.*;

public class Chat {

    private static Map<WsContext, String> userUserNameMap = new ConcurrentHashMap<>();
    private static int nextUserNumber = 1;

    public static void main(String[] args) {
        Javalin app = Javalin.create(config -> {
            config.addStaticFiles("/public");
        }).start(HerokuUtil.getHerokuAssignedPort());

        app.ws("/chat", ws -> {
            ws.onConnect(ctx -> {
                String username = "User" + nextUserNumber++;
                userUserNameMap.put(ctx, username);
                broadcastMessage("Server", (username + " joined the chat"));
            });
            ws.onClose(ctx -> {
                String username = userUserNameMap.get(ctx);
                userUserNameMap.remove(ctx);
                broadcastMessage("Server", (username + " left the chat."));
            });
            ws.onMessage(ctx -> {
                broadcastMessage(userUserNameMap.get(ctx), ctx.message());
            });
        });
    }

    public static void broadcastMessage(String sender, String message) {
        userUserNameMap.keySet().stream().filter(ctx -> ctx.session.isOpen()).forEach(session -> {
            session.send(
                    new JSONObject()
                        .put("userMessage", createHtmlMessageFromSender(sender, message))
                        .put("userList", userUserNameMap.values().toString())
            );
        });
    }

    public static String createHtmlMessageFromSender(String sender, String message) {
        return article(
                b(sender + " says:"),
                span(attrs(".timestamp"), new SimpleDateFormat("HH:mm:ss").format(new Date())),
                p(message)
        ).render();
    }
}
