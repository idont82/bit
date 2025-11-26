package kr.co.scrab.kccfw.common;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class Session {
	

	//@Autowired(required=true) 
	//private HttpServletRequest request;

	public static String getRemoteIP() {
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		String IP = request.getHeader("X-FORWARDED-FOR");
		if (IP == null || IP.equals("")) {
			IP = request.getRemoteAddr();
		}
		//System.out.println("----------- IP ----------- : " + IP);
		
		return IP;
	}
	
	
	public static HttpServletRequest getRequest() {
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
		return request;
	}
	
	public static HttpServletResponse getResponse() {
		HttpServletResponse request = 
				((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getResponse();
		return request;
	}
	
	/*
	 * 세션값을 세팅한다.
	 */
	public static void setSession(Object userList ) {
		
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder .getRequestAttributes()).getRequest();
		
		HttpSession session = request.getSession();
		
		if(List.class.isInstance(userList)){
			
			for (Map<String, Object> map : (List<Map<String, Object>>) userList) {
				
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					
            		String nkey = Util.toCamelCase(entry.getKey());
            		Object nValue = entry.getValue();
            		
            		session.setAttribute(nkey, nValue);
            	}
				
				break; //최초값만 셋팅
			}
			
		}else if(Map.class.isInstance(userList)){
			
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) userList).entrySet()) {
				
        		String nkey = Util.toCamelCase(entry.getKey());
        		Object nValue = entry.getValue();
        		
        		session.setAttribute(nkey, nValue);
        	}
			
		}else if(ResultSetWrappingSqlRowSet.class.isInstance(userList)){
			
			SqlRowSet rs = (SqlRowSet)userList;
		
			SqlRowSetMetaData metaData = rs.getMetaData();
			int cols = metaData.getColumnCount();
			
			while ( rs.next() )
			{
			    for ( int i = 0; i < cols; i++ )
			    {
			    	String key = metaData.getColumnNames()[i];
			    	String nkey = Util.toCamelCase(key);
	        		Object nValue = rs.getObject(key);
	        		
	        		session.setAttribute(nkey, nValue);
	        		
			    }
			}
			  
		}
		
	}
	
	/*
	 * 세션값을 세팅한다.
	 */
	public static void setSession(String sessionName, String value ) {
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder .getRequestAttributes()).getRequest();
		
		HttpSession session = request.getSession();
		
		session.setAttribute(sessionName, value);
	}
	
	public static void setSession(String sessionName, Object value ) {
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder .getRequestAttributes()).getRequest();
		
		HttpSession session = request.getSession();
		
		session.setAttribute(sessionName, value);
	}
	
	/*
	 * 세션값을 가져온다.
	 */
	public static String getSession(String sessionName ) {
		
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder .getRequestAttributes()).getRequest();
		
		HttpSession session = request.getSession();
		
		String val = (String) session.getAttribute(sessionName);
		
		
		return val;
	}
	
	public static Object getSessionObject(String sessionName ) {
		
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder .getRequestAttributes()).getRequest();
		
		HttpSession session = request.getSession();
		
		Object val = (Object) session.getAttribute(sessionName);
		
		
		return val;
	}
	
	public static String getLanguage(String[] supportLang, String defaultLang ) {
		
		HttpServletRequest request = 
				((ServletRequestAttributes) RequestContextHolder .getRequestAttributes()).getRequest();
				
		try {
			//사용자가 설정한 언어
			String cookielanguage = Util.getCookie(request.getCookies(), "language");
			
			if(cookielanguage != null && !cookielanguage.equals("")) {
				return cookielanguage;
			}
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		

		
		//기본언어
		CookieLocaleResolver cookieLocaleResolver = new CookieLocaleResolver();
		String language = cookieLocaleResolver.resolveLocale(request).getLanguage();
		
		
		if(language== null || language.equals("") || Arrays.asList(supportLang).indexOf(language) < 0) {
			language = defaultLang;
		}
		
		/*
		System.out.println("-----------------------------------------");
		System.out.println("-----------------------------------------");
		System.out.println("---------" + language);
		
		System.out.println("-----------------------------------------");
		System.out.println("-----------------------------------------");
		*/
		
		return language;
		
	}
}
