package kr.co.scrab.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import kr.co.scrab.kccfw.common.Session;
import kr.co.scrab.kccfw.common.SqlQuery;


@Service
@Transactional(rollbackFor = {RuntimeException.class, Exception.class})
public class ResourceUiService {

	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	//@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@Autowired
	SqlQuery sqlQuery;
	
	//기본언어 - header.html 셋팅과 동기화 해야함.
	String[] supportLang = {/*"ko",*/ "en", "zh", "ja", "ru" };
	String defaultLang = "en"; 

	/**
	 * 그룹ID 리소스 조회
	 * @param groupId
	 * 
	 * @return
	 * "GROUP_ID" :	{
			contents : {
				"RESOURCE_NM" : {content : "오디움", type:"text", sort : 1}
				,"RESOURCE_NM" : {content : "개요", type:"text", sort : 2}
			},
			group : {groupSort : 1, parentSeq : 0}
		}
	 */
	public Map<String,Object> selResource(String groupId) {
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("groupId", groupId);
		params.put("language", Session.getLanguage(supportLang, defaultLang));
		
		//1) SqlQuery 유틸을 통한 조회
		List<Map<String, Object>> selObj = (List<Map<String, Object>> )sqlQuery.select(jdbcTemplate, "cm.ResourceUi.selResource", params); 
		
		//Map<String, Object> rtnMap = new HashMap<String, Object>();
		Map<String, Object> groupMap = new HashMap<String, Object>();
		
		//그룹정보
		for(int i=0; i<selObj.size(); i++) {
			Map<String, Object> m = selObj.get(i);
			
			//GROUP 정보
			if(i == 0) {
				Map<String, Object> group = new HashMap<String, Object>();
				group.put("groupSort", m.get("GROUP_SORT") );
				group.put("parentSeq", m.get("PARENT_RESOURCE_GROUP_SEQ") );
				group.put("groupNm", m.get("GROUP_NM") );
				group.put("groupId", m.get("GROUP_ID") );
				
				groupMap.put( "group" , group);
				
			}
			
			
		}
		
		//컨텐츠 정보
		Map<String, Object> contents = new HashMap<String, Object>();
		
		for(int i=0; i<selObj.size(); i++) {
			Map<String, Object> m = selObj.get(i);
					
			Map<String, Object> conMap = new HashMap<String, Object>();
			
			conMap.put("content" ,  m.get("RESOURCE_CONTENT") );
			conMap.put("type", m.get("RESOURCE_TYPE") );
			conMap.put("sort", m.get("SORT") );
			
			contents.put( m.get("RESOURCE_NM").toString() , conMap);
			
		}
		
		groupMap.put( groupId , contents);
		
		
		//rtnMap.put(groupNm, groupMap);
		
		return groupMap;
	}
	
	/**
	 * 그룹ID 리소스 심플 조회
	 * @param groupId
	 * @return
	 * { "RESOURCE_NM" : "CONTENT", "RESOURCE_NM" : "CONTENT"}
	 * 
	 */
	public Map<String,Object> selResourceSimple(String groupId) {
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("groupId", groupId);
		params.put("language", Session.getLanguage(supportLang, defaultLang));
		
		//1) SqlQuery 유틸을 통한 조회
		List<Map<String, Object>> selObj = (List<Map<String, Object>> )sqlQuery.select(jdbcTemplate, "cm.ResourceUi.selResource", params); 
		
				
		//컨텐츠 정보
		Map<String, Object> contents = new HashMap<String, Object>();
		
		for(int i=0; i<selObj.size(); i++) {
			Map<String, Object> m = selObj.get(i);
								
			contents.put( m.get("RESOURCE_NM").toString() , m.get("RESOURCE_CONTENT") );
			
		}
		
		
		return contents;
	}
	
	/**
	 * 그룹ID 리소스 조회
	 * @param groupId
	 * @return
	 * [{ RESOURCE_NM : "CONTENT", RESOURCE_CONTENT : "CONTENT" .....}]
	 * 
	 */
	public List<Map<String, Object>> selResourceList(String groupId) {
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("groupId", groupId);
		params.put("language", Session.getLanguage(supportLang, defaultLang));
		
		//1) SqlQuery 유틸을 통한 조회
		List<Map<String, Object>> selObj = (List<Map<String, Object>> )sqlQuery.select(jdbcTemplate, "cm.ResourceUi.selResourceList", params); 
		
		return selObj;
	}
	
	/**
	 * 그룹ID로 하위 그룹 리소스 리스트 조회
	 * @param groupId
	 * @return
	 * [
	 * 	 [{ CONTENT : "오디움1-1", TYPE:"text", SORT : 1},  { CONTENT : "오디움1-2", TYPE:"text", SORT : 2}]
	 *   [{ CONTENT : "오디움2-1", TYPE:"text", SORT : 1},  { CONTENT : "오디움2-2", TYPE:"text", SORT : 2}]
	 * ]
	 * 
	 */
	public List<List<Map<String, Object>>> selResourceChildList(String groupId) {
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("groupId", groupId);
		params.put("language", Session.getLanguage(supportLang, defaultLang));
		
		//1) SqlQuery 유틸을 통한 조회
		List<Map<String, Object>> selObj = (List<Map<String, Object>> )sqlQuery.select(jdbcTemplate, "cm.ResourceUi.selChildGroup", params);
		
		//컨텐츠 정보
		List<List<Map<String, Object>>> groupList = new ArrayList<List<Map<String, Object>>>();


		for(int i=0; i<selObj.size(); i++) {
			Map<String, Object> m = selObj.get(i);
			String resourceGroupId = (String)m.get("RESOURCE_GROUP_ID");
			
			List<Map<String, Object>> child =  selResourceList(resourceGroupId);
			
			if(child != null && child.size() > 0) {
				groupList.add( child );
			}
				
		}

		
		return groupList;
		
	}
	
	
	/**
	 * 그룹ID로 하위 그룹 리소스 심플 조회
	 * @param groupId
	 * @return
	 * [
	 * 	{ "RESOURCE_NM" : "CONTENT", "RESOURCE_NM" : "CONTENT", RESOURCE_GROUP_ID : "RESOURCE_GROUP_ID"},
	 *  { "RESOURCE_NM" : "CONTENT", "RESOURCE_NM" : "CONTENT", RESOURCE_GROUP_ID : "RESOURCE_GROUP_ID"},
	 * ]
	 * 
	 */
	public List<Map<String, Object>> selResourceChildSimple(String groupId) {
		
		Map<String, Object> params= new HashMap<String, Object>();
		params.put("groupId", groupId);
		params.put("language", Session.getLanguage(supportLang, defaultLang));
		
		//1) SqlQuery 유틸을 통한 조회
		List<Map<String, Object>> selObj = (List<Map<String, Object>> )sqlQuery.select(jdbcTemplate, "cm.ResourceUi.selResourceChild", params); 
		
				
		//컨텐츠 정보
		List<Map<String, Object>> groupList = new ArrayList<Map<String, Object>>();
		Map<String, Object> contents = new HashMap<String, Object>();
		
		String preResourceGroupId = "";
		
		for(int i=0; i<selObj.size(); i++) {
			Map<String, Object> m = selObj.get(i);
			
			String resourceGroupId = (String)m.get("RESOURCE_GROUP_ID");
			
			if(preResourceGroupId.equals("") || preResourceGroupId.equals(resourceGroupId)) {
				//같으면
				contents.put( m.get("RESOURCE_NM").toString() , m.get("RESOURCE_CONTENT") );
				preResourceGroupId = resourceGroupId;
				
			}
			
			if(selObj.size() == 1 || selObj.size()-1 == i || !preResourceGroupId.equals(resourceGroupId)) {
				//리스트에 담기
				Map<String, Object> cont = new HashMap<String, Object>();
				cont.putAll(contents);
				cont.put("RESOURCE_GROUP_ID", preResourceGroupId);
				groupList.add(cont);
				
				contents = new HashMap<String, Object>();
				
				contents.put( m.get("RESOURCE_NM").toString() , m.get("RESOURCE_CONTENT") );
				
			}
			
			
			preResourceGroupId = resourceGroupId;
			
		}
		
		
		return groupList;
	}
	

}
