package kr.co.scrab.controller;

import java.io.UnsupportedEncodingException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.scrab.kccfw.common.Session;
import kr.co.scrab.kccfw.common.SqlQuery;
import kr.co.scrab.kccfw.common.Util;
import kr.co.scrab.service.ResourceUiService;

@Controller
public class HomeController {
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	public static String startDatetime = "";
	
	@Autowired
	SqlQuery sqlQuery;
	
	@Autowired
	ResourceUiService resourceUiService;
	
	@Autowired
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	@RequestMapping(value = "/")
	public ModelAndView index(HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = exhibitions(request, response, "EX01");
		
        return mav;
	}
	
	@RequestMapping(value = "/audio")
	public ModelAndView indexAudio(HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = exhibitions(request, response, "EX01");
		
        return mav;
	}
	
	
	public ModelAndView main(HttpServletRequest request, HttpServletResponse response) {
		
		
		ModelAndView mav = new ModelAndView("content/main")
                .addObject("path", "content/list");
		

		if(startDatetime.equals("")) {
			startDatetime = System.currentTimeMillis() + "";
		}
		
		
			
		mav.addObject("ts", startDatetime);
		
		return mav;
	}
	
	
	@RequestMapping(value = "/audio/{exhibitions}")
	public ModelAndView exhibitions(HttpServletRequest request, HttpServletResponse response
			,@PathVariable String exhibitions
			) {
		
		//전시장 번호
		logger.debug("exhibitions : {}" , exhibitions);
	
		
		ModelAndView mav = main(request, response );
		mav.addObject("path", "content/list");
		mav.addObject("noticeYn", "N");

		//전시 하위 목록
		List<List<Map<String, Object>>> groupList = resourceUiService.selResourceChildList(exhibitions);
		
		if(groupList.size() > 0) {
			mav.addObject("EXHIBITION", groupList);
		}else {
			mav.addObject("EXHIBITION", null );
		}
		
		//기본
		Map<String, Object> mapBasic = resourceUiService.selResourceSimple(exhibitions);
		mav.addObject("BASIC", mapBasic );
				
				
		mav.addObject("EXHIBITIONS_ID", exhibitions );
				
		//logger.debug("EXHIBITION : {}" , groupList);
        return mav;

	}
	
	
	
	@RequestMapping(value = "/audio/{exhibitions}/{trackNum}")
	public ModelAndView exhibitionsDetail(HttpServletRequest request, HttpServletResponse response
			,@PathVariable String exhibitions
			,@PathVariable String trackNum
			) {
		
		//오디오 번호
		logger.debug("trackNum : {}" , trackNum);
		
		ModelAndView mav = main(request, response );
		mav.addObject("path", "content/audio");
		mav.addObject("noticeYn", "N");

		//오디오 목록
		List<Map<String, Object>> list = resourceUiService.selResourceList(trackNum);
		
		if(list.size() > 0) {
			mav.addObject("AUDIO_LIST", list);
		}else {
			mav.addObject("AUDIO_LIST", null );
		}
		
		//이전 다음 트랙 
		List<Map<String, Object>> groupList = resourceUiService.selResourceChildSimple(exhibitions);
		String prevTrack = "";
		String nextTrack = "";
		
		for(int i=0; i<groupList.size(); i++) {
			
			Map<String, Object> m = groupList.get(i);
			
			if(m.get("RESOURCE_GROUP_ID").equals(trackNum)) {
				//이전 트랙
				if(i > 0) {
					prevTrack = "/audio/" + exhibitions + "/" + (String)groupList.get(i-1).get("RESOURCE_GROUP_ID");
				}
				
				//다음 트랙
				if(i < groupList.size()-1) {
					nextTrack = "/audio/" + exhibitions + "/" + (String)groupList.get(i+1).get("RESOURCE_GROUP_ID");
				}
			}
		}
		mav.addObject("PREV_TRACK",  prevTrack );
		mav.addObject("NEXT_TRACK",  nextTrack );
		mav.addObject("NOW_TRACK", trackNum );
		
		//오디오 목록
		Map<String, Object> map = resourceUiService.selResourceSimple(trackNum);
		mav.addObject("AUDIO_MAP", map );
		
		
		//기본
		Map<String, Object> mapBasic = resourceUiService.selResourceSimple(exhibitions);
		mav.addObject("BASIC", mapBasic );
		
		mav.addObject("EXHIBITIONS_ID", exhibitions );
		
		
		
		
		//logger.debug("collections : {}" , list);
        return mav;

	}
	
	
	@RequestMapping(value = "/map")
	public ModelAndView map(HttpServletRequest request, HttpServletResponse response) {
		ModelAndView mav = main(request, response );
		mav.addObject("path", "content/map");
		mav.addObject("noticeYn", "N");
		
		
		//목록
		List<Map<String, Object>> list = resourceUiService.selResourceList("MAP");
		mav.addObject("MAP_LIST", list );
		
		Map<String, Object>  map = resourceUiService.selResourceSimple("MAP");
		mav.addObject("MAP", map );
		
        return mav;
	}
	

}
