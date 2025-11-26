package kr.co.scrab.common;

import kr.co.scrab.kccfw.common.Session;

public class SessionUser extends Session {

	public static String EMP = "emp";
	public static String EMP_NM = "empNm";
	public static String EMP_RANK = "empRank";
	public static String USER_ID = "userId";
	public static String DEPT = "dept";
	public static String DEPT_NM = "deptNm";
	public static String COMPANY = "company";
	public static String CORP_ID = "corpId";
	public static String REP_CORP_ID = "repCorpId";
	public static String HR_DEPT = "hrDept";
	
	public static String CORP_CODE = "corpCode";
	public static String CORP = "corp";
	public static String ACCT_UNIT = "acctUnit";
	public static String ACCT_UNIT_NM = "acctUnitNm";
	public static String TAX_ACCT_UNIT = "taxAcctUnit";
	public static String TAX_ACCT_UNIT_NM = "taxAcctUnitNm";
	
	public static String CONCURRENT_COUNT = "concurrentCount";
	
	public static String TASK_DATE = "taskDate";
	
	public static String LANGUAGE = "language";
	public static String EDPS_USER = "edpsUser";
	public static String EDPS_YN = "edpsYn";
	
	public static String MENU_USER_ID = "menuUserId";
	public static String USER_LOCATION = "userLocation";
	public static String REMOTE_IP = "remoteIp";
	public static String MOBILE = "mobile";
	
	public static String CAMP_USER_EMP = "campUserEmp";
	public static String CAMP_USER_MOBILE = "campUserMobile";
	public static String CAMP_USER_EMP_NM = "campUserEmpNm";
	
	public static String MANAGER_ROLE = "managerRole";
	
	public static String getManagerRole() {
		return Session.getSession(MANAGER_ROLE);
	}
	
	public static String getEmp() {
		return Session.getSession(EMP);
	}
	
	public static String getEmpNm() {
		return Session.getSession(EMP_NM);
	}
	
	public static String getDept() {
		return Session.getSession(DEPT);
	}
	
	public static String getUserLocation(String userid, String clientIP) {
		
		// User Location 정의

		String userLoc = "";
		
		if(clientIP.indexOf("70.150.") == 0){
			userLoc = "V"; //KCC VPN
		}else if(clientIP.indexOf("70.170.") == 0){
			userLoc = "V"; //KCC GLASS VPN
		}else if(clientIP.indexOf("70.") == 0){
			userLoc = "I"; //사내
		}else{
			userLoc = "E"; //사외
		}
		
		
		return userLoc;
	}
		
}
