package kr.co.scrab.kccfw.common;

import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class Util {
	
	private static final SecureRandom secureRandom = new SecureRandom(); //
	private static final Base64.Encoder base64Encoder = Base64.getUrlEncoder(); //
	private static final Base64.Decoder base64Decoder = Base64.getUrlDecoder(); //
	
    public static List<String> findSpecificDayTimes(String startDate, String endDate, String[] specificDays, 
            String startTime, int intervalMinutes, int repeatCount) {
		List<String> result = new ArrayList<>();
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
		
		LocalDate start = LocalDate.parse(startDate, dateFormatter);
		LocalDate end = LocalDate.parse(endDate, dateFormatter);
		LocalTime time = LocalTime.parse(startTime, timeFormatter);
		
		List<DayOfWeek> daysOfWeek = new ArrayList<>();
		for (String day : specificDays) {
			daysOfWeek.add(DayOfWeek.valueOf(day.toUpperCase()));
		}
		
		LocalDate current = start;
		while (!current.isAfter(end)) {
			if (daysOfWeek.contains(current.getDayOfWeek())) {
				for (int i = 0; i < repeatCount; i++) {
					LocalDateTime dateTime = LocalDateTime.of(current, time.plusMinutes(i * intervalMinutes));
					result.add(dateTime.format(dateTimeFormatter));
				}
			}
			current = current.plusDays(1);
		}
		
		return result;
	}
	
	public static String toCamelCase(String pColNm){
		
		//if(pColNm.indexOf("_") < 0){
		//	return pColNm;
		//}
		
		int lenOfName = pColNm.length();
		char[] c = pColNm.toLowerCase().toCharArray();
		StringBuilder sb = new StringBuilder();
		
		boolean toUpperFlag = false;
		for( int j = 0 ;j < lenOfName ; j++){
			
			if( c[j] == '_' ) {
				toUpperFlag = true;
				continue;
			}
			else {
				 if( toUpperFlag ) {
					 sb.append( (c[j]+"").toUpperCase() );
					 toUpperFlag = false;
				 }
				 else sb.append(c[j]);
			}
		}
		
		//pColNm = sb.toString();	//Ã¬Â»Â¬Ã«ÂÂ¼Ã«ÂªÂ
		
		return sb.toString();
	}
	
	public static String generateToken(String id) {
	    
	    String key = id + "|" + new Date().getTime() + "|" + secureRandom.nextInt(10000);
	    System.out.println(key);
	    return base64Encoder.encodeToString(key.getBytes());

	}
	
	// 예약 코드 10자리 만들기
	public static String gen10CharHash() throws Exception {
		
    	char[] BASE62_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
		
		MessageDigest digest = MessageDigest.getInstance("SHA-256");
		
		String ts = Long.toString(System.currentTimeMillis());
		
		byte[] hashByte = digest.digest(ts.getBytes(StandardCharsets.UTF_8));
		
        StringBuilder sb = new StringBuilder();
        for (byte b : hashByte) {
            int value = b & 0xFF; // Convert byte to unsigned int
            sb.append(BASE62_CHARS[value % 62]);
        }
        String data = sb.toString();
		
		data = data.substring(0, 10);
		
		return data;
	}

	public static String generateAuthCode(String id, String remoteIp) {
	    
	    String key = id + "|" + new Date().getTime() + "|" + secureRandom.nextInt(10000) + "|" + remoteIp;
	    System.out.println(key);
	    return base64Encoder.encodeToString(key.getBytes());

	}
	
	public static String decodeAuthCode(String authCode) {
	  
	    return new String(base64Decoder.decode(authCode));

	}
	
	
	public static String getCookie(HttpServletRequest request, String key) throws UnsupportedEncodingException{
		
		Cookie[] getCookie = request.getCookies();

		return getCookie(getCookie,key);
	}
	
	public static String getCookie(Cookie[] getCookie , String key) throws UnsupportedEncodingException{
		
		if(getCookie != null){

			for(int i=0; i<getCookie.length; i++){
				Cookie c = getCookie[i];
				String name = c.getName(); //
				String value = c.getValue(); // 

				if(name.equals(key)){
					return URLDecoder.decode(value,"UTF-8");
				}
			}
		}
		
		return "";
	}
	
	
	public static void setCookie(HttpServletResponse response, String name, String value, int day) throws UnsupportedEncodingException{
		
		if(value == null){
			value = "";
		}
		
		Cookie setCookie = new Cookie(name, URLEncoder.encode(value,"UTF-8")); // 
		
		if(day == -1){
			setCookie.setMaxAge(-1); //브라우저 종료시
		}else{
			setCookie.setMaxAge(60*60*24*day); 
		}

		response.addCookie(setCookie);
	}
	
	public static void delCookie(HttpServletResponse response, String name){
		
		Cookie setCookie = new Cookie(name, null); 
		setCookie.setMaxAge(0); // 
		response.addCookie(setCookie);
	}
	
	public static String getRemoteIp(HttpServletRequest request){
		
		String remoteIp = request.getHeader("X-FORWARDED-FOR");
        
        if (remoteIp == null || remoteIp.equals("")){
        	remoteIp = request.getRemoteAddr();
        }
        
        return remoteIp;
	}
	
	public static String getHttps(HttpServletRequest request) {
	    boolean https = false;
	    String proto = (String) request.getHeader("X-FORWARDED-PROTO");
	    if (proto != null) { 
	        https = "https".equals(proto) ? true : false;
	    } else {
	        https = request.isSecure();
	    }
	    if( https ) {
	    	return "https";
	    }
	    else {
	    	return "http";
	    }
	}
	
	public static String getServerIp(){
		
		InetAddress inet = null;
		String rtn = "";
		try {
			inet = InetAddress.getLocalHost();
			rtn = inet.getHostAddress();
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
        return rtn;
	}
	
	// 4자리 난수 생성기
	public static String authNumGenerator() {
		
		Random rand = new Random();
		String numStr = "";
		
		for(int i = 0; i < 4; i++) {
			numStr += Integer.toString(rand.nextInt(10));
		}
		
		return numStr;
	}
	
	/*
	 * 세션값을 세팅한다.
	 */
	public static void setSession(HttpSession session, Object userList ) {
		
		
		if(List.class.isInstance(userList)){
			
			for (Map<String, Object> map : (List<Map<String, Object>>) userList) {
				
				for (Map.Entry<String, Object> entry : map.entrySet()) {
					
            		String nkey = Util.toCamelCase(entry.getKey());
            		Object nValue = entry.getValue();
            		
            		session.setAttribute(nkey, nValue);
            	}
			}
			
		}else if(Map.class.isInstance(userList)){
			
			for (Map.Entry<String, Object> entry : ((Map<String, Object>) userList).entrySet()) {
				
        		String nkey = Util.toCamelCase(entry.getKey());
        		Object nValue = entry.getValue();
        		
        		session.setAttribute(nkey, nValue);
        	}
		}
		
	}
	
	
	/*
	 * 세션값을 세팅한다.
	 */
	public static String getSession(HttpSession session, String sessionName ) {
		
		
		String val = (String) session.getAttribute(sessionName);
		
		
		return val;
	}
	
	
	/*
	 * JSON 리스트 문자열을 List<Map> 으로 변환
	 */
	public static List<Map<String, Object>> jsonStringToList(String jsonStr){
		
		ObjectMapper mapper = new ObjectMapper();

		List<Map<String, Object>> list= null;
		
		try {
			list = mapper.readValue(jsonStr,
			        new TypeReference<List<Map<String, Object>>>() {
			        });
		} catch (JsonMappingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (JsonProcessingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return list;
	}
	
	
	public static boolean isNumberic(String s) { //숫자 판별 함수
		try {
	     	Double.parseDouble(s);
	    	return true;
	    } catch(NumberFormatException e) {  //문자열이 나타내는 숫자와 일치하지 않는 타입의 숫자로 변환 시 발생
	    	return false;
	    }
	}

	public static boolean isInteger(String s) { //정수 판별 함수
		try {
	     	Integer.parseInt(s);
	    	return true;
	    } catch(NumberFormatException e) {  //문자열이 나타내는 숫자와 일치하지 않는 타입의 숫자로 변환 시 발생
	    	return false;
	    }
	}
}
