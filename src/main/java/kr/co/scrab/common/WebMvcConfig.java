package kr.co.scrab.common;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer  {
 
	@Override
    public void addInterceptors(InterceptorRegistry registry) {

        registry.addInterceptor(getInterceptor())
        .excludePathPatterns("/css/**", "/js/**", "/api/**", "/error/**", "/vendor/**", 
        		"/admin/vendor/**", "/admin/js/*");
    }
	   
    @Bean
    public CommonInterceptor getInterceptor() {
    	return new CommonInterceptor();
    }
    

}