package top.hcode.hoj.controller.admin;

import top.hcode.hoj.utils.SshModel;

import com.jcraft.jsch.JSchException;

import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import javax.annotation.PostConstruct;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.ChannelShell;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.ssh.ChannelType;
import cn.hutool.extra.ssh.JschUtil;
import cn.hutool.core.thread.ThreadUtil;
import org.springframework.stereotype.Component;
import org.slf4j.LoggerFactory;
import org.apache.shiro.authz.annotation.Logical;
import org.apache.shiro.authz.annotation.RequiresRoles;
import org.slf4j.Logger;

import java.util.Arrays;
import java.io.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;

@ServerEndpoint("/ws/ssh")
@Component
@RequiresRoles(value = { "root" }, logical = Logical.OR)
public class WebSocketController {

    private String sshUsername;

    private String sshPassword;

    private String sshHost;

    private Integer serverPort;

    private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    @PostConstruct
    public void init() {
        log.info("Websocket Init");
    }

    private static Logger log = LoggerFactory.getLogger(WebSocketController.class);
    private static final AtomicInteger OnlineCount = new AtomicInteger(0);
    // concurrent包的线程安全Set,用来存放每个客户端对应的Session对象。
    private static CopyOnWriteArraySet<javax.websocket.Session> SessionSet = new CopyOnWriteArraySet<javax.websocket.Session>();

    /**
     * 连接建立成功调用的方法
     */
    @OnOpen
    public void onOpen(javax.websocket.Session session, EndpointConfig config) throws Exception {
        SessionSet.add(session);

        Map<String, List<String>> parameters = session.getRequestParameterMap();
        this.sshUsername = parameters.get("username").get(0);
        this.sshPassword = parameters.get("password").get(0);
        this.sshHost = parameters.get("host").get(0);
        this.serverPort = Integer.parseInt(parameters.get("port").get(0));

        SshModel sshItem = new SshModel();
        sshItem.setHost(sshHost);
        sshItem.setPort(serverPort);
        sshItem.setUser(sshUsername);
        sshItem.setPassword(sshPassword);

        int cnt = OnlineCount.incrementAndGet(); // 在线数加1
        log.info("Websocket Connect,Connected Cnt:{}, SessionId={}", cnt, session.getId());
        HandlerItem handlerItem = new HandlerItem(session, sshItem);
        handlerItem.startRead();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);

    }

    /**
     * 连接关闭调用的方法
     */
    @OnClose
    public void onClose(javax.websocket.Session session) {
        SessionSet.remove(session);
        int cnt = OnlineCount.decrementAndGet();
        log.info("Websocket Close,Connected Cnt:{}", cnt);
    }

    /**
     * 收到客户端消息后调用的方法
     *
     * @param message 客户端发送过来的消息
     */
    @OnMessage
    public void onMessage(String message, javax.websocket.Session session) throws Exception {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        this.sendCommand(handlerItem, message);
    }

    /**
     * 出现错误
     *
     * @param session
     * @param error
     */
    @OnError
    public void onError(javax.websocket.Session session, Throwable error) {
        log.error("Websocket Error:{},Session ID:{}", error.getMessage(), session.getId());
        SendMessage(session, "\n");
        error.printStackTrace();
    }

    private void sendCommand(HandlerItem handlerItem, String data) throws Exception {
        if (handlerItem.checkInput(data)) {
            handlerItem.outputStream.write(data.getBytes());
        } else {
            handlerItem.outputStream.write("Don't Have Such Command".getBytes());
            handlerItem.outputStream.flush();
            handlerItem.outputStream.write(new byte[] { 3 });
        }
        handlerItem.outputStream.flush();
    }

    /**
     * 发送消息,实践表明,每次浏览器刷新,session会发生变化。
     *
     * @param session
     * @param message
     */
    public static void SendMessage(javax.websocket.Session session, String message) {
        try {
            // session.getBasicRemote().sendText(String.format("%s (From Server,Session
            // ID=%s)",message,session.getId()));
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送消息出错:{}", e.getMessage());
            e.printStackTrace();
        }
    }

    private class HandlerItem implements Runnable {
        private final javax.websocket.Session session;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final Session openSession;
        private final ChannelShell channel;
        private final SshModel sshItem;
        private final StringBuilder nowLineInput = new StringBuilder();

        HandlerItem(javax.websocket.Session session, SshModel sshItem) throws IOException {
            this.session = session;
            this.sshItem = sshItem;
            this.openSession = JschUtil.openSession(sshItem.getHost(), sshItem.getPort(), sshItem.getUser(),
                    sshItem.getPassword());
            this.channel = (ChannelShell) JschUtil.createChannel(openSession, ChannelType.SHELL);
            this.inputStream = channel.getInputStream();
            this.outputStream = channel.getOutputStream();
        }

        void startRead() throws JSchException {
            this.channel.connect();
            ThreadUtil.execute(this);
        }

        /**
         * 添加到命令队列
         *
         * @param msg 输入
         * @return 当前待确认待所有命令
         */
        private String append(String msg) {
            char[] x = msg.toCharArray();
            if (x.length == 1 && x[0] == 127) {
                // 退格键
                int length = nowLineInput.length();
                if (length > 0) {
                    nowLineInput.delete(length - 1, length);
                }
            } else {
                nowLineInput.append(msg);
            }
            return nowLineInput.toString();
        }

        public boolean checkInput(String msg) {
            String allCommand = this.append(msg);
            boolean refuse;
            if (StrUtil.equalsAny(msg, StrUtil.CR, StrUtil.TAB)) {
                String join = nowLineInput.toString();
                if (StrUtil.equals(msg, StrUtil.CR)) {
                    nowLineInput.setLength(0);
                }
                refuse = SshModel.checkInputItem(sshItem, join);
            } else {
                // 复制输出
                refuse = SshModel.checkInputItem(sshItem, msg);
            }
            return refuse;
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[2048];
                int i;
                // 如果没有数据来,线程会一直阻塞在这个地方等待数据。
                while ((i = inputStream.read(buffer)) != -1) {
                    sendBinary(session, new String(Arrays.copyOfRange(buffer, 0, i), sshItem.getCharsetT()));
                }
            } catch (Exception e) {
                if (!this.openSession.isConnected()) {
                    return;
                }

                WebSocketController.this.destroy(this.session);
            }
        }
    }

    public void destroy(javax.websocket.Session session) {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        if (handlerItem != null) {
            IoUtil.close(handlerItem.inputStream);
            IoUtil.close(handlerItem.outputStream);
            JschUtil.close(handlerItem.channel);
            JschUtil.close(handlerItem.openSession);
        }
        IoUtil.close(session);
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
    }

    private static void sendBinary(javax.websocket.Session session, String msg) {
        if (!session.isOpen()) {
            // 会话关闭不能发送消息
            return;
        }
        try {
            // TODO 中文无法发送
            session.getBasicRemote().sendText(msg);
        } catch (IOException e) {
        }
    }

}
