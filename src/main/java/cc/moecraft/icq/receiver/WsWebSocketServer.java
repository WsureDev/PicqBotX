package cc.moecraft.icq.receiver;

import cc.moecraft.icq.PicqBotX;
import cc.moecraft.icq.receiver.callback.WSCallbackImpl;
import cc.moecraft.icq.receiver.callback.WSReturn;
import cc.moecraft.icq.receiver.callback.WSReturnImpl;
import cc.moecraft.logger.HyLogger;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class WsWebSocketServer extends WebSocketServer {

    /**
     * 机器人对象
     */
    private final PicqBotX bot;

    /**
     * 日志对象
     */
    protected final HyLogger logger;



    private static int onlineCount = 0;

    private static ConcurrentHashMap<String, WsWebSocketServer> clients = new ConcurrentHashMap<String, WsWebSocketServer>();

    private static ConcurrentHashMap<String, String> returnMessage = new ConcurrentHashMap<String, String>();
    private WebSocket webSocket;

    public WebSocket getWebSocket() {
        return webSocket;
    }

    private String userId;
    /**
     * 构造一个webSocket服务器
     */
    public WsWebSocketServer(InetSocketAddress address,PicqBotX bot)
    {
        super(address);
        this.bot = bot;
        logger = bot.getLogger();
    }

    public String sendMessageTo(String message, String userId,String echo) throws IOException {
        if(!isConnected(userId))
        {
            return "{\n" +
                    "    \"status\": \"failed\",\n" +
                    "    \"retcode\": 1404,\n" +
                    "    \"data\": null\n" +
                    "}";
        }
        clients.get(userId).getWebSocket().send(message);
        returnMessage.put(echo,echo);
        return "{\n" +
                "    \"status\": \"ok\",\n" +
                "    \"retcode\": 0,\n" +
                "    \"data\": null\n" +
                "}";
    }

    public void sendMessageAll(String message) throws IOException {
        broadcast(message);
    }

    public static boolean isConnected(String userId){
        return clients.keySet().contains(userId);
    }

    public static synchronized int getOnlineCount() {
        return onlineCount;
    }

    public static synchronized void addOnlineCount() {
        WsWebSocketServer.onlineCount++;
    }

    public static synchronized void subOnlineCount() {
        WsWebSocketServer.onlineCount--;
    }

    public static synchronized ConcurrentHashMap<String, WsWebSocketServer> getClients() {
        return clients;
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        this.webSocket = webSocket;
        this.userId = clientHandshake.getFieldValue("X-Self-ID");
        if(isConnected(userId))
        {
            logger.warning(userId+"已经存在");
            return;
        }
        if(bot.getConfig().isDebug())
        {
            logger.log(clientHandshake.getResourceDescriptor());
            logger.log("X-Self-ID:"+clientHandshake.getFieldValue("X-Self-ID"));
            logger.log("X-Client-Role:"+clientHandshake.getFieldValue("X-Client-Role"));
        }
        clients.put(userId,this);
        bot.addAccount(Long.parseLong(userId),null,null,null);
        addOnlineCount();
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        logger.log(userId+"关闭了连接");
        clients.remove(userId);
        subOnlineCount();
    }

    @Override
    public void onMessage(WebSocket webSocket, String message) {
        if (bot.getConfig().isDebug())
            logger.debug(userId+"- 数据: {}", message);
        // 调用事件
        putReturnMessage(message);
        bot.getEventManager().getEventParser().call(message);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.warning(userId+"- 发生错误: {}", e.getMessage());
    }

    @Override
    public void onStart() {
        logger.log(this.getClass().getName()+":onStart!");
    }

    void putReturnMessage(String message){
        JsonObject json = new JsonParser().parse(message).getAsJsonObject();
        String echo = json.get("echo").getAsString();
        if(echo!=null && !echo.isEmpty()){
            returnMessage.put(echo,message);
        }
    }

    String getReturnMessage(String echo){
        String message = returnMessage.get(echo);
        returnMessage.remove(echo);
        return message;
    }
}
