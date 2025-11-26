package kr.co.scrab.kccfw.controller;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.cert.CertificateException;
import javax.security.cert.X509Certificate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.scrab.kccfw.util.FileUtil;

@Controller
public class FileController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());

	
	
	/**
	 * 파일 다운로드 공통 
	 * @author kim sung eun
	 * @since 2021.01.19
	 * @param param
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value = "/file/{path}")
	public @ResponseBody void fileServlet(
			HttpServletRequest request, 
			HttpServletResponse response,
			@PathVariable String path) throws Exception {
		
		File file = null;
		InputStream inputStream = null;
		String orgFileName = path;
		String destFilePath = path;
		if( request.getParameter("orgFileName") != null) {
			orgFileName = request.getParameter("orgFileName");
		}
		
		if (destFilePath.contains("../")) {
			throw new Exception("File Download Error.");
		}
		
		String rootDestFilePath = FileUtil.FILE_PATH_ROOT + "/" + destFilePath;
		
		file = new File(rootDestFilePath);
		
		//logger.debug("orgFileName:" + orgFileName);
		//logger.debug("destFilePath:" + destFilePath);
		
		//logger.debug("rootDestFilePath:" + rootDestFilePath);
		
		String fileName = new String(orgFileName.getBytes("UTF-8"), "ISO-8859-1");

		response.setHeader(HttpHeaders.CONTENT_DISPOSITION, String.format("attachment; filename=\"%s\"", fileName));
		response.setContentType(Files.probeContentType(Paths.get(file.getPath())));
		response.setHeader(HttpHeaders.ACCEPT_RANGES, "bytes");
		response.setHeader(HttpHeaders.CACHE_CONTROL, "no-store");
	
			
		Calendar calendar = Calendar.getInstance();
		calendar.setTimeInMillis(file.lastModified());
		//response.setHeader(HttpHeaders.LAST_MODIFIED, file.lastModified() );
		
        /* "Content-Disposition : attachment" will be directly download, may provide save as popup, based on your browser setting*/
        //response.setHeader("Content-Disposition", String.format("attachment; filename=\"%s\"", file.getName()));

        if(inputStream == null)
        	inputStream = new BufferedInputStream(new FileInputStream(file));
        
        response.setContentLength(inputStream.available());
        //Copy bytes from source to destination(outputstream in this example), closes both streams.
        FileCopyUtils.copy(inputStream, response.getOutputStream());

	}
	
	/**
	 * 운영시 nginx 를 통과하여 실제 파일명으로 다운로드 지정 가능
	 * @param request
	 * @param response
	 * @param path
	 * @throws Exception
	 */
	@RequestMapping(value = "/filename/{path}")
	public @ResponseBody void filenameServlet(
			HttpServletRequest request, 
			HttpServletResponse response,
			@PathVariable String path) throws Exception {
		
		fileServlet(request,response, path);
	
	}
	
	
	
	/**
	 * 파일 다운로드 ZIP 공통 
	 * @author kim sung eun
	 * @since 2021.01.19
	 * @param param
	 * @param request
	 * @param response
	 * @return
	 * @throws Exception 
	 */
	@RequestMapping(value = "/fileZip.do")
	public void CompressZIP(HttpServletRequest request, HttpServletResponse response, Object handler) {
		
		String orgFileName = (String)request.getParameter("orgFileName");
		String[] destFilePath = request.getParameterValues("destFilePath");
		
		//String rootDestFilePath =FileUtil.FILE_PATH_ROOT + "/" + destFilePath;
		
		
		String[] files = destFilePath;

		ZipOutputStream zout = null;
		String zipName = orgFileName ;		//ZIP 압축 파일명
		String tempPath = "";

		if (files.length > 0) {
	          try{
	             tempPath = FileUtil.FILE_PATH_ROOT;		//ZIP 압축 파일 저장경로
	             
	             logger.debug("FileOutputStream:" + tempPath + "/" +zipName);
	     		
	             
	             File zipFile = new File(tempPath + "/" +zipName);
	             
	             //ZIP파일 압축 START
	             zout = new ZipOutputStream(new FileOutputStream(zipFile));
	             byte[] buffer = new byte[1024];
	             FileInputStream in = null;
	             
	             for ( int k=0; k<files.length; k++){
	                in = new FileInputStream(FileUtil.FILE_PATH_ROOT + "/" +  files[k]);		//압축 대상 파일
	                zout.putNextEntry(new ZipEntry(files[k]));	//압축파일에 저장될 파일명
	                
	                int len;
	                while((len = in.read(buffer)) > 0){
	                   zout.write(buffer, 0, len);			//읽은 파일을 ZipOutputStream에 Write
	                }
	                
	                zout.closeEntry();
	                in.close();
	             }
	             
	             zout.close();
	             //ZIP파일 압축 END

	            String fileName = new String(zipName.getBytes("UTF-8"), "ISO-8859-1");
	             
	            response.setContentType("application/octer-stream");
				response.setHeader("Content-Transfer-Encoding", "binary;");
				response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + "\"");
				try {
					OutputStream os = response.getOutputStream();
					FileInputStream fis = new FileInputStream(zipFile);
				
					int count = 0;
					byte[] bytes = new byte[512];
				
					while ((count = fis.read(bytes)) != -1 ) {
						os.write(bytes, 0, count);
					}
				    
					fis.close();
					os.close();
				} catch (FileNotFoundException ex) {
					//System.out.println("FileNotFoundException");
				}
	             
	             //파일다운로드 END
	             
	            zipFile.deleteOnExit();
	            
	          }catch(IOException e){
	             //Exception
	          }finally{
	             if (zout != null){
	                zout = null;
	             }
	          }
		}
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

		@Override
		public void checkClientTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void checkServerTrusted(
				java.security.cert.X509Certificate[] arg0, String arg1)
				throws java.security.cert.CertificateException {
			// TODO Auto-generated method stub
			
		}

		@Override
		public java.security.cert.X509Certificate[] getAcceptedIssuers() {
			// TODO Auto-generated method stub
			return null;
		}
    }
}
