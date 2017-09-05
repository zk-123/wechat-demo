package cn.zkdcloud;

import cn.zkdcloud.core.WeChatListener;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Wechatdemo2Application {

	public static void main(String[] args) {
		SpringApplication.run(Wechatdemo2Application.class, args);
	}

	@Bean
	public ServletListenerRegistrationBean<WeChatListener> registrationServletBean(){
		ServletListenerRegistrationBean registListen = new ServletListenerRegistrationBean(new WeChatListener());
		return registListen;
	}
}
