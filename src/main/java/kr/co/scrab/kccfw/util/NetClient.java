package kr.co.scrab.kccfw.util;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import jakarta.servlet.http.HttpServletRequest;


public class NetClient {
	//public String Server = "http://localhost:8088";
		//public static String Server = "http://70.11.11.201:8088";
		public String Server = "http://mss.kccworld.co.kr";
		//public String auth = "/authLdap";
		// http://localhost:8080/RESTfulExample/json/product/get

		public NetClient() {
			
		}
		
		public NetClient(String pServer) {
			Server = pServer;
		}
		
		public String runHttpGet(String httpParam) {
			String res = "";
			try {
				URL url = new URL(Server + httpParam);
				HttpURLConnection conn = (HttpURLConnection) url.openConnection();
				conn.setRequestMethod("GET");
				conn.setConnectTimeout(1000);
				conn.setReadTimeout(1000);
				if (conn.getResponseCode() != 200) {
					throw new RuntimeException("Failed : HTTP error code : " + conn.getResponseCode());
				}

				BufferedReader br = new BufferedReader(new InputStreamReader((conn.getInputStream())));

				String output;
				while ((output = br.readLine()) != null) {
					res += output;
				}

				conn.disconnect();

			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return res;
		}

		
		public String runHttpPost(String httpUrl, Map<String,String> paramMap, Map<String, Object> header) throws Exception {
			
			String res = "";
			String postData = "";
			
			if (paramMap!=null) {
		        Set<String> keys=paramMap.keySet();

		        for (String key: keys) {	 
		        	postData = postData +
		        			"&" + URLEncoder.encode(key,"UTF-8")
		        			+ "=" +URLEncoder.encode(paramMap.get(key)==null?"":paramMap.get(key),"UTF-8");
		        }
	        }


			//System.out.println(Server + postData);
			//System.out.println(Server + httpUrl);
			
			//byte[] convertParamBytes = postData.getBytes("UTF-8");
			
			URL url = new URL(Server + httpUrl);
			System.out.println(url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			if (header != null) {
				Set<String> keys=header.keySet();
		        for (String key: keys) {	
		        	conn.setRequestProperty(key, (String)header.get(key));
		        }
			}
			
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(5000);
		
			
			//conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
			//conn.setRequestProperty("Content-Length", String.valueOf(convertParamBytes.length));
			
			DataOutputStream wr = new DataOutputStream(conn.getOutputStream()); 
			wr.writeBytes(postData); 
			wr.flush(); 
			wr.close();
			
			
//				OutputStream os = conn.getOutputStream();
//				os.write(PostData.getBytes());
//				os.flush();
			
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				res += output;
			}
			
			System.out.println("res : " + res);
			
			conn.disconnect();
		
			return res;
		}
		
		public String runHttpPostBodyJson(String httpUrl, String jsonBody, Map<String, Object> header) throws Exception {
			
			String res = "";

			//System.out.println(Server + postData);
			//System.out.println(Server + httpUrl);
			
			//byte[] convertParamBytes = postData.getBytes("UTF-8");
			
			URL url = new URL(Server + httpUrl);
			System.out.println(url);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			
			conn.setRequestMethod("POST");
			conn.setDoOutput(true);
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(5000);
		
			if (header != null) {
				Set<String> keys=header.keySet();
		        for (String key: keys) {	
		        	conn.setRequestProperty(key, (String)header.get(key));
		        }
			}
			
            // 출력 스트림을 열고 본문 데이터를 전송
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);           
            }
			
//			DataOutputStream wr = new DataOutputStream(conn.getOutputStream()); 
//			wr.writeBytes(jsonBody); 
//			wr.flush(); 
//			wr.close();
			
		
			
			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
					+ conn.getResponseCode());
			}
			
			BufferedReader br = new BufferedReader(new InputStreamReader(
					(conn.getInputStream())));

			String output;
			System.out.println("Output from Server .... \n");
			while ((output = br.readLine()) != null) {
				res += output;
			}
			
			System.out.println("res : " + res);
			
			conn.disconnect();
		
			return res;
		}

		
		public static String getRemoteIp(HttpServletRequest request){
			
			
			if(request == null) {return "";};
			
			String remoteIp = request.getHeader("X-FORWARDED-FOR");
	        
	        if (remoteIp == null){
	        	remoteIp = request.getRemoteAddr();
	        }
	        
	        return remoteIp;
		}
		
		// ssl security Exception 방지
	    public static void disableSslVerification(){
	        // TODO Auto-generated method stub
	        try
	        {
	            // Create a trust manager that does not validate certificate chains
	            TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() {
	                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
	                    return null;
	                }
	                public void checkClientTrusted(X509Certificate[] certs, String authType){
	                }
	                public void checkServerTrusted(X509Certificate[] certs, String authType){
	                }
	            }
	            };

	            // Install the all-trusting trust manager
	            SSLContext sc = SSLContext.getInstance("SSL");
	            sc.init(null, trustAllCerts, new java.security.SecureRandom());
	            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());

	            // Create all-trusting host name verifier
	            HostnameVerifier allHostsValid = new HostnameVerifier() {
	                public boolean verify(String hostname, SSLSession session){
	                    return true;
	                }
	            };

	            // Install the all-trusting host verifier
	            HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
	        } catch (NoSuchAlgorithmException e) {
	            e.printStackTrace();
	        } catch (KeyManagementException e) {
	            e.printStackTrace();
	        }
	    }
		
		
		public static void main(String[] args) throws Exception {

			
		}

}
