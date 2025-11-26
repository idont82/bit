package kr.co.scrab.kccfw.util;


import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Result {

	private Map<String, Object> resultMap = new HashMap<String, Object>();

	// 성공메세지
	public final static String STATUS_SUCESS = "S";

	// 성공 메세지
	public final static String STATUS_SUCESS_MESSAGE = "정상 처리되었습니다.";

	// 오류메세지
	public final static String STATUS_ERROR = "E";

	// 기본 에러 상세 코드
	public final static String STATUS_ERROR_DEFAULT_DETAIL_CODE = "E9999";

	// 오류메세지
	public final static String STATUS_ERROR_MESSAGE = "처리 도중 오류가 발생되었습니다.";

	// 경고메세지
	public final static String STATUS_WARNING = "W";

	// 경고메세지
	public final static String STATUS_WARNING_MESSAGE = "처리 도중 오류가 발생되었습니다.";

	// 기본(map 타입) 웹스퀘어 view
	public final static String VIEW_DEFAULT = "wqView";

	// 결과값에 대한 메세지 key명
	public final static String MESSAGE_KEY = "rsMsg";

	// 결과값에 대한 메세지 key 값의 Key
	public final static String STATUS_CODE = "statusCode";
	public final static String MESSAGE = "message";
	public final static String MESSAGE_DETAIL = "messageDetail";
	public final static String ERROR_CODE = "errorCode";
	
	// viewType이 VIEW_STRING 일 경우 참조하는 key
	public final static String RESULT_KEY_DEFAULT = "result";

	
	// 서비스 호출 명 파라미터
	public final static String SERVICE = "service";
		
	// 서비스 호출 명 파라미터
	public final static String SERVICE_NAME = "serviceName";
		
	// 메소드 호출 명 파라미터
	public final static String METHOD_NAME = "methodName";
	
	//REQUEST 명칭
	public final static String REQUEST_NAME = "requestName";
	
	//RESPONSE 명칭
	public final static String RESPONSE_NAME = "responseName";
	
	//SESSION 명칭
	public final static String SESSION_NAME = "sessionName";
	
	//파일 업로드 명칭
	public final static String FILE_LIST_NAME = "fileListName";
	
	//파일 업로드 파라미터 명칭
	public final static String PARAM_LIST_NAME = "paramListName";
		
	public void setData(String id, String data) {
		resultMap.put(id, data);
	}

	public void setData(String id, Map data) {
		resultMap.put(id, data);
	}

	public void setData(String id, List data) {
		resultMap.put(id, data);
	}

	public void setData(String id, Object data) {
		resultMap.put(id, data);
	}
	
	public Map<String, Object> getResult() {
		if (resultMap.get(MESSAGE_KEY) == null) {
			setMsg(STATUS_SUCESS);
		}

		return resultMap;
	}

	/**
	 * 메세지 처리 - 상태 기본 메세지 처리
	 * 
	 * @date 2017.12.02
	 * @memberOf
	 * @param {} status : 메세지 상태
	 * @returns void
	 * @author Inswave
	 * @example WqModel.setMsg( STATUS_SUCCESS );
	 */
	public void setMsg(String status) {
		String msg = "";
		if (status == STATUS_ERROR) {
			msg = STATUS_ERROR_MESSAGE;
		} else if (status == STATUS_SUCESS) {
			msg = STATUS_SUCESS_MESSAGE;
		} else if (status == STATUS_WARNING) {
			msg = STATUS_WARNING_MESSAGE;
		}
		setMsg(status, msg);
	}

	/**
	 * 메세지 처리
	 * 
	 * @date 2017.12.02
	 * @memberOf
	 * @param {} status : 메세지 상태, message : 메세지 내용
	 * @returns void
	 * @author Inswave
	 * @example WqModel.setMsg( STATUS_SUCCESS, "정상 처리되었습니다." );
	 */
	public void setMsg(String status, String message) {
		setMsg(status, message, null);
	}

	/**
	 * 메세지 처리
	 * 
	 * @date 2017.12.02
	 * @memberOf
	 * @param {} status : 메세지 상태, message : 메세지 내용
	 * @returns void
	 * @author Inswave
	 * @example WqModel.setMsg(returnData, MsgUtil.STATUS_SUCCESS, "정상 처리되었습니다." , exception 객체);
	 */
	public void setMsg(String status, String message, Exception ex) {

		Map<String, Object> result = new HashMap<String, Object>();

		if (status.equals(STATUS_SUCESS)) {
			result.put(STATUS_CODE, STATUS_SUCESS);
			result.put(MESSAGE, getDefaultStatusMessage(message, STATUS_SUCESS_MESSAGE));
		} else if (status.equals(STATUS_WARNING)) {
			result.put(STATUS_CODE, STATUS_WARNING);
			result.put(MESSAGE, getDefaultStatusMessage(message, STATUS_WARNING_MESSAGE));
		} else if (status.equals(STATUS_ERROR)) {
			setErrorMsg(STATUS_ERROR_DEFAULT_DETAIL_CODE, message, ex);
			return;
		}

		if (ex != null) {
			result.put(MESSAGE_DETAIL, printExceptionLog(ex));
		}

		resultMap.put(MESSAGE_KEY, result);
	}

	/**
	 * 오류 메세지 처리
	 * 
	 * @date 2017.12.02
	 * @memberOf
	 * @param {} errorCode : 오류코드, message : 메세지 내용
	 * @returns void
	 * @author Inswave
	 * @example WqModel.setErrorMsg("E0001", "세션이없습니다." );
	 */
	public void setErrorMsg(String errorCode, String message) {
		setErrorMsg(errorCode, message, null);
	}

	/**
	 * 오류 메세지 처리
	 * 
	 * @date 2017.12.02
	 * @memberOf
	 * @param {} errorCode : 오류코드, message : 메세지 내용
	 * @returns void
	 * @author Inswave
	 * @example WqModel.setErrorMsg("E0001", "세션이없습니다." , exception 객체);
	 */
	public void setErrorMsg(String errorCode, String message, Exception ex) {
		Map<String, Object> result = new HashMap<String, Object>();

		result.put(STATUS_CODE, STATUS_ERROR);
		result.put(ERROR_CODE, errorCode);
		result.put(MESSAGE, getDefaultStatusMessage(message, STATUS_ERROR_MESSAGE));

		if (ex != null) {
			result.put(MESSAGE_DETAIL, "" + printExceptionLog(ex));
		}
		resultMap.put(MESSAGE_KEY, result);
	}

	public String getDefaultStatusMessage(String message, String defMessage) {
		if (message == null) {
			return defMessage;
		}
		return message;
	};
	
	public String printExceptionLog(Exception e) {
		StringBuffer msgBuf = new StringBuffer();
		//msgBuf.append(e.printStackTrace());
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		PrintStream printStream = new PrintStream(out);
		e.printStackTrace(printStream);
		msgBuf.append(out.toString());
	
		out = null;
		printStream =null;

		return msgBuf.toString();
	}
}