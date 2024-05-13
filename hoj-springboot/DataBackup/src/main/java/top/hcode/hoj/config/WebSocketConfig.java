package top.hcode.hoj.config;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;
import org.springframework.web.util.WebAppRootListener;

@Configuration
@AutoConfigureAfter(NacosSwitchConfig.class)
public class WebSocketConfig implements ServletContextInitializer {
    /**
     * 给spring容器注入这个ServerEndpointExporter对象
     * 相当于xml：
     * <beans>
     * <bean id="serverEndpointExporter" class=
     * "org.springframework.web.socket.server.standard.ServerEndpointExporter"/>
     * </beans>
     * <p>
     * 检测所有带有@serverEndpoint注解的bean并注册他们。
     *
     * @return
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

    @Override
    public void onStartup(ServletContext servletContext) throws ServletException {
        servletContext.addListener(WebAppRootListener.class);
        servletContext.setInitParameter("org.apache.tomcat.websocket.textBufferSize", "52428800");
        servletContext.setInitParameter("org.apache.tomcat.websocket.binaryBufferSize", "52428800");
    }
}
