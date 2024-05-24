package top.hcode.hoj.utils;

import java.io.*;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.Session;

/**
 * java 使用jsch远程链接linux 执行命令
 *
 */
public class JSchUtil {

    /**
     * 连接服务器后执行相应的linux命令
     *
     * @param command
     * @throws JSchException
     */
    public static String execCmd(String hostIP, Integer port, String username, String password, String command)
            throws JSchException {
        JSch jsch = new JSch();
        Session session = jsch.getSession(username, hostIP, port);
        session.setPassword(password);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect(60 * 1000);
        StringBuilder responseBuilder = new StringBuilder();
        Channel channel = null;
        try {
            while (command != null) {
                channel = session.openChannel("exec");
                ((ChannelExec) channel).setCommand(command);
                channel.setInputStream(null);
                ((ChannelExec) channel).setErrStream(System.err);
                // 获取流
                InputStream in = channel.getInputStream();
                channel.connect();

                byte[] tmp = new byte[1024];

                while (true) {
                    while (in.available() > 0) {
                        int i = in.read(tmp, 0, 1024);
                        if (i < 0)
                            break;
                        responseBuilder.append(new String(tmp, 0, i));
                    }
                    if (channel.isClosed()) {
                        if (in.available() > 0)
                            continue;
                        if (channel.getExitStatus() != 0) {
                            return null; // Command execution failed
                        }
                        break;
                    }
                }
                channel.disconnect();
                session.disconnect();
                return responseBuilder.toString();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSchException e) {
            e.printStackTrace();
        } finally {// 最后流和连接的关闭
            try {
                channel.disconnect();
                session.disconnect();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return responseBuilder.toString();
    }

}
