package kr.co.scrab.service;

import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.scrab.kccfw.util.FileUtil;
import kr.co.scrab.kccfw.util.RestCallUtil;

@Service
@Transactional(rollbackFor = {RuntimeException.class, Exception.class},  timeout = -1)
public class ScrabService  {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	//String coookie = "";
	
	public static boolean isStop = false;
	
	Map<String, Object> header = new HashMap<String, Object>();
	String filePath = "D:\\kccfw\\workspace_sc\\scrab\\src\\main\\resources\\static\\json";
	String fileNm = "";
	String nowYYmmdd = "";
	String id = "";
	String pwd = "";
	long sleepMs = 200;
	
	JSONArray jsonArr = null;
	
	public static void main(String[] arg) throws Exception{
		
		ScrabService scrabService = new ScrabService();
		//scrabService.start("경기", "2025", "06", 10000);
		//scrabService.start("강남", "2025", "06", 10000);
		//scrabService.start("홍대", "2025", "06", 10000);
		//scrabService.start("부산", "2025", "06", 10000);
		//scrabService.start("인천", "2025", "06", 10000);
		scrabService.start("제주도", "2025", "09", 10000, 0);
		//scrabService.land("성동샤르망");
		
	}
	
	public Map<String,Object> scrab(Map<String, Object> params) throws Exception{
			
		Map<String,Object> rtn = new HashMap<String, Object>();
		
		String area = (String)params.get("area");
		String yyyymm = (String)params.get("yyyymm");
		String yyyy = yyyymm.split("_")[0];
		String mm = yyyymm.split("_")[1];
		int startRow = (int)params.get("startRow");
		
		id = (String)params.get("id");
		pwd = (String)params.get("pwd");
		 
		if(id == null || id.equals("")) {
			throw new Exception("ID empty");
		}
		
		if(pwd == null || pwd.equals("")) {
			throw new Exception("PWD empty");
		}
		
		start(area, yyyy, mm, 10000, startRow);
		
		
		return rtn;
		
	}
	
	public Map<String,Object> scrabStop(Map<String, Object> params) {
		
		Map<String,Object> rtn = new HashMap<String, Object>();
		
		isStop = true;
		
		return rtn;
	}
		
	public void land(String keyword) throws Exception{
			
		
			Map<String, Object> mapParam = new HashMap<>();
			mapParam.put("keyword", keyword);

			
			String loginurl =  "https://new.land.naver.com/api/autocomplete";  
			
			
			//Map<String, Object> header = new HashMap<String, Object>();
			header.put("Host","new.land.naver.com");
			header.put("Origin","https://m.land.naver.com");
			header.put("Referer","https://m.land.naver.com/");
			header.put("User-Agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/137.0.0.0 Safari/537.36");
			//header.put("Cookie", cookie);
			header.put("Accept","application/json, text/javascript, */*; q=0.01");
			header.put("Connection","keep-alive");
			header.put("Accept-Language","ko,ja;q=0.9,en-US;q=0.8,en;q=0.7,id;q=0.6,en-GB;q=0.5");
			header.put("Accept-Language","ko,ja;q=0.9,en-US;q=0.8,en;q=0.7,id;q=0.6,en-GB;q=0.5");
			header.put("Sec-ch-ua",	"\"Google Chrome\";v=\"137\", \"Chromium\";v=\"137\", \"Not/A)Brand\";v=\"24\"");
			header.put("Sec-ch-ua-mobile",	"?0");
			header.put("Sec-ch-ua-platform",			"\"Windows\"");
			header.put("Sec-fetch-dest",		"empty");
			header.put("Sec-fetch-mode",		"cors");
			header.put("Sec-fetch-site",		"same-site");
			header.put("Accept-encoding","gzip, deflate, br, zstd");
			Map<String,Object> rtn = call(loginurl, mapParam, header);
			
			if ( (int)rtn.get(RestCallUtil.RESPONSE_CODE) == 200) {
				
				String rtn2 = (String)rtn.get(RestCallUtil.CONTENTS);
				
				System.out.println(""+rtn2);
			}
			
	}
		
	public void start(String keyword, String yyyy, String mm, int itemcount, int startRow) throws Exception{
		
		Date today = new Date();
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd");
		
		nowYYmmdd = df.format(today);
		fileNm = keyword + "_"+ yyyy + "_"+  mm+".json";
		
		Map<String, Object> mapParam = new HashMap<>();
		mapParam.put("username", id);
		mapParam.put("password", pwd);
		
		String loginurl =  "https://33m2.co.kr/login/submit";  
		
		header = new HashMap<String, Object>();
		
		//Map<String, Object> header = new HashMap<String, Object>();
		header.put("Host","33m2.co.kr");
		header.put("Origin","https://33m2.co.kr");
		//header.put("X-FORWARDED-FOR","8.8.8.8");
		
		header.put("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0");
		//header.put("Cookie", cookie);
		
		Map<String,Object> rtn = call(loginurl, mapParam, header);
		
		//System.out.println(rtn);
		boolean isOK = true;
		
		isStop = false;
		
		if(isOK) {
			if ( (int)rtn.get(RestCallUtil.RESPONSE_CODE) == 200) {
				
				String rtn2 = (String)rtn.get(RestCallUtil.CONTENTS);
				header.put("Cookie", (String)rtn.get(RestCallUtil.SET_COOKIE));
				//coookie = ;
				//String rtn = restCallUtil.runHttpPostBodyJson(url, jsonObject, header);
				//System.out.println("1:"+rtn2);
				//System.out.println("2. coookie:"+coookie);
				
				//mapParam.put(RestCallUtil.SET_COOKIE, coookie);
				
				isOK = true;
			}else {
				
				isOK = false;
			}
		
		}
		
		if(isOK) {
			String url = "https://33m2.co.kr/app/room/search";
			
			mapParam = new HashMap<>();
			mapParam.put("keyword", keyword);
			mapParam.put("sort", "popular");
			mapParam.put("itemcount", itemcount);
			mapParam.put("by_location", true);
			mapParam.put("map_level", 7);
			mapParam.put("now_page", 1);
			//mapParam.put("by_location", 	true);
			//mapParam.put("north_east_lng",			127.12179875341975);
			//mapParam.put("north_east_lat",			37.54252647912777);
			//mapParam.put("south_west_lng",			126.84720154330276);
			//mapParam.put("south_west_lat",			37.42932390134193);
			
			rtn = call(url, mapParam, header);
			
			if ( (int)rtn.get(RestCallUtil.RESPONSE_CODE) == 200) {
				
				String rtn2 = (String)rtn.get(RestCallUtil.CONTENTS);
				
				//System.out.println("3:"+rtn2);
				
				JSONParser jsonParser = new JSONParser();
				Object obj = jsonParser.parse(rtn2);
		        JSONObject jsonObj = (JSONObject) obj;
		        
		        JSONArray list = (JSONArray) jsonObj.get("list");
		        int size = list.size();
		        
		        for(int i = 0; i<size; i++) {
		        	
		        	if(isStop) return;
		        	
		        	JSONObject o = (JSONObject)list.get(i);
		        	//System.out.println( i + ":"+o.toJSONString());
		        	String rid = o.get("rid").toString();
		        	
		        	//System.out.println( i + ":"+o.toJSONString());
		        	
		        	SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
		        	SimpleDateFormat sdf2 = new SimpleDateFormat("yyyy-MM-dd HH:mm");
		        	Calendar cal = Calendar.getInstance();
		            
		        	
		            int MONTH_CNT = 2; //2개월 후까지
		            for(int dd=0; dd <= MONTH_CNT ; dd++) {
		            	
		            	cal.setTime(new Date());
		            	cal.add(Calendar.MONTH, dd); // 다음달
		            	
		            	String[] resultDate = sdf.format(cal.getTime()).split("-");
		            	String yyyyDays = resultDate[0];
		            	String mmDays = resultDate[1];
			        	try {
			        		
			        		Map<String,Object> rtnMap = new HashMap<String, Object>();
			        		
			        		if(dd >= startRow) { 
			        			rtnMap = getSchedule(rid, yyyyDays, mmDays);
			        		}
			        		
			        		//int cnt = (int)rtnMap.get("cnt"); //예약건수
			    	        //boolean isBooking = (boolean)rtnMap.get("is_booking"); //오늘 예약 여부
			    	        String dates = (String)rtnMap.get("dates"); //당월 예약일자 (오늘 이후데이터)
			    	        
			    	        String datesList = "";
			        		
			    	        //System.out.println(rid); 
			    	        
			    	        //이전 데이터 조회
			        		JSONObject orgJson = getJsonMap(rid);
			        		String orgDates = (String)orgJson.get(yyyyDays+""+mmDays+"_"+"days");
			        				
			        		//System.out.println(orgJson); 
			        		if(dates == null ) {
			        			dates = "";
			        		}
			        		
			        		if(orgDates == null || orgDates.equals("")|| orgDates.equals("Error")) {
			        			datesList = dates;
			        		}else {
			        			
			        			//합치기
			        			 Set<String> set = new HashSet<>();
			        			 
			        			 //오늘 이전 데이터만 담기
			        			 String[] orgDatesArr = orgDates.split(",");
			        			 for(int kk = 0; kk<orgDatesArr.length; kk++) {
			        				 if(!orgDatesArr[kk].equals("")) {
			        					 if( Integer.parseInt( nowYYmmdd.replaceAll("-", "") ) > Integer.parseInt( yyyy+mm+orgDatesArr[kk] ) ) {
			        						 set.add(orgDatesArr[kk]);
			        					 }
			        				 }
			        			 }
			        			 
			        		     set.addAll(Arrays.asList(dates.split(",")));
			        		     //set.addAll(Arrays.asList(orgDates.split(",")));
			        		     String[] mergedArray = set.toArray(new String[0]);
			        		     datesList = String.join(",", mergedArray);
			        		}
			        		
			        		int c = datesList.equals("")?0:datesList.split(",").length;
			    	        
			        		o.put(yyyyDays+""+mmDays+"_"+"days", datesList);
			        		o.put(yyyyDays+""+mmDays+"_"+"cnt", c);
			        		
			        		
			        		o.put("last_date", sdf2.format(new Date()));
			        		
			        		//o.put("booking"+"_"+"days", datesList);
			        		//o.put("booking"+"_"+"cnt", c);
			        		
			        		System.out.println(i+"/"+size+ ". " +  rid + ":"+c + "   " + o.toJSONString()); 
			        		
			        		
			        	}catch(Exception ee) {
			        		ee.printStackTrace();
			        		o.put(yyyyDays+""+mmDays+"_"+"days", "Error");
			        		o.put(yyyyDays+""+mmDays+"_"+"cnt", 0);
			        		System.out.println(i+"/"+size+ ". " + rid + ": error : "+ee.getMessage());
			        	}
		        	
		            }
		            
		            if (i % 100 == 0) {
		            	System.out.println("Write");
		            	FileUtil.writeFile(list.toJSONString().getBytes(), fileNm, filePath);
		            }
		            
		            Thread.sleep(sleepMs);
		        	
		        }
		        
		        //System.out.println(list.toJSONString());
		        
		        System.out.println("Finish");
		        FileUtil.writeFile(list.toJSONString().getBytes(), fileNm, filePath);
		        
				isOK = true;
			}else {
				
				String rtn2 = (String)rtn.get(RestCallUtil.CONTENTS);
				System.out.println(rtn2);
				isOK = false;
			}
			
		}
		
		
		
	}

	
	public Map<String, Object> getSchedule(String rid, String year, String month) throws Exception {
		
		String url = "https://33m2.co.kr/app/room/schedule";
		
		Map<String, Object> mapParam = new HashMap<>();
		mapParam.put("rid", rid);
		mapParam.put("year", year);
		mapParam.put("month", month);
		
		Map<String,Object> rtn = call(url, mapParam, header);
		
		
		Map<String, Object> rtnMap = new HashMap<>();
		
		int rtnCnt = 0;
		String rtnDates = "";
		boolean isBooking = false;
		
		if ( (int)rtn.get(RestCallUtil.RESPONSE_CODE) == 200) {
			
			String rtn2 = (String)rtn.get(RestCallUtil.CONTENTS);
			
			//System.out.println("3:"+rtn2);
			
			JSONParser jsonParser = new JSONParser();
			Object obj = jsonParser.parse(rtn2);
	        JSONObject jsonObj = (JSONObject) obj;
	        
	        JSONArray list = (JSONArray) jsonObj.get("schedule_list");
	        
	        for(int i = 0; i<list.size(); i++) {
	        	JSONObject o = (JSONObject)list.get(i);
	        	//System.out.println( i + ":"+o.toJSONString());
	        	String status = o.get("status").toString();
	        	String date = o.get("date").toString();
	        	String dd = date.split("\\-")[2];
	        	
	        	if(status.equals("booking")) {	        		
		        	rtnCnt++;
		        	rtnDates = rtnDates + (rtnDates.equals("")?dd:","+dd);
		        	if(nowYYmmdd.equals(date)) {
		        		//오늘 예약 여부
		        		isBooking = true;
		        	}
		   
		        }
	        	
	        }
	        
	        

		}
		
		rtnMap.put("cnt", rtnCnt);
		rtnMap.put("dates", rtnDates);
        rtnMap.put("is_booking", isBooking);
        
		return rtnMap;
	}
	
	public  Map<String,Object>  call(String url,  Map<String, Object> mapParam, Map<String, Object> header ) throws Exception{
		//1) 로그인
		
		String coookie = "";
		
		
		//String url = "https://hcafe.hgreenfood.com/api/com/login.do";//
		
		JSONObject jsonObject = new JSONObject();
		//jsonObject.put("keyword", "은평구");
		//jsonObject.put("clcoMvicoYn", "N");
		//jsonObject.put("sort", "popular");
		
		
		
		List<String> strList = new ArrayList<String>();


		//url = "https://33m2.co.kr/app/room/search";//
		
		StringBuilder sbParam = new StringBuilder();
		for (Map.Entry<String, Object> param : mapParam.entrySet()) {
			if (sbParam.length() != 0)
				sbParam.append('&');
			sbParam.append(URLEncoder.encode(param.getKey(), "UTF-8"));
			sbParam.append('=');
			sbParam.append(URLEncoder.encode(String.valueOf(param.getValue()), "UTF-8"));
		}
		
		url = url + "?" + sbParam.toString();
		
		//Map<String, Object> header = new HashMap<String, Object>();
		//header.put("Cookie", "MBLCTF_SESSIONID_PRD=554273de-014e-4475-8624-9dd0d78ba6e5; "); 
		//header.put("Host","33m2.co.kr");
		//header.put("Origin","https://33m2.co.kr");
		//header.put("user-agent","Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/136.0.0.0 Safari/537.36 Edg/136.0.0.0");
		//header.put("Cookie", "_gcl_au=1.1.342237384.1748990822; _ga=GA1.1.201038067.1748990822; _fbp=fb.2.1748990822665.717090135110363064; _fwb=130VicwFAUcqMq4QF1Z4tkX.1748990823688; _kmpid=km|33m2.co.kr|1748990823693|b3a60180-e81d-4a67-a9da-6d809cace0ea; _tt_enable_cookie=1; _ttp=01JWVZM3V2P4XSBH370VVVDDDR_.tt.2; airbridge_migration_metadata__33m2=%7B%22version%22%3A%221.10.70%22%7D; ab180ClientId=a0e36af6-9c64-415d-9484-5bb54c8de902; _mlcp=MLCP.2.1748990830995.41554033; _mlcs=MLCP.2.1748990830998.1; webuuid=web-e7dab142-776d-488e-928d-c18531d212ba; SESSION=NjdkMmNiZjQtNDA2OC00ZGJjLTljNDgtZjliZGIwMjZiZTg3; _gcl_gs=2.1.k1$i1749090830$u169407331; l_token=eb5cca42754246efb08206e8b3733f72:m6vM2tpOpjsPBOArtYBMfqbu946gHQdz; mqtt_client_id=xTxTtJc4HfC8; airbridge_touchpoint=%7B%22channel%22%3A%22www.google.com%22%2C%22parameter%22%3A%7B%7D%2C%22generationType%22%3A1224%2C%22url%22%3A%22https%3A//33m2.co.kr/%3Fgad_source%3D1%26gad_campaignid%3D21125713821%26gbraid%3D0AAAAABtVWOoS3nzs4h4xPKJCYBo2Exu0o%26gclid%3DCjwKCAjw3f_BBhAPEiwAaA3K5Hf47pwcG_59FhZ80-OglUcMJVxMtCr-3hvGi_i_hklJwlIlVIZ-phoCsjEQAvD_BwE%22%2C%22timestamp%22%3A1749101913653%7D; airbridge_user=%7B%22externalUserID%22%3A%22live-00199906%22%7D; _gcl_aw=GCL.1749101915.CjwKCAjw3f_BBhAPEiwAaA3K5Hf47pwcG_59FhZ80-OglUcMJVxMtCr-3hvGi_i_hklJwlIlVIZ-phoCsjEQAvD_BwE; __rtbh.lid=%7B%22eventType%22%3A%22lid%22%2C%22id%22%3A%22igcFPTGx8mBJWtOGWXqn%22%2C%22expiryDate%22%3A%222026-06-05T05%3A46%3A43.457Z%22%7D; wcs_bt=s_18b4995d0973:1749102403; airbridge_session=%7B%22id%22%3A%2208204a51-a547-4efb-b569-20f8b45a47bb%22%2C%22timeout%22%3A1800000%2C%22start%22%3A1749101127374%2C%22end%22%3A1749102406496%7D; ttcsid=1749101126759::a5KlZ41MIKmB_-fufoSX.3.1749103335753; ttcsid_CQF0V7BC77UEC0E4E5O0=1749101126759::lJ3wbKzbJAXNsUZLuzNY.3.1749103335758; _ga_9DDC4XR357=GS2.1.s1749101125$o3$g1$t1749103336$j60$l0$h0"); 
		//header.put("Cookie", cookie);
		//호출 
		RestCallUtil restCallUtil = new RestCallUtil();
		//String contentType = "application/x-www-form-urlencoded; charset=UTF-8";
		 
		Map<String,Object> rtn = restCallUtil.runHttpPostMap(url, jsonObject, header);
		
		return rtn;
				

	}
	
	
	public JSONObject getJsonMap (String rid) {
		
		if(jsonArr == null) {
			
			String read;
			try {
				read = FileUtil.readFile( fileNm,filePath);
				JSONParser jsonParser = new JSONParser();
				jsonArr = (JSONArray) jsonParser.parse(read);
				
			} catch (Exception e) {
				//e.printStackTrace();
				logger.debug(e.getMessage());
			} 
			
		}
		
		if(jsonArr != null) {

			try {

				for(int i = 0; i<jsonArr.size(); i++) {
		        	JSONObject o = (JSONObject)jsonArr.get(i);
		        	//System.out.println( i + ":"+o.toJSONString());
		        	String crid = o.get("rid").toString();
		        	
		        	if(crid.equals(rid)) {
		        		return o;
		        	}
		        }
				
			} catch (Exception e) {
				e.printStackTrace();
			} 
			
		}
	      
 
        JSONObject j = new JSONObject();
        return j;
        
	}
	
}