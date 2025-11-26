package kr.co.scrab.common;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.servlet.HandlerInterceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CommonInterceptor implements HandlerInterceptor {
	private static final Logger logger = LoggerFactory.getLogger(CommonInterceptor.class);
	
	@Override
	public boolean preHandle(HttpServletRequest request,
			HttpServletResponse response, Object handler) throws Exception {
		
		//String j = Util.getCookie(request, "JSESSIONID");
		String RequestURI = request.getRequestURI();

		if(RequestURI.startsWith("/admin")) {
			System.out.println(RequestURI);
			
			if(request.getRequestURI().contentEquals("/admin/login")){
				return true;
			}
			
			if(SessionUser.getSession("adminId") == null) {				
				response.sendRedirect("/admin/login");
				return false;
			}
		}
		
		return true;
	}
	
	//모든 주소 가져오기
	public String getFullUrl (HttpServletRequest request) {
		
		String url = "";
		
		Set<String> keySet = request.getParameterMap().keySet();
		for(String key: keySet) {
			
			//logger.debug("preHandle getFullUrl =======" + key + "=" + request.getParameter(key));
			
			if(key.equals("authCode") || key.equals("userid") || key.equals("userpwd") ) {
				continue;
			}
			
			String separator = "";
			if(url.equals("")) {
				separator = "?";
			}else {
				separator = "&";
			}

			url += separator + "" + key + "=" + request.getParameter(key);
		}
		
		return url;
		
	}
	
}
