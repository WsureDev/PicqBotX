package cc.moecraft.icq.receiver;

import cc.moecraft.icq.PicqBotX;
import cc.moecraft.icq.accounts.BotAccount;
import cc.moecraft.icq.receiver.callback.Sender;
import cc.moecraft.icq.receiver.callback.WSCallback;
import cc.moecraft.logger.HyLogger;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;

import java.net.InetSocketAddress;
import java.util.concurrent.ConcurrentHashMap;

public class PicqWebSocketServer extends WebSocketServer  implements Sender {
    /**
     * 日志对象
     */
    private HyLogger logger;

    private PicqBotX bot;

    private ConcurrentHashMap<WebSocket,Long> websockets = new ConcurrentHashMap<>();

    private ConcurrentHashMap<String,WSCallback> callbackMap = new ConcurrentHashMap<>();

    public PicqWebSocketServer(InetSocketAddress address, PicqBotX bot){
        super(address);
        this.bot = bot;
        this.logger = bot.getLogger();
    }

    public PicqWebSocketServer() {
    }

    @Override
    public void onOpen(WebSocket webSocket, ClientHandshake clientHandshake) {
        Long userId = Long.parseLong(clientHandshake.getFieldValue("X-Self-ID"));
        if(bot.getConfig().isDebug())
        {
            logger.log("Thread:{},path:{}",Thread.currentThread().getName(),clientHandshake.getResourceDescriptor());
            logger.log("X-Self-ID:"+clientHandshake.getFieldValue("X-Self-ID"));
            logger.log("X-Client-Role:"+clientHandshake.getFieldValue("X-Client-Role"));
        }
        if(userId!=null)
        {
            logger.log("add {} to accountManager",userId);

            new Thread(new Runnable() {
                @Override
                public void run() {
                    websockets.put(webSocket,userId);
                    BotAccount account = new BotAccount(userId,null,bot,null,null,webSocket);
                    bot.addAccount(account);
                }
            }).start();
        }
    }

    @Override
    public void onClose(WebSocket webSocket, int i, String s, boolean b) {
        if(bot.getConfig().isDebug()){
            logger.warning("{} - {} is closed.",this.getClass().getName(),websockets.get(webSocket));
        }
    }

    @Override
    public void onMessage(WebSocket webSocket, String s) {
        if(bot.getConfig().isDebug()) {
            logger.log("{} - {}:onMessage:{}", this.getClass().getName(), websockets.get(webSocket), s);
        }
        putReturnMessage(s);
    }

    @Override
    public void onError(WebSocket webSocket, Exception e) {
        logger.error("{} - {} Throw a Exception.:{}",this.getClass().getName(),websockets.get(webSocket),e.getMessage());
    }

    @Override
    public void onStart() {
        logger.log("{} - is Starting...",this.getClass().getName());
    }

    @Override
    public void submitMessage(WSCallback callback, String echo, String message) {
        callbackMap.put(echo,callback);
    }

    void putReturnMessage(String response){
        JsonObject json = new JsonParser().parse(response).getAsJsonObject();
        JsonElement echoElement = json.get("echo");
        String echo = echoElement==null?null:echoElement.getAsString();
        if(echo!=null){
            callbackMap.get(echo).report(response);
        } else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    bot.getEventManager().getEventParser().call(response);
                }
            }).start();
        }
    }
}
