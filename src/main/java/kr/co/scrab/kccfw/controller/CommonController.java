package kr.co.scrab.kccfw.controller;

import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Blob;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.sql.rowset.serial.SerialClob;

import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.support.rowset.ResultSetWrappingSqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import kr.co.scrab.kccfw.common.BeanUtils;
import kr.co.scrab.kccfw.common.Util;
import kr.co.scrab.kccfw.util.Result;

@Controller
public class CommonController {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	//@Autowired
	//private LoggerService loggerService;
	
	//@Autowired
	//private LoginService loginService;
	/*
	@RequestMapping(value = "/index")
	public String index() {
		logger.debug("=======index====" );
		logger.debug("======index=====" );
		
		return "index";
	}
	
	
	
	 */
	
	@RequestMapping(value = "/views/{layout}", method = RequestMethod.GET)
    public String views(@PathVariable(value = "layout") String layout) { // I prefer binding to the variable name explicitely
				
        return "/views/" + layout;
    }
	
	/*
	@RequestMapping(value="/testMapList", consumes = MediaType.APPLICATION_JSON_VALUE)
    public @ResponseBody Object selectOrgDeptAjax( 
			@RequestParam Map<String,Object> mapParam,
			HttpServletRequest request
	        ) throws Exception  {
		
		
		List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
		
		for(int i=0; i<5; i++) {
			Map<String, Object> map = new HashMap<String, Object>();
			map.put("user", i+"");
			map.put("userNm", "NAME:"+i);
			list.add(map);
		}
		
		
		return list;

	}
	*/
	
	/**
	 * 웹스퀘어 공통 컴포넌트
	 * @author kim sung eun
	 * @since 2021.01.19
	 * @param param
	 * @param request
	 * @param response
	 * @return
	 */
	@RequestMapping(value = "/jsonw.do")
	public @ResponseBody Map<String, Object> jsonwServlet(
			@RequestBody Map<String, Object> param, 
		
			HttpServletRequest request, 
			HttpServletResponse response) {
		
		
		
		
		
		HttpSession session = request.getSession();
		
		Result result = new Result();
	
		String status = "success";
		Map loginParam = null;

		Map<String, String> service = (Map<String, String>) param.get(Result.SERVICE);

		String serviceName = service.get(Result.SERVICE_NAME);
		String methodName = service.get(Result.METHOD_NAME);
		
		String target = (String)param.get("target");
		String[] dltList = null;
		if(target != null) {
			dltList = target.split(",");
		}
		
		//https://dbbymoon.tistory.com/5
		//http://wiki.sys4u.co.kr/pages/viewpage.action?pageId=10586374
		
		//loginService.loginCheck(param);
		Map<String, Object> rtnObj = null;
		
				
		
		String errMsg = null;
		
		// Service Log Start
		try {
			//loggerService.setServiceId();
			//loggerService.start(param);
			
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
		
		
		try {
			

			logger.debug("serviceName:" + serviceName);
			
		
			Object objBean = BeanUtils.getBean(serviceName);
			
			
			Class[] methodParamClass = new Class[] {Map.class};
			
			Method method = objBean.getClass().getMethod(methodName, methodParamClass);
			//Method method = approvalBean.getClass().getMethod("saveApproval", methodParamClass);
			
			Object[] args = new Object[] {param};
			
			rtnObj = (Map<String, Object>) method.invoke(objBean, args);

	
			Iterator<String> keys = rtnObj.keySet().iterator();
	        while( keys.hasNext() ){
	            String key = keys.next();
	            Object obj = rtnObj.get(key);
	            
	            
	            if( ResultSetWrappingSqlRowSet.class.isInstance(obj) ) {
	            	
	            	SqlRowSet rs = (SqlRowSet)obj;
	            	
	            	JSONArray rtnList = convertSqlRowSetToJson(rs); //데이터
	            	JSONArray rtnMetaList = convertSqlRowSetMetaToJson(rs); //헤더정보

					logger.debug("\n=== Sql header : " + rtnMetaList.toJSONString() + "\n=== header END" );
	            	
	            	logger.debug("Sql total rows:" + rtnList.size());
	            	
	            	//리턴받을 target 데이터셋이 정의 되어있지 않다면, 변경하여 전달한다.
	            	boolean isKeyNm = false;
	            	
	            	logger.debug("target:" + param);
	            	logger.debug("dltList:" + dltList);
	            	
	            	for(String item : dltList){
	            		if(item.equals(key)) {
	            			isKeyNm = true;
	            		}
	            	}
	            	
	            	if(isKeyNm) {
	            		result.setData(key, rtnList );
	            		result.setData(key+"_columnInfo", rtnMetaList );
	            	}else {
	            		result.setData(dltList[dltList.length-1] , rtnList);
	            		result.setData(dltList[dltList.length-1]+"_columnInfo", rtnMetaList );
	            	}
	            	
	            	
	            	
	            }else if(List.class.isInstance(obj)){
	            	
	            	
	            	List<Map<String, Object>> rtnList = new ArrayList<Map<String, Object>>();
	            	
	            	boolean isheader = true;
	            	String headerStr = "";
	            	
	            	for (Map<String, Object> map : (List<Map<String, Object>>) obj) {
	            		           	
	            		Map<String, Object> rtnMap = new  HashMap<String, Object>();
	            		
		            	// 리턴이 LIST MAP 인 경우 CamelCase 적용
		            	for (Map.Entry<String, Object> entry : map.entrySet()) {
	
		            		String nkey = Util.toCamelCase(entry.getKey());
		            		Object nValue = entry.getValue();
		            		
		            		rtnMap.put(nkey, nValue);	
		            		
		            		if(isheader) {
		            			headerStr = headerStr+ nkey + "\n";
		            		}
		            	}
		            	
		            	isheader = false;
		            	
		            	rtnList.add(rtnMap);
	            	}
	            	
	            	logger.debug("\n=== header : "+key+" ===\n" + headerStr + "\n=== header END" );
	            	
	            	logger.debug("total rows:" + rtnList.size());
	            	
	            	//리턴받을 target 데이터셋이 정의 되어있지 않다면, 변경하여 전달한다.
	            	boolean isKeyNm = false;
	            	
	            	logger.debug("target:" + param);
	            	logger.debug("dltList:" + dltList);
	            	if(dltList == null) {
	            		isKeyNm = true;
	            		
	            	}else {
		            	for(String item : dltList){
		            		if(item.equals(key)) {
		            			isKeyNm = true;
		            		}
		            	}
	            	}
	            	
	            	if(isKeyNm) {
	            		result.setData(key, rtnList );
	            	}else {
	            		if(dltList != null) {
	            			result.setData(dltList[dltList.length-1] , rtnList);
	            		}
	            	}
	            	
	            }else {	            	            
	            
	            	result.setData(key, rtnObj.get(key) );
	            }
	        }

			
			//logger.debug("rtnObj id:" + ((Map<String, Object>)rtnObj).get("id"));
			
	        result.setMsg(Result.STATUS_SUCESS);
	        
		}catch (InvocationTargetException ex) {
			
			ex.printStackTrace();
			
			errMsg =  ex.getTargetException().getMessage();
			
			logger.error("InvocationTargetException:" + errMsg);
			
			result.setMsg(Result.STATUS_ERROR, errMsg, ex);
			
			response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
			
		}catch (Exception ex) {
			
		
			errMsg =  ex.getMessage();
			
			logger.error("Exception:" + errMsg);
			
			result.setMsg(Result.STATUS_ERROR, errMsg, ex);
			
			response.setStatus(response.SC_INTERNAL_SERVER_ERROR);
		}
		
		
		// Service Log End
		try {
			//loggerService.end(result.getResult());
			
		}catch (Exception e) {
			// TODO: handle exception
			e.printStackTrace();
		}
				
				
        /*
		try {
			// loginParam은 param(EMP_CD/PW)의 값을 꺼내는 용도
			//loginParam = (Map) param.get("dma_loginCheck");


			// 로그인 성공
			if (status.equals("success")) {
				result.setMsg(Result.STATUS_SUCESS, "로그인 성공");
			} else if (status.equals("error")) {
				result.setMsg(Result.STATUS_ERROR, "로그인 실패(패스워드 불일치)");
			} else {
				result.setMsg(Result.STATUS_ERROR, "사용자 정보가 존재하지 않습니다.");
			}
		} catch (Exception ex) {// DB커넥션 없음
			ex.printStackTrace();
			result.setMsg(Result.STATUS_ERROR, "처리도중 시스템 오류가 발생하였습니다.", ex);
		}
		*/
		return result.getResult();
	}
	
	
	/**
	 * ajax 를 통한 멀티파트 호출
	 * @param mtfRequest
	 * @param request
	 * @param response
	 * @return
	 */
	//@RequestMapping(value = { "/ajax.do" }, method = RequestMethod.POST, produces = "json/plain;charset=UTF-8")
	@RequestMapping(value = {"/ajax.do","/jsonf.do"})
	public @ResponseBody Map<String, Object>  
			ajaxServlet(MultipartHttpServletRequest mtfRequest, 
			HttpServletRequest request, 
			HttpServletResponse response
			
			) {
		
		Map<String, Object> param = new HashMap<String, Object>();
		
		if(mtfRequest != null) {
			
			String paramList = request.getParameter(Result.PARAM_LIST_NAME);
			
			ObjectMapper mapper = new ObjectMapper();
			
			try {
				param = mapper.readValue(paramList,  
						new TypeReference<Map<String, Object>>() {
				});
				
			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	       
			
			//2) 첨부 파일 담기
			String fileNm =  mtfRequest.getParameter(Result.FILE_LIST_NAME);
			
			List<MultipartFile> fileList = mtfRequest.getFiles(fileNm);
			
			param.put(fileNm, fileList);
			
			
			
		}
		
		//param.put("", value)
		return jsonwServlet(param, request, response );
		
	}
	
	
	
	private JSONArray convertSqlRowSetToJson(final SqlRowSet rs){
		if( rs == null ) {
			return new JSONArray();
		}
		
		SqlRowSetMetaData metaData = rs.getMetaData();
		
		int columnCnt = metaData.getColumnCount();
		
		String[] columnNames = metaData.getColumnNames();
		String columnName = "";
		String columnValue = "";
		
		StringBuffer sb = new StringBuffer();
		 
		JSONArray rows = new JSONArray();
		
		
		//SimpleDateFormat sf = new SimpleDateFormat("yyyyë MMì ddì¼ a hh:mm:ss");

		
		while(rs.next()){
			
			JSONObject row = new JSONObject();
			 	
			for(int k=0; k<columnCnt; k++){
			         
				columnName = columnNames[k];
				
				if(metaData.getColumnTypeName(k+1).equals("CLOB")){
					// ì ì ëª ì°¨ì¥ (byte > line) : ì±ë¥ ì íë¡ ë³ê²½
					try{
						SerialClob c =  (SerialClob) rs.getObject(columnName);
						
						String result = "";
						try{
							Reader rd = c.getCharacterStream();
					
							int i = 0;
							int charValue = 0;
							String data = "";
							
							char[] buffer = new char[4096];

							while ((charValue = rd.read(buffer)) != -1) {
								data = new String(buffer, 0, charValue);
						        result = result + data;
						        i++;
						     
							}
					
						}catch(Exception ex){
							result = "";
						}
						
						columnValue = result;
					
						
					}catch (Exception ex){
						columnValue = "";
					}
					
	            }else if(metaData.getColumnTypeName(k+1).equals("BLOB")){
	            	try{
						Blob blobObject = (Blob)rs.getObject(columnName);
						byte[] encode = Base64.encodeBase64(blobObject.getBytes(1l, (int)blobObject.length()));
						columnValue = new String(encode);
					}catch (Exception ex){
						columnValue = "";
					}
	            	
	            }else if(metaData.getColumnTypeName(k+1).equals("numeric") 
	            		|| metaData.getColumnTypeName(k+1).equals("decimal")
	            		|| metaData.getColumnTypeName(k+1).equals("NUMBER")
	            		){
	            	
	            	try{
	            		
	            		columnValue = rs.getString(columnName);
	            		
		            	if (columnValue != null && columnValue.contains("E")) {
		            		int scale = metaData.getScale(k+1);
		                	String format = "0";
		                	if(scale >= 1) {
		                		format += ".";
		                		for(int i = 0 ; i < scale ; i++) {
		                 			format += "0";
		                    	}
		                	}
		                	double ddNum = rs.getDouble(columnName);
		                	DecimalFormat df = new DecimalFormat(format);
		                	columnValue = df.format(ddNum);
		
		            	}else {
		            		columnValue = rs.getString(columnName);
		            	}
		            	
	            	}catch(Exception ex){
	            		columnValue = "";
	            	}
	            	
	            	
				}else{
					columnValue = rs.getString(columnName);
				}

			
				row.put( Util.toCamelCase(columnName) , columnValue);

			}
			
			rows.add(row);
		}
		
		return rows;
	}

	
	private JSONArray convertSqlRowSetMetaToJson(final SqlRowSet rs){
		
		if( rs == null ) {
			return new JSONArray();
		}
		 
		SqlRowSetMetaData metaData = rs.getMetaData();
			
		int columnCnt = metaData.getColumnCount();
		
	    String[] columnNames = metaData.getColumnNames();
	    String columnName = "";
	    String columnValue = "";
	    
	    StringBuffer sb = new StringBuffer();
		    
	    JSONArray rows = new JSONArray();
	    
		for(int k=0; k<columnCnt; k++){
			 
			JSONObject row = new JSONObject();
	 
			columnName = columnNames[k];

			row.put("id", Util.toCamelCase(columnName));
			row.put("name", "");
			row.put("dataType", "text");
			rows.add(row);
		}
	    
		return rows;
	}
	
	
	
	public JSONObject convertMapToJson(Map<String, String> map) {
		
		JSONObject json = new JSONObject();

		for (Map.Entry<String, String> entry : map.entrySet()) {

			String key = entry.getKey();
			String value = entry.getValue();
			json.put(key, value);

		}
		return json;
	}
	
	public JSONArray convertMapListToJson(List<Map<String, String>> pList) {

		JSONArray jsonArray = new JSONArray();

		for (Map<String, String> map : pList) {
			jsonArray.add(convertMapToJson(map));
		}
		return jsonArray;

	}
	
	public JSONArray convertMapColumnToJson(List<Map<String, String>> pList) {

		JSONArray rows = new JSONArray();
		
		for (Map.Entry<String, String> entry : pList.get(0).entrySet()) {

			JSONObject row = new JSONObject();
			 
			String key = entry.getKey();
			String value = entry.getValue();
		
			row.put("id", key);
			row.put("name", "");
			row.put("dataType", "text");
			rows.add(row);
		
		}
		return rows;
	}

	
}
