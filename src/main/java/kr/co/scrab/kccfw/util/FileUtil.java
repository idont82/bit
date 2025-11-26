package kr.co.scrab.kccfw.util;


import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class FileUtil {

	private final static Logger logger = LoggerFactory.getLogger(FileUtil.class);
	
	public static String FILE_PATH_ROOT = "";
	
	private String orgFileName = "";
	private long size = 0;
	private String destFilePath = "";
	private String destFileName = "";
	private String destFolder = "";
	
	
	@Value("${file.path.root}")
	public  void setRoot(String root) {
		FILE_PATH_ROOT = root;
	}
	
	public String getOrgFileName() {
		return orgFileName;
	}

	public void setOrgFileName(String orgFileName) {
		this.orgFileName = orgFileName;
	}

	public long getSize() {
		return size;
	}

	public void setSize(long size) {
		this.size = size;
	}

	public String getDestFilePath() {
		return destFilePath;
	}

	public void setDestFilePath(String destFilePath) {
		this.destFilePath = destFilePath;
	}

	public String getDestFileName() {
		return destFileName;
	}

	public void setDestFileName(String destFileName) {
		this.destFileName = destFileName;
	}

	public String getDestFolder() {
		return destFolder;
	}

	public void setDestFolder(String destFolder) {
		this.destFolder = destFolder;
	}
	
	public static void main(String[] arg) {
		
		
	};

	/**
	 * 파일 서버에 생성
	 * @param mFileList
	 * @param path
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static List<FileUtil> writeFile(List<MultipartFile> mFileList, String path) throws IllegalStateException, IOException{
		
		//루트 경로 셋팅
		String rootPath = FILE_PATH_ROOT + "/"+ path;
		
		List<FileUtil> rtnList = new ArrayList<FileUtil>();
		
		if(mFileList==null) {return rtnList;};
		
		String timeStamp = null;
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		timeStamp = (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date));
			
		File folder = new File(rootPath);
		
		if(!folder.exists()) {
			folder.mkdirs();
		}
		
		for(int i=0; i<mFileList.size(); i++) {
			MultipartFile multi = mFileList.get(i);
			if(multi == null) {
				
			}else if(multi.getSize() == 0) {
				
			}else {
				
				FileUtil fileInfo = new FileUtil();
				
				String ofileNm = multi.getOriginalFilename();
				//String sfileNm = ofileNm;
				
				/*
				File file = new File(rootPath +"/"+ ofileNm);
				
				if(file.exists()) {
					
					int idx = ofileNm.lastIndexOf(".");
					String ext = ofileNm.substring(idx);
					
					sfileNm = ofileNm.substring(0, idx) + "_" + timeStamp + ext;
					
					file = new File(rootPath+"/"+ sfileNm);
				}
				*/
				
				int idx = ofileNm.lastIndexOf(".");
				String ext = ofileNm.substring(idx);
				
				String sfileNm = timeStamp + "_" + i + ext;
				
				File file = new File(rootPath+"/"+ sfileNm);
				
				if(file.exists()) {
					
					sfileNm = timeStamp + "_" + (i + 10)  + ext;
					
					file = new File(rootPath+"/"+ sfileNm);

				}
				
				
				multi.transferTo(file);
				
				fileInfo.setOrgFileName(ofileNm);
				fileInfo.setDestFileName(sfileNm);
				fileInfo.setDestFilePath(path +"/"+ sfileNm);
				fileInfo.setDestFolder(path);
				fileInfo.setSize(multi.getSize());
				
				rtnList.add(fileInfo);
				
				logger.debug("OrgFileName : " 	+ ofileNm 
						+ " / DestFileName : "	+ sfileNm
						+ " / DestFilePath : "	+ path +"/"+ sfileNm
						+ " / Size : "	+ multi.getSize()
						);
			}
		}
		
		return rtnList;
	}
	
	/**
	 * 파일 서버에 복사
	 * @param mFileList
	 * @param path
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static FileUtil copyFile(String originPath, String fileName, int fileSize, String path) throws IllegalStateException, IOException{
		
		//루트 경로 셋팅
		String rootPath = FILE_PATH_ROOT + "/"+ path;
		
		String timeStamp = null;
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		timeStamp = (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date));
			
		File folder = new File(rootPath);
		
		if(!folder.exists()) {
			folder.mkdirs();
		}
		
		FileUtil fileInfo = new FileUtil();
		
		int idx = fileName.lastIndexOf(".");
		String ext = fileName.substring(idx);
		
		String sfileNm = timeStamp + ext;
		
		 // 2. FileInputStream, FileOutputStream 준비
        FileInputStream input = new FileInputStream(originPath);
        FileOutputStream output = new FileOutputStream(rootPath+"/"+ sfileNm);
 
        // 3. 한번에 read하고, write할 사이즈 지정
        byte[] buf = new byte[1024];
 
        // 4. buf 사이즈만큼 input에서 데이터를 읽어서, output에 쓴다.
        int readData;
        while ((readData = input.read(buf)) > 0) {
            output.write(buf, 0, readData);
        }
 
        // 5. Stream close
        input.close();
        output.close();

		fileInfo.setOrgFileName(fileName);
		fileInfo.setDestFileName(sfileNm);
		fileInfo.setDestFilePath(path +"/"+ sfileNm);
		fileInfo.setDestFolder(path);
		fileInfo.setSize(fileSize);


		return fileInfo;
	}
	
	/**
	 * 파일 서버에 복사
	 * @param byte
	 * @param path
	 * @return
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public static FileUtil writeFile(byte[] pData, String fileName, String path) throws IllegalStateException, IOException{
		
		//루트 경로 셋팅
		String rootPath = FILE_PATH_ROOT + "/"+ path;
		
		//String timeStamp = null;
		Calendar calendar = Calendar.getInstance();
		Date date = calendar.getTime();
		//timeStamp = (new SimpleDateFormat("yyyyMMddHHmmssSSS").format(date));
			
		File folder = new File(rootPath);
		
		if(!folder.exists()) {
			folder.mkdirs();
		}
		
		FileUtil fileInfo = new FileUtil();
		
		int idx = fileName.lastIndexOf(".");
		String ext = fileName.substring(idx);
		
		//String sfileNm = timeStamp + ext;
		String sfileNm = fileName;
		int fileSize = pData.length;
		
        FileOutputStream output = new FileOutputStream(rootPath+"/"+ sfileNm);
        output.write(pData);
        output.close();
        
		fileInfo.setOrgFileName(fileName);
		fileInfo.setDestFileName(sfileNm);
		fileInfo.setDestFilePath(path +"/"+ sfileNm);
		fileInfo.setDestFolder(path);
		fileInfo.setSize(fileSize);


		return fileInfo;
	}
	
	public static String readFile(String fileName, String path) throws IllegalStateException, IOException{
		
		//루트 경로 셋팅
		String rootPath = FILE_PATH_ROOT + "/"+ path;
		
		File file = new File(rootPath +"/"+  fileName);
		FileReader fr = new FileReader(file);
		BufferedReader br = new BufferedReader(fr);

		String line = "";
		String all = "";
		while((line=br.readLine()) != null) {
			all += line;
		}
		br.close();
		return all;
	}
}

