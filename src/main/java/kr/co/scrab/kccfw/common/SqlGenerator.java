package kr.co.scrab.kccfw.common;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.jdbc.support.rowset.SqlRowSetMetaData;
import org.springframework.stereotype.Component;

import kr.co.scrab.kccfw.util.Result;


/**
 * @author Administrator
 * Sql 생성기
 */
public class SqlGenerator {
	
	SqlQuery sqlQuery;
	
	private final Logger logger = LoggerFactory.getLogger(this.getClass());
	
	private List<Object> dtoList = new ArrayList<Object>();
	private List<Object> insDtoList = new ArrayList<Object>();
	private List<Object> updDtoList = new ArrayList<Object>();
	private List<Object> delDtoList = new ArrayList<Object>();
		
	private List<Map<String, Object>> hashMapList = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> insHashMapList = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> updHashMapList = new ArrayList<Map<String, Object>>();
	private List<Map<String, Object>> delHashMapList = new ArrayList<Map<String, Object>>();
	
	private String taskMode;
	private String strInsQuery;
	private String strUpdQuery;
	private String strDelQuery;
	private String strDelRealQuery;
	private String strSelInsQuery;
	private String strInsHistQuery;
	private String strMergeIntoQuery;
	private String strSelectQuery;
	private String jndiName;
	private String tableName;
	private String seqName;
	private String historyTableName;
	private String seqColName;
	private String executeSequenceName;
	private String dtoNm;
	private String timeKeyColumnName;
	private String[] pkColNames;
	private String[] insColNames;
	private String[] updColNames;
	private String[] delUpdColNames;
	private String[] mergeIntoOnColNames;
	private LinkedHashMap<String, String> orderByColNames;

	private  Map<String, Object> params = null;
	private String empNum = null; 
	private String serviceName = null; 
	private String methodName = null; 
	
	private String logPrintYn = "Y";		//BATCH UPDATE 실행시 데이터 로그 출력여부
	private String inputDateUpdateYn = "N";		//입력일자 강제 업데이트여부
	private String delDateUpdateYn = "N";		//삭제일자 강제 업데이트여부
	private String queryCondition = "";		//UPDATE나 DELETE 시 조건을 PK 로 할지 SEQ 로 할지 구분, 2019.07.12 DWKIM ROWNUM 추가. 한건만 가져오도록;
	private String executeSequenceYn = "Y";		//INSERT 시 SEQUENCE.NEXTVAL 실행여부
	private String dbKind = "ORACLE";	
	
	private boolean isGenQueryComplete = false; //쿼리 생성여부
	
	private NamedParameterJdbcTemplate jdbcTemplate;
	
	private List<String> insColNameList = new ArrayList<String>();			//INSERT 쿼리 컬럼
	private List<String> updColNameList = new ArrayList<String>();			//UPDATE 쿼리 컬럼
	private List<String> delColNameList = new ArrayList<String>();			//DELETE 쿼리 컬럼
	private List<String> delRealColNameList = new ArrayList<String>();		//DELETE REAL 쿼리 컬럼
	private List<String> selInsColNameList = new ArrayList<String>();		//SELECT INSERT 쿼리 컬럼
	private List<String> mergeIntoColNameList = new ArrayList<String>();	//MERGE INTO 쿼리 컬럼
	private List<String> selectColNameList = new ArrayList<String>();		//SELECT 쿼리 컬럼
	private List<String> insHistColNameList = new ArrayList<String>();		//INSERT HISTOR 쿼리 컬럼
	
	private Map<String,String> dtoFiledMap = new HashMap<String,String>();
	
	
	private Map<String,Object> selectParam = new HashMap<String,Object>();	//SELECT 매개변수
	
		
	public SqlGenerator(){
		//조회에만 사용
		taskMode = "Select";
	}
	
	/**
	 * 쿼리 제너레이터 셋팅
	 * @param jdbcTemplate 데이터케넥션
	 * @param pHashMapList 저장된 데이터 Json String or List<Map>
	 * @param pTableName 테이블명
	 * @param pEmpNum 사번 INPUT_EMP, MOD_EMP Update 시 사용됨
	 * @param params 모둔 파라미터로 History 남길경우 사용
	 * @throws Exception 
	 * <pre>
		SqlGenerator sqlGenRoomList = new SqlGenerator(jdbcTemplate, params.get("dlt_userDept"), 
												"T_C00W_PT_ROOM", SessionUser.getSession(SessionUser.EMP), params);
			
			
		//디테일부문 저장을 위해 시퀀스 셋팅 (파라미터는 CamelCase) 
		//List<Map<String, Object>> roomList = sqlGenRoomList.getHashMapList();
		int seq = sqlGenRoomList.getSequence("SEQC00W_PT_ROOM"); 
		sqlGenRoomList.setEmptytVal("seqPtRoom", seq);
		
		
		//PK 컬럼을 지정 (시퀀스 값을 지정했으므로 PK 지정)
		String[] pkColNames = {"SEQ_PT_ROOM"};   
		sqlGenRoomList.setPkColNames( pkColNames );
		
		//데이터 실제 저장 쿼리 실행부
		sqlGenRoomList.insert();
		sqlGenRoomList.update();
		sqlGenRoomList.delete();
		
	  </pre>
	 */
	public SqlGenerator(NamedParameterJdbcTemplate pJdbcTemplate, Object pHashMapList, String pTableName, 
			String pEmpNum, Map<String, Object> params) throws Exception{
		
		set(pJdbcTemplate, pHashMapList, pTableName, 
				pEmpNum, params);
		
	}
	public SqlGenerator(NamedParameterJdbcTemplate pJdbcTemplate, Object pHashMapList, String pTableName, 
			String pEmpNum) throws Exception{
		
		set(pJdbcTemplate, pHashMapList, pTableName, 
				pEmpNum, null);
		
	}
	
	public void clear(){
		//초기화
		this.tableName = null;
		this.seqColName = null;
		this.pkColNames = null;
		this.updColNames = null;
		this.executeSequenceName = null;
	}
	
	//hashmap 용
	/**
	 * 쿼리 제너레이터 셋팅
	 * @param jdbcTemplate 데이터케넥션
	 * @param pHashMapList 저장된 데이터 Json String or List<Map>
	 * @param pTableName 테이블명
	 * @param pEmpNum 사번 INPUT_EMP, MOD_EMP Update 시 사용됨
	 * @param params 모둔 파라미터로 History 남길경우 사용
	 * @throws Exception 
	 */
	public void set(NamedParameterJdbcTemplate pJdbcTemplate, Object pHashMapList, String pTableName, 
			String pEmpNum, Map<String, Object> params) throws Exception {
		
		clear(); //초기화
				
		if(sqlQuery == null) {
			sqlQuery = new SqlQuery(pJdbcTemplate);
		}
		
		
		//값세팅
		this.empNum = pEmpNum;
		this.params = params;
		this.jdbcTemplate = pJdbcTemplate;
		this.tableName =  pTableName;
		
		if(params != null) {
		
			Map<String, String> service = (Map<String, String>) params.get(Result.SERVICE);
	
			if(service != null) {
				this.serviceName = service.get(Result.SERVICE_NAME);
				this.methodName = service.get(Result.METHOD_NAME);
			}
			
		}
		
		taskMode = "HashMap";
		
		if( pHashMapList instanceof String ) {
			this.hashMapList =  Util.jsonStringToList( (String)pHashMapList );
			
		}else if( pHashMapList instanceof Map ) {
			hashMapList.add((Map<String, Object>) pHashMapList);
			
		}else{
			this.hashMapList = (List<Map<String, Object>>)pHashMapList;
		}
		
		
		for( int i = 0; i < hashMapList.size(); i ++ ) {
			
			Map<String, Object> hashMap = hashMapList.get(i);
			
			String rowStatus = hashMap.get("rowStatus").toString();
			
			if( sqlQuery.UPD_TYPE_INSERT.equals( rowStatus ) ) {
				insHashMapList.add(hashMap);
				
			} else if( sqlQuery.UPD_TYPE_UPDATE.equals( rowStatus ) ) {
				updHashMapList.add(hashMap);
				
			} else if( sqlQuery.UPD_TYPE_DELETE.equals( rowStatus ) ) {
				delHashMapList.add(hashMap);
				
			}				
		}
		
		//genStrQuery();
		
	}
	
	
	/**
	 * 시퀀스를 조회하여 가져온다.
	 * @return
	 * @throws Exception
	 */
	public int getSequence(NamedParameterJdbcTemplate pJdbcTemplate, String pSequenceName) throws Exception{
		
		if( pSequenceName == null ) {
			throw new Exception("pSequenceName 시퀀스명이 지정되지 않았습니다.");
		}
		
		String query = "";
		
		if( dbKind.equals("ORACLE") ) {
			query = "SELECT "+ pSequenceName +".NEXTVAL FROM DUAL";
		} else {
			query = "SELECT NEXT VALUE FOR " + pSequenceName;
		}
		
		int seq = pJdbcTemplate.getJdbcOperations().queryForObject(query, Integer.class);

		return seq;
		
	}
	
	/**
	 * 시퀀스를 조회하여 가져온다.
	 * @return
	 * @throws Exception
	 */
	public int getSequence(String pSequenceName) throws Exception{
		return getSequence(jdbcTemplate, pSequenceName);
	}
	
	/**
	 * 빈 값을 채워준다.
	 * @param list
	 * @param colNm
	 * @param value
	 */
	public void setEmptytVal(List<Map<String, Object>> list, String colNm, Object value) {
		
		//빈값이 공백이면 변경한다.
		for(int i=0; i<list.size(); i++) {
			Map<String, Object> map = list.get(i);

			if(map.get(colNm) == null || map.get(colNm).equals("")){
				map.put(colNm, value);
			}

		}
				
	}
	
	/**
	 * 빈  값을 채워준다.
	 * @param colNm
	 * @param value
	 */
	public void setEmptytVal(String colNm, Object value) {
		
		setEmptytVal(hashMapList, colNm, value);
				
	}
	
	
	/**
	 * Map 의  값을 변경한다.
	 * @param colNm
	 * @param value
	 */
	public void setColVal(List<Map<String, Object>> list, String colNm, Object value) {
		
		for(int i=0; i<list.size(); i++) {
			Map<String, Object> map = list.get(i);
			map.put(colNm, value);
		}
				
	}
	
	/**
	 * Map 의  값을 변경한다.
	 * @param colNm
	 * @param value
	 */
	public void setColVal(String colNm, Object value) {
		
		setColVal(hashMapList, colNm, value);
				
	}
	
	/**
	 * 쿼리문 생성
	 * @throws Exception 
	 */
	private void genStrQuery() throws Exception{
		
		
		if(isGenQueryComplete) return;//쿼리 생성완료 체크
			
			
		//테이블 체크
		if(tableName == null) {
			throw new Exception("tableName이 정의되지 않았습니다. \n setTableName()");
		}
		
		//seq 와 pk 가 없으면 테이블명과 동일하게 seq를 자동 정의한다.
		if(seqColName == null && pkColNames == null){
			
			if(tableName.indexOf("T_") == 0) {
				
				String tableNameRe = tableName.replaceFirst("T_", "T"); // T_C00W_REPORT_TEMP_DEPT -> TC00W_REPORT_TEMP_DEPT
				
				int underStarIndex = tableNameRe.indexOf("_");
				seqColName = "SEQ"+ tableNameRe.substring(underStarIndex);
				
			}else {
				int underStarIndex = tableName.indexOf("_");
				seqColName = "SEQ"+ tableName.substring(underStarIndex);
				
			}
						
		}
		

		
		if( executeSequenceName == null ) {
			
			//시퀀스 이름 생성
			if(tableName.indexOf("T_") == 0) {
				executeSequenceName = tableName.replaceFirst("T_", "SEQ");
			}else {
				executeSequenceName = tableName.replaceFirst("T", "SEQ");
			}
			
		} 
		

		String query = "";
		
		if( dbKind.equals("ORACLE") ) {
			query = "SELECT * FROM " + tableName + " WHERE ROWNUM = 1";
		} else {
			query = "SELECT TOP 1 * FROM " + tableName  + "(NOLOCK)";
		}
		
		try {
			SqlRowSet rs = jdbcTemplate.getJdbcOperations().queryForRowSet(query);
			
			SqlRowSetMetaData meta = rs.getMetaData();
			
			String[] colNames = meta.getColumnNames();
			
			for(int i=1; i <= colNames.length; i++){
				//logger.debug(meta.getColumnType(i) + "");
				//logger.debug(meta.getSchemaName(i) + "");
			}
	
			
			/** Insert 구문 생성**/			
			strInsQuery = makeInsQuery(colNames, meta);	
			//logger.debug("strInsQuery :" + strInsQuery);
			
			/**update 구문 생성**/
			strUpdQuery = makeUpdQuery(colNames, meta);
			//logger.debug("strUpdQuery :" + strUpdQuery);
			
			/**delete 구문 생성**/			
			strDelQuery = makeDelQuery(colNames, meta);
			//logger.debug("strDelQuery :" + strDelQuery);
			
			
			/**Real Delete 구문 생성**/			
			strDelRealQuery = makeDelRealQuery(colNames, meta);
				
			/** Select Insert 구문 생성**/						
			strSelInsQuery = makeSelInsQuery(colNames, meta);
				
			/** Merge Into 구문 생성**/							
			strMergeIntoQuery = makeMergeIntoQuery(colNames, meta);
			
			/** Select 구문 생성**/													
			strSelectQuery = makeSelQuery(colNames, meta);	
			
//			System.out.println(strSelectQuery);
				
			
			/** Insert History 구문 생성**/
			if( historyTableName != null ) {				
				
				
				query = "SELECT * FROM " + historyTableName + " WHERE ROWNUM = 1";
				
				SqlRowSet rsHist = jdbcTemplate.getJdbcOperations().queryForRowSet(query);
					
				SqlRowSetMetaData metaHist = rsHist.getMetaData();
					
				String[] colNamesHist = metaHist.getColumnNames();
				
				strInsHistQuery = makeInsHistQuery(colNamesHist, meta.getColumnNames());
			}
			
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		isGenQueryComplete = true; //쿼리 생성 완료
		
	}
	
	/**
	 * INSERT 쿼리 생성
	 */
	private String makeInsQuery(String[] colNames, SqlRowSetMetaData meta) {
		StringBuilder sbSql = new StringBuilder();
				
		sbSql.append("INSERT INTO " + tableName + " ( " + "\n");
		
		for(int i=0; i < colNames.length; i++){
			
			
			if(
//					colNames[i].equals("UPD_DATE") || colNames[i].equals("UPD_EMP") || colNames[i].equals("UPD_DATETIME") ||
//					colNames[i].equals("MOD_DATE") || colNames[i].equals("MOD_EMP") || colNames[i].equals("MOD_DATETIME") ||
					colNames[i].equals("DEL_DATE") || colNames[i].equals("DEL_EMP") || colNames[i].equals("DEL_DATETIME") || 
					colNames[i].equals("MODIFIER")){
				continue;
			}
			
			
			if(colNames[i].equals(seqColName)){
				sbSql.append("   " + colNames[i] + ", \n");
			}else if(colNames[i].equals("INPUT_DATE") || colNames[i].equals("UPD_DATE") || colNames[i].equals("MOD_DATE")){
				sbSql.append("   " + colNames[i] + ", \n");
				
			}else if(colNames[i].equals("INPUT_TIME") || colNames[i].equals("UPD_TIME") || colNames[i].equals("MOD_TIME")){
				sbSql.append("   " + colNames[i] + ", \n");
				
			}else if(colNames[i].equals("INPUT_DATETIME") || colNames[i].equals("UPD_DATETIME") || colNames[i].equals("MOD_DATETIME") ||
					colNames[i].equals("CREATETIME") || colNames[i].equals("LASTUPDATETIME") || colNames[i].equals("CREATE_DATETIME") ||
					colNames[i].equals("CHECKOUTTIME") ){
				sbSql.append("   " + colNames[i] + ", \n");
			
			}else if(colNames[i].equals("LAST_EVENTTIME")){
				sbSql.append("   " + colNames[i] + ", \n");
				
			}else if(colNames[i].equals("ISVALID")){
				sbSql.append("   " + colNames[i] + ", \n");
				
			}else if( colNames[i].equals("INPUT_EMP") || colNames[i].equals("UPD_EMP") || colNames[i].equals("MOD_EMP") ||
					colNames[i].equals("CREATOR") || colNames[i].equals("CREATEUSERID") || colNames[i].equals("LASTUPDATEUSERID") ||
					colNames[i].equals("CHECKOUTUSERID") ){
				sbSql.append("   " + colNames[i] + ", \n");
											
			} else if( colNames[i].equals(timeKeyColumnName) ) {
				sbSql.append("   " + colNames[i] + ", \n");
			} else {
				if(insColNames != null) {
					//컬럼지정값에서 동일한 컬럼만 update 한다.
					for(int k=0; k<insColNames.length; k++){
						if(insColNames[k].equals(colNames[i])){
	
							sbSql.append("   " + colNames[i]  + ", \n");
							break;
						}
					}
				}else{				
					sbSql.append("   " + colNames[i]  + ", \n");
				}
			}
							
//			
//			if(insColNames != null) {
//				//컬럼지정값에서 동일한 컬럼만 update 한다.
//				for(int k=0; k<insColNames.length; k++){
//					if(insColNames[k].equals(colNames[i])){
//						rtnColumnText = convertColumnText(colNames[i], "INSERT", "TARGET", meta.getColumnTypeName(i + 1));
//						sbSql.append("   " + rtnColumnText.get("COLUMN_TEXT")  + ", \n");
//						break;
//					}
//				}
//			}else{				
//				rtnColumnText = convertColumnText(colNames[i], "INSERT", "TARGET", meta.getColumnTypeName(i + 1));
//				sbSql.append("   " + rtnColumnText.get("COLUMN_TEXT")  + ", \n");
//			}
			
//			if(colNames[i].equals(seqColName) ||
//					colNames[i].equals("INPUT_DATE") || colNames[i].equals("INPUT_TIME") || colNames[i].equals("INPUT_DATETIME") ||
//					colNames[i].equals("UPD_DATE") || colNames[i].equals("UPD_TIME") || colNames[i].equals("UPD_DATETIME") ||
//					colNames[i].equals("MOD_DATE") || colNames[i].equals("MOD_TIME") || colNames[i].equals("MOD_DATETIME") ||
//					colNames[i].equals("CREATETIME") || colNames[i].equals("LASTUPDATETIME") ||
//					colNames[i].contains("DATETIME") && !colNames[i].contains("INSP") ||
//					colNames[i].equals("CREATE_DATETIME") || colNames[i].equals("ISVALID") || colNames[i].equals("LAST_EVENTTIME") ){
//				sbSql.append("   " + colNames[i] + ", \n");
//								
//			}else{
//							
//				if(insColNames != null) {
//					//컬럼지정값에서 동일한 컬럼만 update 한다.
//					for(int k=0; k<insColNames.length; k++){
//						if(insColNames[k].equals(colNames[i])){
//	
//							sbSql.append("   " + colNames[i]  + ", \n");
//							break;
//						}
//					}
//				}else{				
//					sbSql.append("   " + colNames[i]  + ", \n");
//				}
//			}
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		
		sbSql.append( " \n");
		sbSql.append( " ) VALUES ( \n");
		
		for(int i=0; i < colNames.length; i++){
			
			if(
//					colNames[i].equals("UPD_DATE") || colNames[i].equals("UPD_EMP") || colNames[i].equals("UPD_DATETIME") ||
//					colNames[i].equals("MOD_DATE") || colNames[i].equals("MOD_EMP") || colNames[i].equals("MOD_DATETIME") ||
					colNames[i].equals("DEL_DATE") || colNames[i].equals("DEL_EMP") || colNames[i].equals("DEL_DATETIME") ||
					colNames[i].equals("MODIFIER")){
						continue;
					}
			
			

			
//			//시퀀스
			if(colNames[i].equals(seqColName)){
				if( executeSequenceYn.equals("Y") ) {
					sbSql.append("   " + executeSequenceName + ".NEXTVAL, \n" );
				} else {
					insColNameList.add(colNames[i]);
					sbSql.append("   :" + Util.toCamelCase(colNames[i]) + ",  \n");
				}					
			}else if(colNames[i].equals("INPUT_DATE") || colNames[i].equals("UPD_DATE") || colNames[i].equals("MOD_DATE")){
				sbSql.append("   TO_CHAR(SYSDATE, 'YYYYMMDD'),  \n ");
				
			}else if(colNames[i].equals("INPUT_TIME") || colNames[i].equals("UPD_TIME") || colNames[i].equals("MOD_TIME")){
				sbSql.append("   TO_CHAR(SYSDATE, 'HH24MI'),  \n");
				
			}else if(colNames[i].equals("INPUT_DATETIME") || colNames[i].equals("UPD_DATETIME") || colNames[i].equals("MOD_DATETIME") ||
					colNames[i].equals("CREATETIME") || colNames[i].equals("LASTUPDATETIME") || colNames[i].equals("CREATE_DATETIME") ||
					colNames[i].equals("CHECKOUTTIME") ){
				sbSql.append("   SYSDATE,  \n");
			
			}else if(colNames[i].equals("LAST_EVENTTIME")){
				sbSql.append("   TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF'),  \n");
				
			}else if(colNames[i].equals("ISVALID")){
				sbSql.append("   'Y',  \n");
				
			}else if( colNames[i].equals("INPUT_EMP") || colNames[i].equals("UPD_EMP") || colNames[i].equals("MOD_EMP") ||
					colNames[i].equals("CREATOR") || colNames[i].equals("CREATEUSERID") || colNames[i].equals("LASTUPDATEUSERID") ||
					colNames[i].equals("CHECKOUTUSERID") ){
				if( empNum == null || empNum.equals("")) {
					sbSql.append("   '',  \n");
				} else {
					sbSql.append("   '" + empNum + "',  \n");
				}
											
			} else if( colNames[i].equals(timeKeyColumnName) ) {
				sbSql.append("   TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF6'),  \n \n");
				
			} else {
				
				if(insColNames != null) {
					//컬럼지정값에서 동일한 컬럼만 update 한다.
					for(int k=0; k<insColNames.length; k++){
						if(insColNames[k].equals(colNames[i])){
							insColNameList.add(colNames[i]);
							
							if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
								sbSql.append("   TO_DATE(:" + Util.toCamelCase(colNames[i]) + ", 'YYYYMMDDHH24MISS'),  \n \n");
							} else {							
								sbSql.append("   " + ":" + Util.toCamelCase(colNames[i]) + ", \n");
							}
							break;
						}
					}
				}else{									
					insColNameList.add(colNames[i]);
					if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
						sbSql.append("   TO_DATE(:" + Util.toCamelCase(colNames[i]) + ", 'YYYYMMDDHH24MISS'),  \n \n");
					} else {							
						sbSql.append("   " + ":" + Util.toCamelCase(colNames[i])  + ", \n");
					}
				}
			}
		}
		
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append( " \n");
		
		sbSql.append("\n) \n");
		
		
		return sbSql.toString();
	}
	
	
	/**
	 * UPDATE 쿼리 생성
	 */
	private String makeUpdQuery(String[] colNames, SqlRowSetMetaData meta) { 
		StringBuilder sbSql = new StringBuilder(); //update
		
		
		sbSql.append("UPDATE " + tableName + "\n");
		sbSql.append(" SET " + "\n");
		
		String updYn = "Y";
		
		for(int i=0; i < colNames.length; i++){
			
			if(colNames[i].equals("INPUT_DATE")
					|| colNames[i].equals("INPUT_EMP")
					|| colNames[i].equals("INPUT_DATETIME")
					|| colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME")
					|| colNames[i].equals("CREATOR")
					|| colNames[i].equals("CREATE_DATETIME")
					|| colNames[i].equals("CREATETIME")
					|| colNames[i].equals("CREATEUSERID")
					|| colNames[i].equals("ISVALID")){

				//입력일자를 강제로 업데이트하기 위한 체크
				if( colNames[i].equals("INPUT_DATE") && inputDateUpdateYn.equals("Y") ||
						colNames[i].equals("INPUT_DATETIME") && inputDateUpdateYn.equals("Y") ||
						colNames[i].equals("DEL_DATE") && delDateUpdateYn.equals("Y") ||
						colNames[i].equals("DEL_DATETIME") && delDateUpdateYn.equals("Y") ||
						colNames[i].equals("DEL_EMP") && delDateUpdateYn.equals("Y") ||
						colNames[i].equals("CREATE_DATETIME") && inputDateUpdateYn.equals("Y") ||
						colNames[i].equals("CREATOR") && inputDateUpdateYn.equals("Y") ||
						colNames[i].equals("ISVALID") && delDateUpdateYn.equals("Y")) {
					
				} else {
					continue;
				}
			}
			
			//if(i>0){
			//	sbSql.append(",");
			//}
							
			
			
			if(colNames[i].equals("UPD_DATE") || colNames[i].equals("MOD_DATE") || 
					colNames[i].equals("INPUT_DATE") && inputDateUpdateYn.equals("Y")){
				sbSql.append(colNames[i]  + " = " + " TO_CHAR(SYSDATE, 'YYYYMMDD'), "  + "\n");
			
			} else if(colNames[i].equals("UPD_TIME") || colNames[i].equals("MOD_TIME")){
				sbSql.append(colNames[i]  + " = " + " TO_CHAR(SYSDATE, 'HH24MI'), "  + "\n");

			}else if(colNames[i].equals("UPD_DATETIME") || colNames[i].equals("MOD_DATETIME") || colNames[i].equals("LASTUPDATETIME") ||
					colNames[i].equals("MODIFY_DATETIME") || colNames[i].equals("INPUT_DATETIME") && inputDateUpdateYn.equals("Y") ||
					colNames[i].equals("CHECKOUTTIME") ){
				sbSql.append(colNames[i]  + " = " + " SYSDATE, "  + " \n");

			}else if(colNames[i].equals("LAST_EVENTTIME") ){
				sbSql.append(colNames[i]  + " = " + " TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF'),"  + "\n");
				
			}else if(colNames[i].equals("UPD_EMP") || colNames[i].equals("MOD_EMP")|| colNames[i].equals("MODIFIER") ||
					colNames[i].equals("LASTUPDATEUSERID") || colNames[i].equals("CHECKOUTUSERID") ){
				
				if( empNum == null || empNum.equals("")) {
					sbSql.append(colNames[i]  + " = " + "'" + colNames[i] + "',"  + "\n");
				} else {
					sbSql.append(colNames[i]  + " = " + "'" + empNum + "',"  + "\n");
				}			
			
					
			} else if( colNames[i].equals("DEL_DATE") && delDateUpdateYn.equals("Y") ||
					colNames[i].equals("DEL_DATETIME") && delDateUpdateYn.equals("Y") ||
					colNames[i].equals("DEL_EMP") && delDateUpdateYn.equals("Y")) {
				
				sbSql.append(colNames[i]  + " = " + "NULL,"  + "\n");

				 									
			}else{
				//업데이트 컬럼 지정시
				if(updColNames != null) {
					//컬럼지정값에서 동일한 컬럼만 update 한다.
					for(int k=0; k<updColNames.length; k++){
						if(updColNames[k].equals(colNames[i])){
							updColNameList.add(colNames[i]);
							if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
								sbSql.append(colNames[i]  + " = TO_DATE(:" + Util.toCamelCase(colNames[i]) + ", 'YYYYMMDDHH24MISS') " + ", \n");
							} else {
								sbSql.append(colNames[i]  + " = :" + Util.toCamelCase(colNames[i]) + " " + ", \n");
							}
							break;
						}
					}
				}else{
					
					updYn = "Y";
					if( seqColName != null && seqColName.equals( colNames[i] ) ) {
						updYn = "N";
					}
					
					if( updYn.equals("Y") ) {
						if( pkColNames != null ) {
							for(int k=0; k<pkColNames.length; k++){
								if(pkColNames[k].equals(colNames[i])){
									updYn = "N";
									break;
								}
							}
						}
					}

					if( updYn.equals("Y") ) {
						updColNameList.add(colNames[i]);
						if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
							sbSql.append(colNames[i]  + " = TO_DATE(:" + Util.toCamelCase(colNames[i]) + ", 'YYYYMMDDHH24MISS') " + ", \n");
						} else {							
							sbSql.append(colNames[i]  + " = :" + Util.toCamelCase(colNames[i]) + " " + ", \n");
						}
					}
				}
			}
			
			
								
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append( " \n");
		sbSql.append( "WHERE 1 = 1 \n");
		
		
		if( queryCondition.equals("") || queryCondition.equals("SEQ") ) {
			//시퀀스가 있을 경우
			if(seqColName != null){
				sbSql.append( "  AND "+ seqColName + " = :" + Util.toCamelCase(seqColName) + " "+ "\n");
				updColNameList.add(seqColName);
			}
		}
		
		if( queryCondition.equals("") || queryCondition.equals("PK") ) {
			//PK가 있을 경우
			if(pkColNames != null){
				for(String pkColName : pkColNames){
					
					if( pkColName.equals("DEL_DATE") || pkColName.equals("DEL_DATETIME") ) {
						sbSql.append( "  AND "+ pkColName+ " IS NULL "+ "\n");
					} else {						
						sbSql.append( "  AND "+ pkColName+ " = :" + Util.toCamelCase(pkColName) + " "+ "\n");
						updColNameList.add(pkColName);
					}
				}
			}
		}
		
		return sbSql.toString();

	}
	
	
	/**
	 * DELETE 쿼리 생성
	 */
	private String makeDelQuery(String[] colNames, SqlRowSetMetaData meta) {
		StringBuilder sbSql = new StringBuilder(); //update
		
		sbSql.append("UPDATE " + tableName + "  \n");
		sbSql.append("SET " + "\n");
		
		for(int i=0; i < colNames.length; i++){
			if(colNames[i].equals("DEL_DATE")){
				sbSql.append(colNames[i] + " = " + " TO_CHAR(SYSDATE, 'YYYYMMDD'),"  + "\n");

			}else if(colNames[i].equals("DEL_TIME")){
				sbSql.append(colNames[i] + " = " + " TO_CHAR(SYSDATE, 'HH24MI'),"  + "\n");

			}else if(colNames[i].equals("DEL_DATETIME") || colNames[i].equals("MODIFY_DATETIME")){
				sbSql.append(colNames[i] + " = " + " SYSDATE,"  + "\n");

			}else if(colNames[i].equals("LAST_EVENTTIME")){
				sbSql.append(colNames[i] + " = " + " TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF'),"  + "\n");

			}else if(colNames[i].equals("ISVALID")){
				sbSql.append(colNames[i] + " = " + " 'N',"  + "\n");

			}else if(colNames[i].equals("DEL_EMP") || colNames[i].equals("MODIFIER")){
				if( empNum == null || empNum.equals("")) {

				} else {
					sbSql.append(colNames[i] + " = '" + empNum + "', \n");
				}
								
								
			} else if(delUpdColNames != null) {
				//컬럼지정값에서 동일한 컬럼만 update 한다.
				for(int k=0; k< delUpdColNames.length; k++){
					if(delUpdColNames[k].equals(colNames[i])){
						delColNameList.add(colNames[i]);
						if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
							sbSql.append(colNames[i]  + " = TO_DATE(:" + Util.toCamelCase(colNames[i]) + ", 'YYYYMMDDHH24MISS'), " + "\n");
						} else {
							sbSql.append(colNames[i]  + " = :" + Util.toCamelCase(colNames[i]) + ", " + "\n");
						}
						break;
					}
				}
			}
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append( " \n");
		sbSql.append( "WHERE 1=1 \n");
		
					
		if( queryCondition.equals("") || queryCondition.equals("SEQ") ) {
			//시퀀스가 있을 경우
			if(seqColName != null){
				sbSql.append( "  AND "+ seqColName + " = :" + Util.toCamelCase(seqColName) + " "+ "\n");
				delColNameList.add(seqColName);
			}
		}
		
		if( queryCondition.equals("") || queryCondition.equals("PK") ) {
			//PK가 있을 경우
			if(pkColNames != null){
				for(String pkColName : pkColNames){
					if( pkColName.equals("DEL_DATE") || pkColName.equals("DEL_DATETIME") ) {
						sbSql.append( "  AND "+ pkColName+ " IS NULL "+ "\n");
					} else {
						sbSql.append( "  AND "+ pkColName+ " = :" + Util.toCamelCase(pkColName) + " "+ "\n");
						delColNameList.add(pkColName);
					}
				}
			}
		}
		
		return sbSql.toString();
	}
	
	/**
	 * DELETE REAL 쿼리 생성
	 */
	private String makeDelRealQuery(String[] colNames, SqlRowSetMetaData meta) {
		StringBuilder sbSql = new StringBuilder();
		
		sbSql.append("DELETE FROM " + tableName + " " + "\n");
		sbSql.append( " WHERE 1=1 \n");
		
		if( queryCondition.equals("") || queryCondition.equals("SEQ") ) {
			//시퀀스가 있을 경우
			if(seqColName != null){
				sbSql.append( "  AND "+ seqColName + " = :" + Util.toCamelCase(seqColName) + " "+ "\n");
				delRealColNameList.add(seqColName);
			}
		}
		
		
		if( queryCondition.equals("") || queryCondition.equals("PK") ) {
			//PK가 있을 경우
			if(pkColNames != null){
				for(String pkColName : pkColNames){
					sbSql.append( "  AND "+ pkColName+ " = :" + Util.toCamelCase(pkColName) + " "+ "\n");
					delRealColNameList.add(pkColName);
				}
			}
		}
		
		return sbSql.toString();
	}
	
	/**
	 * SELECT INSERT 쿼리 생성
	 */
	private String makeSelInsQuery(String[] colNames, SqlRowSetMetaData meta) {
		StringBuilder sbSql = new StringBuilder();
		sbSql.append("INSERT INTO " + tableName + " ( " + "\n");
		
		for(int i=0; i < colNames.length; i++){
			
			if(colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME") ){
				continue;
			}
			
			//if(i>0){
			//	sbSql.append(",");
			//}
			sbSql.append("["+ colNames[i] + "] \n");	
			sbSql.append(",");
		}

		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append( " \n");
		
		sbSql.append( ") \n");
		sbSql.append( "SELECT \n");
		
		for(int i=0; i < colNames.length; i++){
			
			if(colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME") ){
				continue;
			}
			
			//if(i>0){
			//	sbSql.append(",");
			//}

			//시퀀스
			if(colNames[i].equals(seqColName)){
				if( executeSequenceYn.equals("Y") ) {
					
					sbSql.append("   " + executeSequenceName + ".NEXTVAL, \n" );
					
				} else {					
					sbSql.append("   :" + Util.toCamelCase(colNames[i]) + ",  \n");
					selInsColNameList.add(colNames[i]);
				}		
									
			}else if(colNames[i].equals("INPUT_DATE")){
				sbSql.append(" TO_CHAR(SYSDATE, 'YYYYMMDD'),  \n");
				
			}else if(colNames[i].equals("INPUT_TIME")){
				sbSql.append(" TO_CHAR(SYSDATE, 'HH24MI'),  \n");
				
			}else if(colNames[i].equals("INPUT_DATETIME")){
				sbSql.append(" SYSDATE,  \n");

			}else if( colNames[i].equals("INPUT_EMP") || colNames[i].equals("UPD_EMP") || colNames[i].equals("MOD_EMP") ||
					colNames[i].equals("CREATOR") || colNames[i].equals("CREATEUSERID") || colNames[i].equals("LASTUPDATEUSERID") ){
				
				if( empNum == null || empNum.equals("")) {
					sbSql.append(" '',  \n");
				} else {
					sbSql.append(" '" + empNum + "',  \n");
				}
				
			}else{
				sbSql.append(" " + colNames[i] + ",  \n");
			}
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		
		sbSql.append( " \n");
		
		sbSql.append( "FROM " + tableName + " \n");
		sbSql.append( "WHERE 1=1 \n");
					
		if( queryCondition.equals("") || queryCondition.equals("SEQ") ) {
			//시퀀스가 있을 경우
			if(seqColName != null){
				sbSql.append( "  AND "+ seqColName + " = :" + Util.toCamelCase(seqColName) + " "+ "\n");
				selInsColNameList.add(seqColName);
			}
		}
		
		if( queryCondition.equals("") || queryCondition.equals("PK") ) {
			//PK가 있을 경우
			if(pkColNames != null){
				for(String pkColName : pkColNames){
					sbSql.append( "  AND "+ pkColName+ " = :" + Util.toCamelCase(pkColName) + " "+ "\n");
					selInsColNameList.add(pkColName);
				}
			}
		}

		return sbSql.toString();
	}
	
	/**
	 * MERGE INTO 쿼리 생성
	 */
	private String makeMergeIntoQuery(String[] colNames, SqlRowSetMetaData meta) {
		StringBuilder sbSql = new StringBuilder();
		sbSql.append("\n");
		sbSql.append("MERGE INTO " + tableName + " A " + "\n");
		sbSql.append("USING ( \n");
		sbSql.append("       SELECT  \n");
		for(int i=0; i < colNames.length; i++){
			
			if(colNames[i].equals("INPUT_DATE")
					|| colNames[i].equals("INPUT_EMP")
					|| colNames[i].equals("INPUT_DATETIME")
					|| colNames[i].equals("UPD_DATE")
					|| colNames[i].equals("UPD_EMP")
					|| colNames[i].equals("UPD_DATETIME")
					|| colNames[i].equals("MOD_DATE")
					|| colNames[i].equals("MOD_EMP")
					|| colNames[i].equals("MOD_DATETIME")
					|| colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME") ){
				continue;
			}
			
			if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
				sbSql.append("              TO_DATE(:" + Util.toCamelCase(colNames[i]) + ", 'YYYYMMDDHH24MISS') AS " + colNames[i] + ", \n");
				 
			} else {
			
				sbSql.append("              :" + Util.toCamelCase(colNames[i]) + " AS "+ colNames[i] + ",\n");
			}
			mergeIntoColNameList.add(colNames[i]);
		}

		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append(" \n");
		sbSql.append("       FROM DUAL \n");
		
		sbSql.append(" \n");
		sbSql.append(") B  \n");
		sbSql.append("       ON ( 1 = 1 \n");
		if( mergeIntoOnColNames != null ) {
			for(int k = 0; k < mergeIntoOnColNames.length; k++) {
				if( mergeIntoOnColNames[k].equals("DEL_DATE") || mergeIntoOnColNames[k].equals("DEL_DATETIME") ) {
					sbSql.append("       AND A." + mergeIntoOnColNames[k]  + " IS NULL  \n");
				} else {
					sbSql.append("       AND B." + mergeIntoOnColNames[k]  + " = A." + mergeIntoOnColNames[k] + " \n");
				}
			}
		} else {
			if( pkColNames != null ) {				
				for(int k = 0; k < pkColNames.length; k++) {				
					if( pkColNames[k].equals("DEL_DATE") || pkColNames[k].equals("DEL_DATETIME") ) {
						sbSql.append("       AND A." + pkColNames[k]  + " IS NULL  \n");
					} else {
						sbSql.append("       AND B." + pkColNames[k]  + " = A." + pkColNames[k] + " \n");
					}
				}
			}
		}
		sbSql.append("       ) \n");
		sbSql.append(" \n");
		sbSql.append("WHEN MATCHED THEN   \n");
		sbSql.append("     UPDATE SET \n");
		
		for(int i=0; i < colNames.length; i++){
			
			if(colNames[i].equals("INPUT_DATE")
					|| colNames[i].equals("INPUT_EMP")
					|| colNames[i].equals("INPUT_DATETIME")
					|| colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME") ){
				
				//입력일자를 강제로 업데이트하기 위한 체크
				if( colNames[i].equals("INPUT_DATE")  && inputDateUpdateYn.equals("Y") ) {
					
				}else if(colNames[i].equals("INPUT_DATETIME") && inputDateUpdateYn.equals("Y") ) {
					
					
				} else if( colNames[i].equals("DEL_DATE") && delDateUpdateYn.equals("Y") ||
						colNames[i].equals("DEL_DATETIME") && delDateUpdateYn.equals("Y") ||
						colNames[i].equals("DEL_EMP") && delDateUpdateYn.equals("Y")) {
					

				} else {
					continue;
				}
			}
			
			//if(i>0){
			//	sbSql.append(",");
			//}
							
			String lowColName = colNames[i].replaceAll("_", "").toLowerCase();
			
			if(colNames[i].equals("UPD_DATE") || colNames[i].equals("MOD_DATE") ||
					colNames[i].equals("INPUT_DATE")  && inputDateUpdateYn.equals("Y") ){
				sbSql.append("          A." + colNames[i] + " = " + " TO_CHAR(SYSDATE, 'YYYYMMDD'),\n");
				
			}else if(colNames[i].equals("UPD_TIME") || colNames[i].equals("MOD_TIME")){
				sbSql.append("          A." + colNames[i] + " = " + " TO_CHAR(SYSDATE, 'HH24MI'),\n");
				
			}else if(colNames[i].equals("UPD_DATETIME") || colNames[i].equals("MOD_DATETIME") || 
					colNames[i].equals("LASTUPDATETIME") || colNames[i].equals("MODIFY_DATETIME") ||
					colNames[i].equals("INPUT_DATETIME") && inputDateUpdateYn.equals("Y") ||
					colNames[i].equals("CHECKOUTTIME") ){					
				sbSql.append("          A." + colNames[i] + " = " + " SYSDATE,\n");
				
			}else if(colNames[i].equals("UPD_EMP") || colNames[i].equals("MOD_EMP") || colNames[i].equals("MODIFIER")
					 || colNames[i].equals("LASTUPDATEUSERID") || colNames[i].equals("CHECKOUTUSERID") ){
				if( empNum == null || empNum.equals("")) {

				} else {
					sbSql.append("          A." + colNames[i] + " = '" + empNum + "',  \n");
				}
				
					
			} else if( colNames[i].equals("DEL_DATE") && delDateUpdateYn.equals("Y") ||
					colNames[i].equals("DEL_DATETIME") && delDateUpdateYn.equals("Y") ||
					colNames[i].equals("DEL_EMP") && delDateUpdateYn.equals("Y")) {
						
				sbSql.append("          A." + colNames[i]  + " = " + "NULL,"  + "\n");
								
			}else{
				//업데이트 컬럼 지정시
				if(updColNames != null) {
					//컬럼지정값에서 동일한 컬럼만 update 한다.
					for(int k=0; k<updColNames.length; k++){
						if(updColNames[k].equals(colNames[i])){									
							sbSql.append("          A." + colNames[i]  + " = B." + colNames[i]  + ",\n");
							break;
						}
					}
				}else{
					if( pkColNames != null ) {				
						boolean isPk = false;
						if( mergeIntoOnColNames != null ) {
							for(int k = 0; k < mergeIntoOnColNames.length; k++) {				
								if( colNames[i].equals( mergeIntoOnColNames[k] ) ) {
									isPk = true;
								}
							}
						}
						if( pkColNames != null ) {
							for(int k = 0; k < pkColNames.length; k++) {				
								if( colNames[i].equals( pkColNames[k] ) ) {
									isPk = true;
								}
							}
						}
						if( isPk == false ) {
							
							if( seqColName != null && seqColName.equals( colNames[i] ) ) {
								
							} else {								
								sbSql.append("          A." + colNames[i]  + " = B." + colNames[i]  + ",\n");
							}
						}
						
						
					} else {
						if( seqColName != null && seqColName.equals( colNames[i] ) ) {
							
						} else {
							sbSql.append("          A." + colNames[i]  + " = B." + colNames[i]  + ",\n");
						}
					}
				}
			}
			
			
								
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append(" \n");
		sbSql.append(" \n");
		sbSql.append("WHEN NOT MATCHED THEN   \n");
		sbSql.append("     INSERT (  \n");
		
		for(int i=0; i < colNames.length; i++){
			
			if( colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME") ){
				continue;
			}
			
			//if(i>0){
			//	sbSql.append(",");
			//}
			
			if(colNames[i].equals(seqColName) || colNames[i].equals("INPUT_DATE") || colNames[i].equals("MOD_DATE") || 
					colNames[i].equals("UPD_DATE") || colNames[i].equals("INPUT_TIME") || colNames[i].equals("MOD_TIME") || 
					colNames[i].equals("UPD_TIME") || colNames[i].equals("INPUT_EMP") || colNames[i].equals("MOD_EMP")|| 
					colNames[i].equals("UPD_EMP") || colNames[i].equals("CREATOR") || colNames[i].equals("CREATEUSERID") || 
					colNames[i].equals("LASTUPDATEUSERID") || colNames[i].equals("INPUT_DATETIME") || colNames[i].equals("MOD_DATETIME")|| 
					colNames[i].equals("UPD_DATETIME") || colNames[i].equals("LASTUPDATETIME") || colNames[i].equals("MODIFY_DATETIME") ||
					colNames[i].equals("CHECKOUTTIME") || colNames[i].equals("CHECKOUTUSERID") ) {
						
				//무조건 입력
				sbSql.append("          " + colNames[i] + ", \n");
								
			}else{
			
				if(insColNames != null) {
					//컬럼지정값에서 동일한 컬럼만 update 한다.
					for(int k=0; k<insColNames.length; k++){
						if(insColNames[k].equals(colNames[i])){

							sbSql.append("          "+ colNames[i] + ",\n");
							break;
						}
					}
				}else{				
					sbSql.append("          "+ colNames[i] + ",\n");
				}
			}
								
		}

		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		
		sbSql.append( "\n     ) VALUES ( \n");
		
		for(int i=0; i < colNames.length; i++){
			
			if(colNames[i].equals("DEL_DATE") 
					|| colNames[i].equals("DEL_EMP")
					|| colNames[i].equals("DEL_DATETIME") ){
				continue;
			}
			
			//if(i>0){
			//	sbSql.append(",");
			//}
			
			//시퀀스
			if(colNames[i].equals(seqColName)){
				if( executeSequenceYn.equals("Y") ) {
					
					sbSql.append("   " + executeSequenceName + ".NEXTVAL, \n" );
					
				} else {					
					sbSql.append("   :" + Util.toCamelCase(colNames[i]) + ",  \n");
					mergeIntoColNameList.add(colNames[i]);
				}	
			}else if(colNames[i].equals("INPUT_DATE") || colNames[i].equals("MOD_DATE") || colNames[i].equals("UPD_DATE")){
				sbSql.append("          TO_CHAR(SYSDATE, 'YYYYMMDD'), \n");
				
			}else if(colNames[i].equals("INPUT_TIME") || colNames[i].equals("MOD_TIME") || colNames[i].equals("UPD_TIME")){
				sbSql.append("          TO_CHAR(SYSDATE, 'HH24MI'), \n");
				
			}else if(colNames[i].equals("INPUT_EMP") || colNames[i].equals("MOD_EMP")|| colNames[i].equals("UPD_EMP") ||
					colNames[i].equals("CREATOR") || colNames[i].equals("CREATEUSERID") || colNames[i].equals("LASTUPDATEUSERID") ||
					colNames[i].equals("CHECKOUTUSERID") ){
				
				if( empNum == null || empNum.equals("")) {
					sbSql.append("         '', \n");
				} else {
					sbSql.append("         '" + empNum + "', \n");
				}
										
			}else if(colNames[i].equals("INPUT_DATETIME") || colNames[i].equals("MOD_DATETIME")|| 
					colNames[i].equals("UPD_DATETIME") || colNames[i].equals("LASTUPDATETIME") || 
					colNames[i].equals("CREATE_DATETIME") || colNames[i].equals("CREATETIME") || 
					colNames[i].equals("CHECKOUTTIME") ){
				sbSql.append("          SYSDATE, \n");
				
			}else if(colNames[i].equals("LAST_EVENTTIME")){
				sbSql.append("          TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF'),  \n");

			} else if( colNames[i].equals(timeKeyColumnName) ) {
				sbSql.append("          TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF6'),  \n \n");

			}else{
				
				if(insColNames != null) {
					//컬럼지정값에서 동일한 컬럼만 update 한다.
					for(int k=0; k<insColNames.length; k++){
						if(insColNames[k].equals(colNames[i])){
							sbSql.append("          B." + colNames[i] + ", \n");
							break;
						}
					}
				}else{				
					sbSql.append("          B." + colNames[i] + ", \n");
				}					
				
			}
			
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		
		sbSql.append( "\n     ) \n");
		sbSql.append( " \n");
		
		return sbSql.toString();
	}
	
	/**
	 * SELECT 쿼리 생성
	 */
	private String makeSelQuery(String[] colNames, SqlRowSetMetaData meta) {
		StringBuilder sbSql = new StringBuilder();
		sbSql.append( " \n");
		sbSql.append( "SELECT  \n");
		
		for(int i=0; i < colNames.length; i++){
			if( meta.getColumnTypeName(i + 1).equals("DATE") ) {
				sbSql.append("              TO_CHAR(" + colNames[i] + ", 'YYYYMMDDHH24MISS') AS " + colNames[i] + ", \n");
				 
			} else {
			
				sbSql.append("              " + colNames[i] + ", \n");
			}
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		sbSql.append( " \n");
		sbSql.append( "FROM " + tableName + " \n");
		sbSql.append( "WHERE 1 = 1 \n");
					
		if( queryCondition.equals("") || queryCondition.equals("SEQ") ) {
			//시퀀스가 있을 경우
			if(seqColName != null){
				sbSql.append( "  AND "+ seqColName + " = :" + Util.toCamelCase(seqColName) + " "+ "\n");
				selectColNameList.add(seqColName);
			}
		}
		
		if( queryCondition.equals("") || queryCondition.equals("PK") ) {
			//PK가 있을 경우
			if(pkColNames != null){
				for(String pkColName : pkColNames){
					
					if( pkColName.equals("DEL_DATE") || pkColName.equals("DEL_DATETIME") ) {
						sbSql.append("AND " + pkColName + " IS NULL  \n");
					} else {
						sbSql.append("AND " + pkColName + " = :" + Util.toCamelCase(pkColName) + " \n");
					}
					
					
					selectColNameList.add(pkColName);
				}
			}
		}
		
		//2019.07.12 DWKIM ROWNU 추가 한건만 가져오도록
		if ( queryCondition.equals("ROWNUM") )
		{
			sbSql.append("AND ROWNUM = 1 \n");
		}
		
		if( orderByColNames != null ) {
			sbSql.append("ORDER BY  \n");
						
			Iterator iterator = orderByColNames.entrySet().iterator();
			while(iterator.hasNext())
			{
				Entry entry = (Entry)iterator.next();   
				
				sbSql.append("        " + entry.getKey().toString() + " " + entry.getValue().toString() + " \n");
				
			}
		}
		
		return sbSql.toString();

	}
	
	
	/**
	 * INSERT HISTORY 쿼리 생성
	 * @param metaHist 
	 * @param colNamesHist 
	 * @throws Exception 
	 */
	private String makeInsHistQuery(String[] colNamesHist, String[] colNames) throws Exception {
		StringBuilder sbSql = new StringBuilder();
				
		sbSql.append("INSERT INTO " + historyTableName + " ( " + "\n");
		
		for(int i=0; i < colNamesHist.length; i++){
			if(colNamesHist[i].equals("TIMEKEY") || colNamesHist[i].equals("TIME_KEY")){
				sbSql.append("         "+ colNamesHist[i] + ", \n");
									
			}else if(colNamesHist[i].equals("EVENTTIME") || colNamesHist[i].equals("EVENT_DATETIME")){				
				sbSql.append("         "+ colNamesHist[i] + ", \n");
								
			}else if(colNamesHist[i].equals("EVENTNAME") ){				
				sbSql.append("         "+ colNamesHist[i] + ", \n");
				
			}else if(colNamesHist[i].equals("COMPONENT_NAME") ){
				sbSql.append("         "+ colNamesHist[i] + ", \n");
				
			}else if(colNamesHist[i].equals("SERVICE_NAME") ){
				sbSql.append("         "+ colNamesHist[i] + ", \n");
			
			}else if(colNamesHist[i].equals("EVENTTYPE") || colNamesHist[i].equals("EVENT_TYPE")){
				sbSql.append("         "+ colNamesHist[i] + ", \n");
				
			}else if(colNamesHist[i].equals("EVENTUSERID") || colNamesHist[i].equals("EVENT_EMP")){
				sbSql.append("         "+ colNamesHist[i] + ", \n");
								
			}else if(colNamesHist[i].equals("EVENTCOMMENT")){
				sbSql.append("         "+ colNamesHist[i] + ", \n");
			} else {
				
				//컬럼이 있는 경우에만 쿼리 생성함
				for( int j = 0; j < colNames.length; j++ ) {
					if( colNames[j].equals( colNamesHist[i] ) ) {
						sbSql.append("         "+ colNamesHist[i] + ", \n");
						break;
					}
				}
			}
			
		}

		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		
		sbSql.append( ") \n");
		sbSql.append( " \n");
		sbSql.append( "SELECT \n");
		
		
		
		for(int i=0; i < colNamesHist.length; i++){
			//시퀀스
			if(colNamesHist[i].equals("TIMEKEY") || colNamesHist[i].equals("TIME_KEY")){
				sbSql.append("         TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF6'), \n");
									
			}else if(colNamesHist[i].equals("EVENTTIME") || colNamesHist[i].equals("EVENT_DATETIME")){				
				sbSql.append("         SYSDATE,  \n");
								
			}else if(colNamesHist[i].equals("EVENTNAME") ){				
				sbSql.append("        '" + serviceName + " " + methodName + "',  \n");
				
			}else if(colNamesHist[i].equals("COMPONENT_NAME") ){
				sbSql.append("        '" + serviceName+ "',  \n");
				
			}else if(colNamesHist[i].equals("SERVICE_NAME") ){
				sbSql.append("        '" + methodName + "',  \n");
			
			}else if(colNamesHist[i].equals("EVENTTYPE") || colNamesHist[i].equals("EVENT_TYPE")){
				sbSql.append("         :" + Util.toCamelCase(colNamesHist[i]) + ",  \n");
				insHistColNameList.add("EVENT_TYPE");
				
			}else if(colNamesHist[i].equals("EVENTUSERID") || colNamesHist[i].equals("EVENT_EMP")){
				if( empNum == null || empNum.equals("")) {
					sbSql.append("        '',  \n");
				} else {
					sbSql.append("        '" + empNum + "',  \n");
				}
					
			}else if(colNamesHist[i].equals("EVENTCOMMENT")){
				sbSql.append("         '',  \n");			
			}else{
				
				//컬럼이 있는 경우에만 쿼리 생성함
				for( int j = 0; j < colNames.length; j++ ) {
					if( colNames[j].equals( colNamesHist[i] ) ) {
						sbSql.append("         " + colNamesHist[i] + ", \n");
						break;
					}
				}
				
				
			}
			
		}
		
		//마지막에 ,콤막일 경우 제거
		if(sbSql.toString().trim().endsWith(",")){
			sbSql.delete(sbSql.lastIndexOf(","), sbSql.length());
		}
		
		sbSql.append( " \n");
		
		sbSql.append( "FROM " + tableName + " \n");
		sbSql.append( "WHERE 1 = 1 \n");
		
		boolean conditionAdd = false;
					
		if( queryCondition.equals("") || queryCondition.equals("SEQ") ) {
			//시퀀스가 있을 경우
			if(seqColName != null){
				sbSql.append( "AND "+ seqColName + " = :" + Util.toCamelCase(seqColName) + " "+ "\n");
				insHistColNameList.add(seqColName);
				
				conditionAdd = true;
			}
		}
		
		if( queryCondition.equals("") || queryCondition.equals("PK") ) {
			//PK가 있을 경우
			if(pkColNames != null){
				for(String pkColName : pkColNames){
					sbSql.append( "AND "+ pkColName+ " = :" + Util.toCamelCase(pkColName) + " "+ "\n");
					insHistColNameList.add(pkColName);
				}
				
				conditionAdd = true;
			}
		}
		
		if( conditionAdd == false ) {
			throw new Exception("History 테이블 입력 쿼리 생성시 조건이 추가되지 않았습니다." + historyTableName);
		}

		return sbSql.toString();
	}
	
	
	public HashMap<String, Object> convertColumnText(String colName, String eventType, String option, String columnTypeName) {
		HashMap<String, Object> rtnHashMap = new HashMap<String, Object>();
		String columnText = "";
		String addList = "N";
		
		if( option.equals("TARGET") ) {
		
			if(colName.equals(seqColName)){
				columnText = colName;		
			}else if(colName.equals("INPUT_DATE") || colName.equals("UPD_DATE") || colName.equals("MOD_DATE")){
				columnText = colName;
				
			}else if(colName.equals("INPUT_TIME") || colName.equals("UPD_TIME") || colName.equals("MOD_TIME")){
				columnText = colName;
				
			}else if(colName.equals("INPUT_DATETIME") || colName.equals("UPD_DATETIME") || colName.equals("MOD_DATETIME") ||
					colName.equals("CREATETIME") || colName.equals("LASTUPDATETIME") || colName.equals("CREATE_DATETIME") ){
				columnText = colName;
			
			}else if(colName.equals("LAST_EVENTTIME")){
				columnText = colName;
				
			}else if(colName.equals("ISVALID")){
				columnText = colName;
				
			}else if( colName.equals("INPUT_EMP") || colName.equals("UPD_EMP") || colName.equals("MOD_EMP") ||
					colName.equals("CREATOR") || colName.equals("CREATEUSERID") || colName.equals("LASTUPDATEUSERID") ){
				columnText = colName;
							
			} else if( columnTypeName.equals("DATE") ) {
				columnText = colName;
				
			} else if( colName.equals(timeKeyColumnName) ) {
				columnText = colName;
			}
			
		} else {
			
			if(colName.equals(seqColName)){
				if( executeSequenceYn.equals("Y") ) {
					
					columnText = executeSequenceName + ".NEXTVAL";
					
				} else {
					addList = "Y";
					columnText = ":" + colName;
				}					
			}else if(colName.equals("INPUT_DATE") || colName.equals("UPD_DATE") || colName.equals("MOD_DATE")){
				columnText = "TO_CHAR(SYSDATE, 'YYYYMMDD')";
				
			}else if(colName.equals("INPUT_TIME") || colName.equals("UPD_TIME") || colName.equals("MOD_TIME")){
				columnText = "TO_CHAR(SYSDATE, 'HH24MI')";
				
			}else if(colName.equals("INPUT_DATETIME") || colName.equals("UPD_DATETIME") || colName.equals("MOD_DATETIME") ||
					colName.equals("CREATETIME") || colName.equals("LASTUPDATETIME") || colName.equals("CREATE_DATETIME") ){
				columnText = "SYSDATE";
			
			}else if(colName.equals("LAST_EVENTTIME")){
				columnText = "TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF')";
				
			}else if(colName.equals("ISVALID")){
				columnText = "'Y'";
				
			}else if( colName.equals("INPUT_EMP") || colName.equals("UPD_EMP") || colName.equals("MOD_EMP") ||
					colName.equals("CREATOR") || colName.equals("CREATEUSERID") || colName.equals("LASTUPDATEUSERID") ){
				if( empNum == null || empNum.equals("")) {
					columnText = "''";
				} else {
					columnText = "'" + empNum + "'";
				}
							
			} else if( columnTypeName.equals("DATE") ) {
				columnText = "TO_DATE(:" + colName + ", 'YYYYMMDDHH24MISS')";
				addList = "Y";
				
			} else if( colName.equals(timeKeyColumnName) ) {
				columnText = "TO_CHAR(SYSTIMESTAMP, 'YYYYMMDDHH24MISSFF6')";
			}
		}
		
		rtnHashMap.put("COLUMN_TEXT", columnText);
		rtnHashMap.put("ADD_LIST", addList);
		
		
		return rtnHashMap;
	}
	
	/**
	 * 상태가 INSERT 인 데이터를 INSERT 한다.
	 * @return
	 * @throws Exception
	 */
	public int insert() throws Exception {
		
		genStrQuery();
		
		int count = 0;
		
		if( taskMode.equals("Dto") ) {
			//count += utilDDAO.batchUpdate(insDtoList, insColNameList, strInsQuery, common, logPrintYn, "INSERT");
			
			//HISTORY 테이블 INSERT
			//if( historyTableName != null ) {
			//	count += utilDDAO.batchUpdate(insDtoList, insHistColNameList, strInsHistQuery, common, logPrintYn, "INSERT");
			//}
		} else if( taskMode.equals("HashMap") ) {
			//count += utilDDAO.batchUpdateHashMap(insHashMapList, insColNameList, strInsQuery, common, logPrintYn, "INSERT");
			
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsQuery, insHashMapList, SqlQuery.UPD_TYPE_INSERT);
			
			//HISTORY 테이블 INSERT
			if( historyTableName != null ) {
				//count += utilDDAO.batchUpdateHashMap(insHashMapList, insHistColNameList, strInsHistQuery, common, logPrintYn, "INSERT");
				count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsHistQuery, insHashMapList, SqlQuery.UPD_TYPE_INSERT);
			}
		}
		
		return count;
	}
	

	/**
	 * 상태가 UPDATE 인 데이터를 UPDATE 한다.
	 * @return
	 * @throws Exception
	 */
	public int update() throws Exception {
		
		genStrQuery();
		
		int count = 0;
		if( taskMode.equals("Dto") ) {
			//count += utilDDAO.batchUpdate(updDtoList, updColNameList, strUpdQuery, common, logPrintYn, "UPDATE");
			
			//HISTORY 테이블 INSERT
			//if( historyTableName != null ) {
			//	count += utilDDAO.batchUpdate(updDtoList, insHistColNameList, strInsHistQuery, common, logPrintYn, "UPDATE");
			//}
		} else if( taskMode.equals("HashMap") ) {
			//count += utilDDAO.batchUpdateHashMap(updHashMapList, updColNameList, strUpdQuery, common, logPrintYn, "UPDATE");
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strUpdQuery, updHashMapList, SqlQuery.UPD_TYPE_UPDATE);
			
			//HISTORY 테이블 INSERT
			if( historyTableName != null ) {
				//count += utilDDAO.batchUpdateHashMap(updHashMapList, insHistColNameList, strInsHistQuery, common, logPrintYn, "UPDATE");
				count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsHistQuery, updHashMapList, SqlQuery.UPD_TYPE_UPDATE);
			}
		}
		
		return count;
	}
	
	/**
	 * 상태가 UPDATE 값을 INSERT 후 DELETE 한다.
	 * @return
	 * @throws Exception
	 */
	public int insertDelete() throws Exception {
		
		genStrQuery();
		
		int count = 0;
		if( taskMode.equals("Dto") ) {
			//count += utilDDAO.batchUpdate(updDtoList, updColNameList, strUpdQuery, common, logPrintYn, "UPDATE");
			
			//HISTORY 테이블 INSERT
			//if( historyTableName != null ) {
			//	count += utilDDAO.batchUpdate(updDtoList, insHistColNameList, strInsHistQuery, common, logPrintYn, "UPDATE");
			//}
		} else if( taskMode.equals("HashMap") ) {
			//count += utilDDAO.batchUpdateHashMap(updHashMapList, updColNameList, strUpdQuery, common, logPrintYn, "UPDATE");
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strDelQuery, updHashMapList, SqlQuery.UPD_TYPE_UPDATE);
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsQuery, updHashMapList, SqlQuery.UPD_TYPE_UPDATE);
			
			//HISTORY 테이블 INSERT
			if( historyTableName != null ) {
				//count += utilDDAO.batchUpdateHashMap(updHashMapList, insHistColNameList, strInsHistQuery, common, logPrintYn, "UPDATE");
				count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsHistQuery, updHashMapList, SqlQuery.UPD_TYPE_UPDATE);
				
			}
		}
		
		return count;
	}

	/**
	 * 상태가 DELETE 인 경우 DEL_DATETIME 을 UPDATE 한다.
	 * @return
	 * @throws Exception
	 */
	public int delete() throws Exception {
		
		genStrQuery();

		int count = 0;
		
		if( taskMode.equals("Dto") ) {
			//count += utilDDAO.batchUpdate(delDtoList, delColNameList, strDelQuery, common, logPrintYn, "DELETE");
			//HISTORY 테이블 INSERT
			//if( historyTableName != null ) {
			//	count += utilDDAO.batchUpdate(delDtoList, insHistColNameList, strInsHistQuery, common, logPrintYn, "DELETE");
			//}
		} else if( taskMode.equals("HashMap") ) {
			//count += utilDDAO.batchUpdateHashMap(delHashMapList, delColNameList, strDelQuery, common, logPrintYn, "DELETE");
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strDelQuery, delHashMapList, SqlQuery.UPD_TYPE_DELETE);
			
			//HISTORY 테이블 INSERT
			if( historyTableName != null ) {
				//count += utilDDAO.batchUpdateHashMap(delHashMapList, insHistColNameList, strInsHistQuery, common, logPrintYn, "DELETE");
				count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsHistQuery, delHashMapList, SqlQuery.UPD_TYPE_DELETE);
			}
		} 
		return count;
	}
	
	
	/**
	 * 상태가 DELETE 인 경우 DELETE 한다.
	 * @return
	 * @throws Exception
	 */
	public int deleteReal() throws Exception {
		
		genStrQuery();
		
		int count = 0;
		
		if( taskMode.equals("Dto") ) {
			//HISTORY 테이블 INSERT
			//if( historyTableName != null ) {
			//	count += utilDDAO.batchUpdate(delDtoList, insHistColNameList, strInsHistQuery, common, logPrintYn, "DELETE_REAL");
			//}
			
			//count += utilDDAO.batchUpdate(delDtoList, delRealColNameList, strDelRealQuery, common, logPrintYn, "DELETE_REAL");		
		} else if( taskMode.equals("HashMap") ) {
			//HISTORY 테이블 INSERT
			if( historyTableName != null ) {
				//count += utilDDAO.batchUpdateHashMap(delHashMapList, insHistColNameList, strInsHistQuery, common, logPrintYn, "DELETE_REAL");
				count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsHistQuery, delHashMapList, SqlQuery.UPD_TYPE_DELETE);
			}
			
			//count += utilDDAO.batchUpdateHashMap(delHashMapList, delRealColNameList, strDelRealQuery, common, logPrintYn, "DELETE_REAL");
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strDelRealQuery, delHashMapList, SqlQuery.UPD_TYPE_DELETE);
		}
		return count;
	}
	
	
	/**
	 * 지정한 상태의 값을 MERGE INTO 한다.
	 * @param pType 상태값 : SqlQuery.UPD_TYPE_UPDATE OR SqlQuery.UPD_TYPE_INSERT
	 * @return
	 * @throws Exception
	 */
	public int mergeInto(String pType) throws Exception {
		
		genStrQuery();
	
		int count = 0;
		if( taskMode.equals("Dto") ) {
			//count += utilDDAO.batchUpdate(updDtoList, mergeIntoColNameList, strMergeIntoQuery, common, logPrintYn, "MERGE_INTO_BY_UPDATE");
			
			//HISTORY 테이블 INSERT
			//if( historyTableName != null ) {
			//	count += utilDDAO.batchUpdate(updDtoList, insHistColNameList, strInsHistQuery, common, logPrintYn, "MERGE_INTO_BY_UPDATE");
			//}
		} else if( taskMode.equals("HashMap") ) {
			//count += utilDDAO.batchUpdateHashMap(updHashMapList, mergeIntoColNameList, strMergeIntoQuery, common, logPrintYn, "MERGE_INTO_BY_UPDATE");
			count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strMergeIntoQuery, hashMapList, pType);
			//HISTORY 테이블 INSERT
			if( historyTableName != null ) {
				//count += utilDDAO.batchUpdateHashMap(updHashMapList, insHistColNameList, strInsHistQuery, common, logPrintYn, "MERGE_INTO_BY_UPDATE");
				count +=  sqlQuery.batchUpdateQuery(jdbcTemplate, strInsHistQuery, hashMapList, pType);
			}
		}
		
		return count;
	}
	
	/**
	 * 상태가 UPDATE 인 경우 MERGE INTO 한다.
	 * @return
	 * @throws Exception
	 */
	public int mergeInto() throws Exception {
		
		int count = mergeInto(SqlQuery.UPD_TYPE_UPDATE);

		return count;
	}
	

	/**
	 * 상태가 INSERT 인 경우 MERGE INTO 한다.
	 * @return
	 * @throws Exception
	 */
	public int mergeIntoByIns() throws Exception {
		
		int count = mergeInto(SqlQuery.UPD_TYPE_INSERT);

		return count;
	}
	
	
	public List<Object> getDtoList() {
		return dtoList;
	}

	public void setDtoList(List<Object> dtoList) {
		this.dtoList = dtoList;
	}

	public List<Object> getInsDtoList() {
		return insDtoList;
	}

	public void setInsDtoList(List<Object> insDtoList) {
		this.insDtoList = insDtoList;
	}

	public List<Object> getUpdDtoList() {
		return updDtoList;
	}

	public void setUpdDtoList(List<Object> updDtoList) {
		this.updDtoList = updDtoList;
	}

	public List<Object> getDelDtoList() {
		return delDtoList;
	}

	public void setDelDtoList(List<Object> delDtoList) {
		this.delDtoList = delDtoList;
	}

	public String getStrInsQuery() {
		return strInsQuery;
	}

	public void setStrInsQuery(String strInsQuery) {
		this.strInsQuery = strInsQuery;
	}

	public String getStrUpdQuery() {
		return strUpdQuery;
	}

	public void setStrUpdQuery(String strUpdQuery) {
		this.strUpdQuery = strUpdQuery;
	}

	public String getStrDelQuery() {
		return strDelQuery;
	}

	public void setStrDelQuery(String strDelQuery) {
		this.strDelQuery = strDelQuery;
	}

	public String getStrDelRealQuery() {
		return strDelRealQuery;
	}

	public void setStrDelRealQuery(String strDelRealQuery) {
		this.strDelRealQuery = strDelRealQuery;
	}

	public String getStrSelInsQuery() {
		return strSelInsQuery;
	}

	public void setStrSelInsQuery(String strSelInsQuery) {
		this.strSelInsQuery = strSelInsQuery;
	}

	public String getStrMergeIntoQuery() {
		return strMergeIntoQuery;
	}

	public void setStrMergeIntoQuery(String strMergeIntoQuery) {
		this.strMergeIntoQuery = strMergeIntoQuery;
	}

	public List<String> getSelInsColNameList() {
		return selInsColNameList;
	}

	public void setSelInsColNameList(List<String> selInsColNameList) {
		this.selInsColNameList = selInsColNameList;
	}

	public String getJndiName() {
		return jndiName;
	}

	public void setJndiName(String jndiName) {
		this.jndiName = jndiName;
	}

	public String getTableName() {
		return tableName;
	}

	public void setTableName(String tableName) {
		this.tableName = tableName;
	}

	public String getSeqColName() {
		return seqColName;
	}

	public void setSeqColName(String seqColName) {
		this.seqColName = seqColName;
	}

	public String[] getPkColNames() {
		return pkColNames;
	}

	public void setPkColNames(String[] pkColNames) {
		this.pkColNames = pkColNames;
	}


	public Map<String, String> getDtoFiledMap() {
		return dtoFiledMap;
	}

	public void setDtoFiledMap(Map<String, String> dtoFiledMap) {
		this.dtoFiledMap = dtoFiledMap;
	}

	public String[] getUpdColNames() {
		return updColNames;
	}

	public void setUpdColNames(String[] updColNames) {
		this.updColNames = updColNames;
	}
	
	public void setLogPrintYn(String logPrintYn) {
		this.logPrintYn = logPrintYn;
	}

	public String getInputDateUpdateYn() {
		return inputDateUpdateYn;
	}

	public void setInputDateUpdateYn(String inputDateUpdateYn) {
		this.inputDateUpdateYn = inputDateUpdateYn;
	}
	
	public String getDelDateUpdateYn() {
		return delDateUpdateYn;
	}

	public void setDelDateUpdateYn(String delDateUpdateYn) {
		this.delDateUpdateYn = delDateUpdateYn;
	} 


	public String getLogPrintYn() {
		return logPrintYn;
	}

	public String[] getMergeIntoOnColNames() {
		return mergeIntoOnColNames;
	}

	public void setMergeIntoOnColNames(String[] mergeIntoOnColNames) {
		this.mergeIntoOnColNames = mergeIntoOnColNames;
	}

	public String getQueryCondition() {
		return queryCondition;
	}

	public void setQueryCondition(String queryCondition) {
		this.queryCondition = queryCondition;
	}

	public NamedParameterJdbcTemplate getJbcTemplate() {
		
		return jdbcTemplate;
	}

	public void setJdbcTemplate(NamedParameterJdbcTemplate jbcTemplate) {
		this.jdbcTemplate = jbcTemplate;
	}

	public String getExecuteSequenceYn() {
		return executeSequenceYn;
	}

	public void setExecuteSequenceYn(String executeSequenceYn) {
		this.executeSequenceYn = executeSequenceYn;
	}

	
	public String getDtoNm() {
		return dtoNm;
	}

	public void setDtoNm(String dtoNm) {
		this.dtoNm = dtoNm;
	}


	public Map<String, Object> getSelectParam() {
		return selectParam;
	}


	public void setSelectParam(Map<String, Object> selectParam) {
		this.selectParam = selectParam;
	}


	public String[] getInsColNames() {
		return insColNames;
	}


	public void setInsColNames(String[] insColNames) {
		this.insColNames = insColNames;
	}


	public String[] getDelUpdColNames() {
		return delUpdColNames;
	}


	public void setDelUpdColNames(String[] delUpdColNames) {
		this.delUpdColNames = delUpdColNames;
	}


	public String getDbKind() {
		return dbKind;
	}


	public void setDbKind(String dbKind) {
		this.dbKind = dbKind;
	}


	public String getExecuteSequenceName() {
		return executeSequenceName;
	}


	public void setExecuteSequenceName(String executeSequenceName) {
		this.executeSequenceName = executeSequenceName;
	}


	public String getHistoryTableName() {
		return historyTableName;
	}


	public void setHistoryTableName(String historyTableName) {
		this.historyTableName = historyTableName;
	}


	public String getTimeKeyColumnName() {
		return timeKeyColumnName;
	}


	public void setTimeKeyColumnName(String timeKeyColumnName) {
		this.timeKeyColumnName = timeKeyColumnName;
	}


	public LinkedHashMap<String, String> getOrderByColNames() {
		return orderByColNames;
	}


	public void setOrderByColNames(LinkedHashMap<String, String> orderByColNames) {
		this.orderByColNames = orderByColNames;
	}


	
	
	public List<Map<String, Object>> getHashMapList() {
		return hashMapList;
	}

	public void setHashMapList(List<Map<String, Object>> hashMapList) {
		this.hashMapList = hashMapList;
	}

	public List<Map<String, Object>> getInsHashMapList() {
		return insHashMapList;
	}


	public void setInsHashMapList(List<Map<String, Object>> insHashMapList) {
		this.insHashMapList = insHashMapList;
	}


	public List<Map<String, Object>> getUpdHashMapList() {
		return updHashMapList;
	}


	public void setUpdHashMapList(List<Map<String, Object>> updHashMapList) {
		this.updHashMapList = updHashMapList;
	}


	public List<Map<String, Object>> getDelHashMapList() {
		return delHashMapList;
	}


	public void setDelHashMapList(List<Map<String, Object>> delHashMapList) {
		this.delHashMapList = delHashMapList;
	}	

}
