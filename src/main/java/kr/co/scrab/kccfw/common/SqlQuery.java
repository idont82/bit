package kr.co.scrab.kccfw.common;

import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.script.ScriptEngine ;
import javax.script.ScriptEngineManager ;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis.utils.XMLUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.namedparam.SqlParameterSourceUtils;
import org.springframework.stereotype.Component;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;




/**
 * 荑쇰━ �떎�뻾 諛� SqlGenerator 荑쇰━�깮�꽦湲�
 * @author soccerma
 *
 */
@Component
public class SqlQuery {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
		
	public static String UPD_TYPE_INSERT = "C"; //�궫�엯
	public static String UPD_TYPE_UPDATE = "U"; //媛깆떊
	public static String UPD_TYPE_DELETE = "D"; //�궘�젣
	public static String UPD_TYPE_ALL = "A"; //�쟾泥�
	
	public static String ROW_STATUS = "rowStatus"; //�긽�깭媛�
	/*
	private static SqlQuery singleton = new SqlQuery();
	
	public static SqlQuery getInstance(){
		if(singleton==null){
			return new SqlQuery();
		}
		return singleton;
	}
	*/
	
	public static Map<String, Object> queryMap = new HashMap<String, Object>();
	public static boolean isSet = true; //荑쇰━ 珥덇린 媛��뒫 �뿬遺�
	
	//@Autowired(required = false)
	private NamedParameterJdbcTemplate prijdbcTemplate;
	
	public  static void main(String[] arg){
		//SqlQuery s = new SqlQuery();
		
		//Map<String, Object> param = new HashMap<String, Object>();
		//param.put("cycle", "AB,BB");
		
		//String query = s.get("sm.Sm.sel01", param);
		
		/*
		String path = "file:/D:/kccfw/workspace/portal/target/kcc-portal-1.0.0.jar!/BOOT-INF/classes!/mappers/mng/Mng02.xml";
		
		String fileNm = path.substring(	path.lastIndexOf("/")+1,   path.lastIndexOf(".xml") );
		String folder = path.substring(	path.lastIndexOf("mappers/")+8 , path.lastIndexOf("/"));
		
		System.out.println( fileNm);
		
		System.out.println( folder);
		*/
		
		String q = "<query id=\"selPgmTreeAll\"> \n"
		    +"SELECT   \n"
			+"<if test=\":emp != '' \"> \n"
			+"		    	AND 1=1 \n"
			+"		    </if> \n"
			+"<![CDATA[--]]> \n"
			+" </query> \n"
			;
		
		//Element el = XMLUtils.newDocument(q)//XMLUtils.StringToElement(null, "query", q);
		
		Document doc = null;
		Element el = null;
		try {
			doc = XMLUtils.newDocument();
			el = doc.createElement("qqq");
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		Text text = doc.createTextNode(q);
		el.appendChild(text);

		String sss = XMLUtils.ElementToString(el);
		System.out.println(sss);
	}
	
	public SqlQuery(NamedParameterJdbcTemplate jdbcTemplate) throws Exception{
		this.prijdbcTemplate = jdbcTemplate;
		//set();
	}
	
	public SqlQuery() throws Exception{
		
		if(isSet) {
			isSet = false;
			set();
		}

	}
	
	
	public Map<String, Object> getQueryMap(){
		return queryMap;
	}
	
	
	public void resetQueryMap() throws Exception{
		set();
	}
	
	
	private void set() throws Exception {
		ClassLoader cl = this.getClass().getClassLoader(); 
		ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver(cl);
		
		String searchPattern = ResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + "/**/mappers/**/*.xml";
		//Resource[] resources = resolver.getResources("classpath:mappers/**/*.xml") ;
		Resource[] resources = resolver.getResources(searchPattern);
		
		DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = builderFactory.newDocumentBuilder();
			
		Map<String, Object> queryMapCheck = new HashMap<String, Object>(); //理쒖큹�뿉留� �떞湲곗쐞�븳 泥댄겕
		
		for (Resource resource: resources){
			try {
				String path = resource.getURI()+"";
				
				//logger.debug("resource path:" + path);
				
				String fileNm = path.substring(	path.lastIndexOf("/")+1,   path.lastIndexOf(".xml"));
				String folder = path.substring(	path.lastIndexOf("mappers/")+8 , path.lastIndexOf("/"));
				
				String componentName = folder + "." + fileNm;
				
				InputStream is = resource.getInputStream();
				
				Document document = builder.parse(is);
				Element root = document.getDocumentElement();
				
				NodeList serviceList = root.getElementsByTagName("query");
				
								
				Element service = null;
				for(int i=0; i<serviceList.getLength(); i++){
					service = (Element)serviceList.item(i);
					String id = (componentName + "." + service.getAttribute("id")).toLowerCase();
					
					//logger.debug("queryMap.get(id):" + queryMap.get(id));
					
					if(queryMapCheck.get(id) != null) {
						
						logger.error("Error Mapper Query: [" + id + "] is Already exists.");
						
					}else {
						
						queryMap.put(id, XMLUtils.ElementToString(service));
						//queryMap.put(id, service);
						queryMapCheck.put(id, id);
					
					}
					
				}
				
				logger.debug("resource getURI2:" + resource.getURI());
			
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
		}
	}
	
	private SqlQuery(String p){
		//�옱�� �븿�닔 �궗�슜�쓣 �쐞�빐 �엫�떆濡� �깮�꽦
	}
	
	public Element stringToElement(final String inputXml)  {

        try {
        	/*
            DocumentBuilder builder = DocumentBuilderFactory
                    .newInstance().newDocumentBuilder();
                    */
        	//Date nDate = new Date();

        	DocumentBuilder builder = XMLUtils.getDocumentBuilder();
        	
        	//Date kDate = new Date();
        	
        	//logger.debug("--------------------------");
    		//logger.debug("stringToElement time2 : " + (kDate.getTime()-nDate.getTime()));
    		//logger.debug("--------------------------");
    		
            //Document document = builder
            //        .parse(new ByteArrayInputStream(inputXml
            //                .getBytes("UTF-8")));
            
            Document doc = builder.parse(new InputSource(new StringReader(inputXml)));
            
            //Date jDate = new Date();
        	
        	//logger.debug("--------------------------");
    		//logger.debug("stringToElement time3 : " + (jDate.getTime()-kDate.getTime()));
    		//logger.debug("--------------------------");
    		
            return doc.getDocumentElement();
       
        }catch (Exception ex) {
        	
        }
        
        return null;
    }
	
	/**
	 * 荑쇰━ 議고쉶
	 * @param id
	 * @return
	 */
	public String get(String id, Map<String, Object> param) {
		
		
		//Element service = queryMap.get(id.toLowerCase());
		
		//String elementId = service.getAttribute("id");
		
		//String serviceStr = queryMap.get(id.toLowerCase());
		Object serviceObj = queryMap.get(id.toLowerCase());
		
		
		
		//logger.debug("getAttribute serviceStr :" + serviceStr);
		
		//Date nDate = new Date();
		
		Element service = null;
		
		if(serviceObj instanceof String ) {
			service = stringToElement( (String)serviceObj );
		}else {
			service = (Element)serviceObj;
		}
		
		
		//Date kDate = new Date();
		
		//logger.debug("--------------------------");
		//logger.debug("stringToElement time : " + (kDate.getTime()-nDate.getTime()));
		//logger.debug("--------------------------");
		
		String q = "";
		
		//logger.debug("getAttribute id :" + service.getAttribute("id"));
		//logger.debug("getAttribute node :" + XMLUtils.ElementToString(service));
		
		//if(elementId != null && !elementId.equals("")) {
		NodeList list = service.getChildNodes();
			
			//SqlQuery sqlQuery = new SqlQuery("");
			
			//logger.debug("getAttribute start :" + id);
		q = getElementQuery(list, param);
			//logger.debug("getAttribute end :" + id);
		//}
		
		
		
		/*
		ScriptEngineManager mng = new ScriptEngineManager();
		ScriptEngine eng = mng.getEngineByName("js");
		
	    for(int index = 0; index < list.getLength(); index++){
	        if(list.item(index) instanceof CharacterData){
	            CharacterData child = (CharacterData) list.item(index);
	            data = child.getData();

	            if(data != null && data.trim().length() > 0) {
	            	logger.debug("Mapper Query:" + id + "," + data); 
	                q += data;
	            }
	            
	          
	        }else {
	        	logger.debug("Mapper Query getNodeName:" + id + "," + list.item(index).getNodeName()); 
	        	
	        	String test = list.item(index).getAttributes().getNamedItem("test").getNodeValue();
	        	Object istest = false;
	        	try {
	        		
	        		if(param != null ) {
		        		for( String key : param.keySet() ){
		        			
		        			logger.debug(key+":" + param.get(key)); 
		        			
		        			test = test.replace(":"+key, (String) param.get(key));
		                   
		                }
	        		}

	        		logger.debug("test:" + test); 
	        		
	        		istest = eng.eval(test);
	        		
	        		logger.debug("istest:" + istest); 
	        		
	        		if((boolean) istest) {
	        			
	        			 NodeList list2 = list.item(index).getChildNodes();
	        			 
	        			  for(int ik = 0; ik < list2.getLength(); ik++){
	        			        if(list2.item(ik) instanceof CharacterData){
	        			            CharacterData child = (CharacterData) list2.item(ik);
	        			            data = child.getData();

	        			            if(data != null && data.trim().length() > 0) {
	        			            	logger.debug("Mapper if:" + data); 
	        			                q += data;
	        			            }
	        			        }
	        			  }     
	        			 
	        		}
	        		
				} catch (ScriptException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} 
	        }
	    }
	    
	    */
		
		
		//諛붿씤�뵫�맂 荑쇰━ 濡쒓렇
		String bindQuery = q;
		
		if(param != null ) {
    		for( String key : param.keySet() ){
    			
    			//logger.debug(key+":" + param.get(key)); 
    			try {
        			if(param.get(key) instanceof String) {
        				bindQuery = bindQuery.replace(":"+key, "'"+(String) param.get(key)+"'");
        				
        			}else if(param.get(key) instanceof Integer) {
        				bindQuery = bindQuery.replace(":"+key, (Integer)param.get(key)+"" );
        				
        			}else if(param.get(key) instanceof ArrayList) {
        				
        				if(((ArrayList<?>)param.get(key)).size() == 0) {
        					bindQuery = bindQuery.replace(":"+key, "''");
        				}else {
        					bindQuery = bindQuery.replace(":"+key, "'"+param.get(key).toString()+"'");
        				}
        				
        			}
    			}catch(Exception ee) {
    			}
            }
		}
		
		
		logger.debug(id + ":" + q);
		logger.debug(id + " bind Query: " +  bindQuery);
		
		if(q == null || q.equals("")) {
			new Exception("["+id + "] query does not exist.");
		}else {
			return q;
		}
		return "";

		
	}
	
	
	public String getElementQuery(NodeList list , Map<String, Object> param) {
		
		StringBuffer q = new StringBuffer();
		//String data;
				
		ScriptEngineManager mng = new ScriptEngineManager();
		ScriptEngine eng = mng.getEngineByName("js");
		
		for(int index = 0; index < list.getLength(); index++){
			//Node nValue = (Node) list.item(index);
			//String data = nValue.getNodeValue();
        	
	        if(list.item(index) instanceof CharacterData){
			//if(data == null){
	           // CharacterData child = (CharacterData) list.item(index);
	           // data = child.getData();
	        	
	        	Node nValue = (Node) list.item(index);
	        	String data = nValue.getNodeValue();

	            if(data != null && data.trim().length() > 0) {
	            	//logger.debug("Mapper Query:" + data); 
	                //q += data;
	            	q.append(data);
	            }
	            
	        }else {
	        	//logger.debug("Mapper Query getNodeName:" + list.item(index).getNodeName()); 
	        	
	        	String test = list.item(index).getAttributes().getNamedItem("test").getNodeValue();
	        	Object istest = false;
	        	try {
	        		
	        		
	        		test = test.replace(" or ", "||");
	        		test = test.replace(" OR ", "||");
	        		test = test.replace(" and ", "&&");
	        		test = test.replace(" AND ", "&&");
	        		test = test.replace("==", " == ");
	        		test = test.replace("!=", " != ");
	        		
	        		if(param != null ) {
		        		for( String key : param.keySet() ){
		        			
		        			logger.debug(key+":" + param.get(key)); 
		        			try {
		        				
		        				if(param.get(key) instanceof String) {
			        				test = test.replace(":"+key+" ", "'"+(String) param.get(key)+"' ");
			        				
			        			}else if(param.get(key) instanceof Integer) {
			        				test = test.replace(":"+key+" ", (Integer)param.get(key)+" " );
			        				
			        			}else if(param.get(key) instanceof ArrayList) {
			        				
			        				if(((ArrayList<?>)param.get(key)).size() == 0) {
			        					test = test.replace(":"+key+" ", "''");
			        				}else {
			        					test = test.replace(":"+key+" ", "'"+param.get(key).toString()+"' ");
			        				}
			        				
			        			}
		        				
		        			}catch(Exception ee) {
		        				logger.debug(key + ":" + ee.getMessage());
		        			}
		                   
		                }
	        		}
//	        		if(test.contains(":")) {
//		        		logger.debug("test.contains(\":\"):" + test); 
//		        		Pattern pattern = Pattern.compile("(:\\S+)");
//		        		Matcher matcher = pattern.matcher(test);
//		        		while (matcher.find()) {
//		        			test = test.replaceFirst(matcher.group(), "null");
//		        		}
//	        		}

//	        		logger.debug("test:" + test); 
	        		
	        		istest = eng.eval(test);
	        		
//	        		logger.debug("istest:" + istest); 
	        		
	        		if((boolean) istest) {
	        			
	        			 NodeList list2 = list.item(index).getChildNodes();

	        			 String q2 = getElementQuery(list2, param);
	        			 q.append(q2);
	        		}
	        		
				} catch (Exception e) {
					// TODO Auto-generated catch block
					//e.printStackTrace();
					logger.debug(param + ":" + e.getMessage());
				} 
	        }
	    }
		
		
		return q.toString();
	}
	
	

	/**
	 * 議고쉶 荑쇰━ 怨듯넻 �븿�닔
	 * @param jdbcTemplate
	 * @param queryId
	 * @param namedParameters
	 * @return
	 */
	public Object select(NamedParameterJdbcTemplate jdbcTemplate, 
			String queryId, 
			Map<String, Object> namedParameters
			)  {
		
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;

		logger.debug(namedParameters+"");
		
		String sql = get(queryId, namedParameters);
		
		Date sDate = new Date();
		
		Object rs = jdbcTemplate.queryForList(sql,namedParameters);
		
		Date eDate = new Date();
		
		long diff = eDate.getTime() - sDate.getTime() ;
		
		logger.debug("Query Time: "+diff+"ms");
		
		return rs;
	}
	
	public Object select(String queryId, 
			Map<String, Object> namedParameters
			) 
	{
		return select(prijdbcTemplate, queryId, namedParameters);
	}
	
	public Object selectRowSet(NamedParameterJdbcTemplate jdbcTemplate, 
			String queryId, 
			Map<String, Object> namedParameters
			)  {
		
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;

		logger.debug(namedParameters+"");
		
		String sql = get(queryId, namedParameters);
		
		Date sDate = new Date();
		
		Object rs = jdbcTemplate.queryForRowSet(sql,namedParameters);
		
		Date eDate = new Date();
		
		long diff = eDate.getTime() - sDate.getTime() ;
		
		logger.debug("Query Time: "+diff+"ms");
		
		return rs;
	}
	
	public Object selectRowSet(String queryId, 
			Map<String, Object> namedParameters
			) 
	{
		return selectRowSet(prijdbcTemplate, queryId, namedParameters);
	}
	
	public int selectInt(NamedParameterJdbcTemplate jdbcTemplate, String queryId, 
			Map<String, Object> namedParameters
			) 
	{
		
		Object o = select(jdbcTemplate, queryId, namedParameters);
		
		Map<String, Object> m = ((List<Map<String, Object>>)o).get(0);
		
		int n = 0;
		
		for( String key : m.keySet() ){

            n = Integer.parseInt(String.valueOf (  m.get(key)));
        }

		
		return n;
	}
	
	public String selectString(NamedParameterJdbcTemplate jdbcTemplate, String queryId, 
			Map<String, Object> namedParameters
			) 
	{
		
		List<Map<String, Object>>  o = (List<Map<String, Object>>) select(jdbcTemplate, queryId, namedParameters);
		
		if(o.size() == 0) return null;
			
		Map<String, Object> m = o.get(0);
		
		String n = "";
		
		for( String key : m.keySet() ){

            n = String.valueOf (  m.get(key) );
        }
		
		return n;
	}
	
	
	
	/**
	 * �뾽�뜲�씠�듃 荑쇰━ 怨듯넻 �븿�닔
	 * @param jdbcTemplate
	 * @param queryId
	 * @param namedParametersList
	 * @return
	 */
	public int batchUpdate(NamedParameterJdbcTemplate jdbcTemplate, 
			String queryId, 
			List<Map<String, Object>> namedParametersList,
			String rowStatus
			)  {
		
		
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		
		
			
		//�긽�깭媛믨낵 媛숈�寃껊쭔 �떞湲�
		for(int i =0; i<namedParametersList.size(); i++) {
				
			Map<String, Object> map = namedParametersList.get(i);
			
			String status = (String)map.get("rowStatus");
			
			if(queryId.indexOf("Schedule") <= -1) { 
				if(map.get("inputEmp") == null || map.get("inputEmp").equals("") ) {
					map.put("inputEmp", Session.getSession("emp"));
				}
				if(map.get("updEmp") == null || map.get("updEmp").equals("")) {
					map.put("updEmp", Session.getSession("emp"));
				}
				if(map.get("modEmp") == null || map.get("modEmp").equals("")) {
					map.put("modEmp", Session.getSession("emp"));
				}
				if(map.get("delEmp") == null || map.get("delEmp").equals("")) {
					map.put("delEmp", Session.getSession("emp"));
				}
			}
			//�쟾泥� ���옣 �씠嫄곕굹 �긽�깭媛믪뿉 �빐�떦�븯�뒗 �뜲�씠�꽣 
			if(rowStatus.equals(UPD_TYPE_ALL) || status.equals(rowStatus)) {
				dataList.add(map);
			}
			
			logger.debug("namedParametersList["+i+"]:" +map);
		}
		
		
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		
		String sql = get(queryId, null);
		
		SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(dataList);
		 
        int[] updateCounts = jdbcTemplate.batchUpdate(sql, batch);
        
        return updateCounts.length;
	}
	
	public int batchUpdate(String queryId, 
			List<Map<String, Object>> namedParametersList,
			String rowStatus
			)  {
        
        return batchUpdate(prijdbcTemplate, queryId, namedParametersList, rowStatus);
	}
	
	public int batchUpdate(String queryId, String jsonStr,String rowStatus
			) {
        
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
        return batchUpdate(prijdbcTemplate, queryId, list, rowStatus);
	}
	
	
	/**
	 * �뾽�뜲�씠�듃 荑쇰━ 怨듯넻 �븿�닔
	 * @param jdbcTemplate
	 * @param queryId
	 * @param namedParametersList
	 * @return
	 */
	public int batchUpdateQuery(NamedParameterJdbcTemplate jdbcTemplate, 
			String query, 
			List<Map<String, Object>> namedParametersList,
			String rowStatus
			)  {
		
		
		List<Map<String, Object>> dataList = new ArrayList<Map<String, Object>>();
		
		if(namedParametersList.size() <= 0) return 0;
			
		//�긽�깭媛믨낵 媛숈�寃껊쭔 �떞湲�
		for(int i =0; i<namedParametersList.size(); i++) {
				
			Map<String, Object> map = namedParametersList.get(i);
			
			String status = (String)map.get("rowStatus");
			
			//�쟾泥� ���옣 �씠嫄곕굹 �긽�깭媛믪뿉 �빐�떦�븯�뒗 �뜲�씠�꽣 
			if(rowStatus.equals(UPD_TYPE_ALL) || status.equals(rowStatus)) {
				dataList.add(map);
				
				logger.debug("ParametersList["+i+"]:" +map);
			}
			
			
		}
		
		logger.debug("query:" +query);
		
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
				
		SqlParameterSource[] batch = SqlParameterSourceUtils.createBatch(dataList);
		 
        int[] updateCounts = jdbcTemplate.batchUpdate(query, batch);
        
        return updateCounts.length;
	}
	
	/**
	 * UPDATE �뜲�씠�꽣留� �떎�뻾
	 * @param queryId
	 * @param jsonStr
	 * @return
	 * @throws Exception
	 */
	public int update(String queryId, String jsonStr
			) {
        
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
		logger.debug(jsonStr);
        return batchUpdate(prijdbcTemplate, queryId, list, UPD_TYPE_UPDATE);
	}
	
	public int update(NamedParameterJdbcTemplate jdbcTemplate, 
					String queryId, String jsonStr
			) {
        
		
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
        return batchUpdate(jdbcTemplate, queryId, list, UPD_TYPE_UPDATE);
	}
	
	public int update(NamedParameterJdbcTemplate jdbcTemplate, 
			String queryId, List<Map<String, Object>>  list
		) {
	
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		return batchUpdate(jdbcTemplate, queryId, list, UPD_TYPE_UPDATE);
	}
	
	/**
	 * INSERT �뜲�씠�꽣留� �떎�뻾
	 * @param queryId
	 * @param jsonStr
	 * @return
	 * @throws Exception
	 */
	public int insert(String queryId, String jsonStr
			) {
        
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
		logger.debug(jsonStr);
        return batchUpdate(prijdbcTemplate, queryId, list, UPD_TYPE_INSERT);
	}
	
	public int insert(NamedParameterJdbcTemplate jdbcTemplate, 
					String queryId, String jsonStr
			) {
        
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
        return batchUpdate(jdbcTemplate, queryId, list, UPD_TYPE_INSERT);
	}
	
	public int insert(NamedParameterJdbcTemplate jdbcTemplate, 
			String queryId, List<Map<String, Object>>  list
		) {
	
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		return batchUpdate(jdbcTemplate, queryId, list, UPD_TYPE_INSERT);
	}
	
	/**
	 * DELETE �뜲�씠�꽣留� �떎�뻾
	 * @param queryId
	 * @param jsonStr
	 * @return
	 * @throws Exception
	 */
	public int delete(String queryId, String jsonStr
			) {
        
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
		logger.debug(jsonStr);
        return batchUpdate(prijdbcTemplate, queryId, list, UPD_TYPE_DELETE);
	}
	
	public int delete(NamedParameterJdbcTemplate jdbcTemplate, 
					String queryId, String jsonStr
			) {
        
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		
		List<Map<String, Object>>  list = Util.jsonStringToList(jsonStr);
        return batchUpdate(jdbcTemplate, queryId, list, UPD_TYPE_DELETE);
	}
	
	public int delete(NamedParameterJdbcTemplate jdbcTemplate, 
			String queryId, List<Map<String, Object>>  list
		) {
	
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;
		return batchUpdate(jdbcTemplate, queryId, list, UPD_TYPE_DELETE);
	}
	
	public int executeUpdate(String queryId, Map<String, Object> namedParameters) {
		return executeUpdate(prijdbcTemplate, queryId, namedParameters);
	}
	
	public int executeUpdate(NamedParameterJdbcTemplate jdbcTemplate, String queryId, Map<String, Object> namedParameters) {
		if(jdbcTemplate == null) jdbcTemplate = prijdbcTemplate;

		logger.debug(namedParameters+"");
		
		if(queryId.indexOf("Schedule") <= -1) { 
			if(namedParameters.get("inputEmp") == null || namedParameters.get("inputEmp").equals("") ) {
				namedParameters.put("inputEmp", Session.getSession("emp"));
			}
			if(namedParameters.get("updEmp") == null || namedParameters.get("updEmp").equals("")) {
				namedParameters.put("updEmp", Session.getSession("emp"));
			}
			if(namedParameters.get("modEmp") == null || namedParameters.get("modEmp").equals("")) {
				namedParameters.put("modEmp", Session.getSession("emp"));
			}
			if(namedParameters.get("delEmp") == null || namedParameters.get("delEmp").equals("")) {
				namedParameters.put("delEmp", Session.getSession("emp"));
			}
		}
		
		String sql = get(queryId, namedParameters);
		
		Date sDate = new Date();
		
		int rs = jdbcTemplate.update(sql, namedParameters);
		
		Date eDate = new Date();
		
		long diff = eDate.getTime() - sDate.getTime() ;
		
		logger.debug("Query Time: "+diff+"ms");
		
		return rs;
	
	}
	
}
