package kr.co.scrab.kccfw.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



/**
 * 신그룹웨어 REST API 호출
 * @date : 2023-02-21 
 * @version :
 * @author : soccerma
 */
public class RestCallUtil {
	
	private static Logger logger = LoggerFactory.getLogger(RestCallUtil.class);
	//public static String restUrl = "https://trans-ai.kccworld.co.kr";
	
	public static String RESPONSE_CODE ="responseCode";
	public static String CONTENTS = "contents";
	public static String CONTENT_LENGTH = "contentLength";
	public static String SET_COOKIE = "Set-Cookie";
	
	private int connectTimeout = 1000;// milisecond
	private int readTimeout = 5000;// milisecond
	
	public  static void main(String[] arg) throws Exception{

		
		//String url = "http://localhost:8951/rest/apprAuth";//로컬
		String url = "https://trans-ai.kccworld.co.kr" + "/trans";//운영
		
		//Map<String, Object> param = new LinkedHashMap<String, Object>();
		//param.put("input_txt", "hello");
		//param.put("input_lang", "en_XX");
		//param.put("output_lang", "ko_KR");
		
		JSONObject jsonObject = new JSONObject();
		jsonObject.put("input_txt", "hello");
		jsonObject.put("input_lang", "en_XX");
		jsonObject.put("output_lang", "ko_KR");
		
		//호출 
		RestCallUtil restCallUtil = new RestCallUtil();
		String rtn = restCallUtil.runHttpPostBodyJson(url, jsonObject, null);
		
		System.out.println(rtn);
		logger.debug(rtn);
	}
	
	/**
	 * https 로 호출
	 * @param urlPath
	 * @param fileName
	 * @param mapParam
	 * @return
	 */
	public static String callSsl(String urlPath, Map<String, Object> mapParam) {
		
		HttpURLConnection conn = null;
	
		URL url = null;
		
		String ret = new String();
		
		try {
			if(urlPath.startsWith("https")){
				ignoreSsl();
				
				//url = new URL(null, urlPath, new sun.net.www.protocol.https.Handler() ); // 호출할 url
				url = new URL(urlPath); // 호출할 url
				
				conn = (HttpsURLConnection) url.openConnection();
				
			}else{
				url = new URL(urlPath); // 호출할 url
				
				conn = (HttpURLConnection) url.openConnection();
			}
			
			
						
			StringBuilder sbParam = new StringBuilder();
			for (Map.Entry<String, Object> param : mapParam.entrySet()) {
				if (sbParam.length() != 0)
					sbParam.append('&');
				sbParam.append(URLEncoder.encode(param.getKey(), "UTF-8"));
				sbParam.append('=');
				sbParam.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
			}
			
			
			
			conn.setRequestMethod("POST");
			conn.setRequestProperty("User-Agent", "Mozilla/5.0");
			conn.setDoOutput(true); // POST 파라미터 전달을 위한 설정
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(5000);
			//conn.setRequestProperty("Content-Type", "application/json");
			
            DataOutputStream wr = new DataOutputStream(conn.getOutputStream()); 
            wr.writeBytes(sbParam.toString()); 
            wr.flush(); 
            wr.close();
			
            
            BufferedReader br = null;
            br = new BufferedReader(new InputStreamReader(conn.getInputStream()));
    
            String input = null;
            
            
            while ((input = br.readLine()) != null){
                ret += input;
            }
            
            br.close();
           

		} catch (Exception e) {
			e.printStackTrace();
		}

		return ret;
	}
	
	public String runHttpPostBodyJson(String httpUrl, JSONObject jsonObject, Map<String, Object> header) throws Exception {
		
		Map<String,Object> rtn = runHttpPostMap(httpUrl, jsonObject, header);
		
		if ( (int)rtn.get(RESPONSE_CODE) != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
				+ rtn.get(RESPONSE_CODE));
		}
		
		return (String)rtn.get(CONTENTS);
	}
	/**
	 * 파라미터를 JSON 형태로 가능
	 * @param httpUrl
	 * @param jsonBody
	 * @param header
	 * @return
	 * @throws Exception
	 */
	public Map<String,Object> runHttpPostMap(String httpUrl, JSONObject jsonObject, Map<String, Object> header) throws Exception {
		
		String res = "";
		//System.out.println(Server + postData);
		//System.out.println(Server + httpUrl);
		
		//byte[] convertParamBytes = postData.getBytes("UTF-8");
		
		if(httpUrl.startsWith("https")){
			ignoreSsl();
		}
		
		
		URL url = new URL( httpUrl);
		logger.debug(httpUrl);
		System.out.println(httpUrl);
		HttpURLConnection conn = (HttpURLConnection) url.openConnection();
		

		conn.setRequestMethod("POST");
		conn.setDoOutput(true);
		conn.setConnectTimeout(connectTimeout);
		conn.setReadTimeout(readTimeout);
		
		conn.setRequestProperty("Content-Type", "application/json");
		
		if (header != null) {
			Set<String> keys=header.keySet();
	        for (String key: keys) {	
	        	//System.out.println("key : " + key + " = " +  (String)header.get(key));
	        	conn.setRequestProperty(key, (String)header.get(key));
	        }
		}
		
        // 출력 스트림을 열고 본문 데이터를 전송
        //try (OutputStream os = conn.getOutputStream()) {
        //    byte[] input = jsonObject.toString().getBytes("utf-8");
        //    os.write(input, 0, input.length);           
        //}
		
//		DataOutputStream wr = new DataOutputStream(conn.getOutputStream()); 
//		wr.writeBytes(jsonBody); 
//		wr.flush(); 
//		wr.close();
		
	

		
		
		
		//if (conn.getResponseCode() != 200) {
		//	throw new RuntimeException("Failed : HTTP error code : "
		//		+ conn.getResponseCode());
		//}
        //System.out.println("conn.getResponseCode() : " + conn.getResponseCode());
        logger.debug("conn.getResponseCode() : " + conn.getResponseCode());
        if (conn.getResponseCode() == 200) {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream()), "UTF-8"));
			String output;
			while ((output = br.readLine()) != null) {
				res += output;
			}
		}else {
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getErrorStream()), "UTF-8"));
			String output;
			while ((output = br.readLine()) != null) {
				res += output;
			}
		}
        
        
        logger.debug("res : " + res);
		
		
		Map<String, Object> rtn = new HashMap<String, Object>();
		int responseCode = conn.getResponseCode();
		
		rtn.put(RESPONSE_CODE, responseCode);
		rtn.put(CONTENTS, res);
		rtn.put(CONTENT_LENGTH, conn.getContentLength());
		rtn.put(SET_COOKIE, conn.getHeaderField(SET_COOKIE));
		conn.disconnect();
		
		return rtn;
	}
	
	
	
	
	public int getConnectTimeout() {
		return connectTimeout;
	}

	public void setConnectTimeout(int connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public int getReadTimeout() {
		return readTimeout;
	}

	public void setReadTimeout(int readTimeout) {
		this.readTimeout = readTimeout;
	}

	public static void ignoreSsl() throws Exception{
        HostnameVerifier hv = new HostnameVerifier() {
        public boolean verify(String urlHostName, SSLSession session) {
                return true;
            }
        };
        trustAllHttpsCertificates();
        HttpsURLConnection.setDefaultHostnameVerifier(hv);
    }

    private static void trustAllHttpsCertificates() throws Exception {
        TrustManager[] trustAllCerts = new TrustManager[1];
        TrustManager tm = new miTM();
        trustAllCerts[0] = tm;
        SSLContext sc = SSLContext.getInstance("SSL");
        sc.init(null, trustAllCerts, null);
        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    }
 
    static class miTM implements TrustManager,X509TrustManager {
        public X509Certificate[] getAcceptedIssuers1() {
            return null;
        }
 
        public boolean isServerTrusted(X509Certificate[] certs) {
            return true;
        }
 
        public boolean isClientTrusted(X509Certificate[] certs) {
            return true;
        }
 
        public void checkServerTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }
 
        public void checkClientTrusted(X509Certificate[] certs, String authType)
                throws CertificateException {
            return;
        }


		public void checkClientTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
			// TODO Auto-generated method stub
			
		}


		public void checkServerTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
			// TODO Auto-generated method stub
			
		}


		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			// TODO Auto-generated method stub
			return null;
		}
    }
	
	
}