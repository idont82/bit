var com = {};


/**
 * Ajax 호출 방식 - FormData 로 파일 전송 가능 (개발중)
 * @param {} options : sync, 성공/실패 핸들러
 * @param {} service : 서비스 명
 * @param {} param : 서비스로 전달할 파라미터값
 * @param {} fileList : 서비스로 전달할 파일 객체 배열
 * 
 	var options = {  async: false
                ,submitDoneHandler : function(e){         
                }
                ,submitErrorHandler : function(e){
                    com.alert(e.rsMsg.message);
                } 
  	};
            
	var service = {serviceName : "mailService",
				   methodName : "write", 
				   fileListName :"fileList" //서비스에서 파일리스트가 담기는 Map key 값
	};
 
 	var param = {
			"seqSave" : 0,
			"reportData" : scwin.crossEditor.GetBodyValue(),
			"attachList" : dlt_attach.getDeletedJSON(), //삭제할 첨부
			"deptList" : dlt_addDept.getModifiedJSON() //참조부서
	};
 	
 	com.execAjax(options, service, param, fileList);
 
 */
com.execAjax = function(options, service, param, fileList) {
	
	param.service = service;
	
	//언어 셋팅값 전달
	if(param.language == null || param.language == ""){
		param.language = com.getLanguage();
	}
	
	//한국과 시간차 전달
	if(param.timeOffset == null || param.timeOffset == ""){
		param.timeOffset = com.getTimezoneOffset();
	}
	
    
	//프로그램ID 전달
	if(param.service.pgmId == null || param.service.pgmId == ""){
		try{
			var paramData = com.getParameter("param");
			
			if(paramData.pgmId != null){ param.service.pgmId = paramData.pgmId; }
			if(paramData.pgmNm != null){ param.service.pgmNm = paramData.pgmNm; }
		}catch(ex){}
		
	}
	
	
	var createForm = document.getElementById("create_form");
	var formData = new FormData();
	
    if(fileList != null && fileList.length > 0){
    	fileList.forEach(function(f){
            formData.append(service.fileListName, f);
        });
    }         
	
    formData.append("fileListName", service.fileListName);
	formData.append("paramListName", JSON.stringify(param));
	
	$.ajax({
        url : "/ajax.do",
        data : formData,
        type:'POST',
        enctype:'multipart/form-data',
        mediatype : "application/json",
        processData:false,
        contentType:false,
        dataType:'json',
        cache:false,
        async: options.async||false,
        success:options.submitDoneHandler,
        error:options.submitErrorHandler
    });
    
};

com.execController = function(options,  param) {
	
	
	//언어 셋팅값 전달
	if(param.language == null || param.language == ""){
		param.language = com.getLanguage();
	}
	
	$.ajax({
        url : options.url,
        data : param,
        type:'POST',
        //enctype:'multipart/form-data',
        mediatype : "application/json",
        //processData:false,
        //contentType:false,
        //dataType:'json',
        cache:false,
        async: options.async||false,
        success:options.submitDoneHandler,
        error:options.submitErrorHandler
    });
    
};

//페이지 이동시 post 파라미터 전달.
com.submit = function(url, param){
	
	let f = document.createElement('form');
	
	for(key in param) {
		let obj;
	    obj = document.createElement('input');
	    obj.setAttribute('type', 'hidden');
	    obj.setAttribute('name', key);
	    obj.setAttribute('value', param[key]);
		f.appendChild(obj);
	}
	
    f.setAttribute('method', 'post');
    f.setAttribute('action', url);
    document.body.appendChild(f);
    f.submit();
	
};

com.execAjaxWithProgress = function(options, service, param, fileList, progressFunction) {
	
	param.service = service;
	
	//언어 셋팅값 전달
	if(param.language == null || param.language == ""){
		param.language = com.getLanguage();
	}
	
	//한국과 시간차 전달
	if(param.timeOffset == null || param.timeOffset == ""){
		param.timeOffset = com.getTimezoneOffset();
	}
	
    
	//프로그램ID 전달
	if(param.service.pgmId == null || param.service.pgmId == ""){
		try{
			var paramData = com.getParameter("param");
			
			if(paramData.pgmId != null){ param.service.pgmId = paramData.pgmId; }
			if(paramData.pgmNm != null){ param.service.pgmNm = paramData.pgmNm; }
		}catch(ex){}
		
	}
	
	
	var createForm = document.getElementById("create_form");
	var formData = new FormData();
	
    if(fileList!= null && fileList.length > 0){
    	fileList.forEach(function(f){
            formData.append(service.fileListName, f);
        });
    }         
	
    formData.append("fileListName", service.fileListName);
	formData.append("paramListName", JSON.stringify(param));
	
	$.ajax({
        url : "/ajax.do",
        data : formData,
        type:'POST',
        enctype:'multipart/form-data',
        mediatype : "application/json",
        processData:false,
        contentType:false,
        dataType:'json',
        cache:false,
        async: options.async||true,
        success:options.submitDoneHandler,
        error:options.submitErrorHandler,
        xhr: function() {
	        var xhr = $.ajaxSettings.xhr();
	        xhr.upload.onprogress = function(e) {  //progress 이벤트 리스너 추가
	        	progressFunction(e);
	        };
	        return xhr;
        }
    });
    
};



//현재 언어 선택 가져오기
//param {supportLang: ["ko", "en", "zh", "ja", "ru" ], defaultLang: "en"} 
com.getLanguage = function(lparam){
	var orgLanguage = com.getCookie('language') || navigator.language || navigator.userLanguage;
	orgLanguage = orgLanguage.toLowerCase().substring(0,2);
	
	if(orgLanguage== null || orgLanguage == "" || (lparam != null && lparam.supportLang.indexOf(orgLanguage) < 0) ) {
		orgLanguage = lparam.defaultLang;
	}
	
	return orgLanguage;
};

// 언어설정 : ko, en, zh
com.setLanguage = function(lang){ 
	com.setCookie("language", lang);
};


//한국시간과의 차이 분으로 가져오기
com.getTimezoneOffset = function(){
	return ( (new Date).getTimezoneOffset()  * -1) - 540;
};



/**
*쿠키를 저장한다.
 * <pre>
 *    fnSetCookie(cookieName,cookieVal,validity,domain)
 * </pre>
 * @sig    : cookieName,cookieVal,validity,domain
 * @param  : cookieName 	쿠키명
 * @param  : cookieVal 		쿠키값
 * @param  : validity 		유효기간(미입력시 10일 기본)
 * @param  : domain 		세션값을 전달할 도메인 ("kccworld.co.kr" 상위도메인)
 * @author :
 */
com.setCookie = function(cookieName,cookieVal,validity,domain){
  date = new Date();
  
  if(validity == null){
  	validity=10;
  }
  
  if(domain == null || domain == ""){
	  domain ="";
  }else{
	  domain ="; domain=" + domain+";";
  }
  
  date.setDate(date.getDate()+validity);
  document.cookie = cookieName+'='+escape(cookieVal)+'; expires='+date.toGMTString()+'; path=/' + domain;
}

/**
*저장된 쿠키를 가져온다
 * <pre>
 *    fnGetCookie(cookieName)
 * </pre>
 * @sig    : cookieName
 * @param  : cookieName 	쿠키명
 * @author :
 */
com.getCookie = function(cookieName){
  allCookies=document.cookie.split('; ');
  for (var i=0;i<allCookies.length;i++){
    cookieArray=allCookies[i].split('=');
    if (cookieName==cookieArray[0]){
		return unescape(cookieArray[1]);
	}
  }
  return "";
};

/**
*저장된 쿠키를 삭제한다.
 * <pre>
 *    fnGetCookie(cookieName)
 * </pre>
 * @param  : cookieName 쿠키명
 * @param  : selGbn 	Search 구분
 * @author :
 */
com.delCookie = function(cookieName, selGbn, domain) {
	if(selGbn){
		  allCookies=document.cookie.split('; ');
		  for (var i=0;i<allCookies.length;i++){
		    cookieArray=allCookies[i].split('=');
		    if(selGbn=="S"){ //startsWith
			    if(com.startsWithStr(cookieArray[0],cookieName)){
			    	com.setCookie(cookieArray[0], null, -10, domain);
			    }
		    }
		    if(selGbn=="C"){ //contains
			    if (cookieArray[0].indexOf(cookieName)>-1){
			    	com.setCookie(cookieArray[0], null, -10, domain);
				}		    	
		    }
		    if(selGbn=="E"){ //endsWith
			    if(com.endsWithStr(cookieArray[0],cookieName)){
			    	com.setCookie(cookieArray[0], null, -10, domain);
			    }		    	
		    }
		  }
	}else{
		com.setCookie(cookieName, null, -10, domain);
	}
};


com.getTodayCalendarFormat = function() {
	const date = new Date();
	const year = date.getFullYear();
	const month = ('0' + (date.getMonth() + 1)).slice(-2);
	const day = ('0' + (date.getDate())).slice(-2);
	
	return `${year}-${month}-${day}`;
}

com.getCalcuateDay = function(dataStr, m, d) {
	const date = new Date(dataStr);
		   
	// 월을 더함
	if(m != undefined) {
		date.setMonth(date.getMonth() + m);	
	}
	
	if(d != undefined) {
		date.setDate(date.getDate() + d);	
	}
	
	// 연도와 월을 조정한 후, 필요한 경우 추가로 조정
	const year = date.getFullYear();
	const month = (date.getMonth() + 1).toString().padStart(2, '0');
	const day = date.getDate().toString().padStart(2, '0');
	
	return `${year}-${month}-${day}`;
}

com.popup = function(str){
	
	var txt = ""
		+'<ngb-modal-backdrop style="z-index: 1055;" aria-hidden="true" class="noticePopup modal-backdrop fade"></ngb-modal-backdrop>'
		+'<ngb-modal-window role="dialog" tabindex="-1" aria-modal="true" class="noticePopup d-block modal fade">'
		+'	<div role="document" class="modal-dialog">'
		+'		<div class="modal-content">'
		+'			<app-reservation-message >'
		+'				<div class="flex flex-col reservation-container">'
		+'				  <cds-icon  shape="window-close" class="close-icon cross-stand-alone"></cds-icon>'
		+'					  <markdown >'
		+                         str
		+'						</markdown>'
		+'				</div>'
		+'			</app-reservation-message>'
		+'		</div>'
		+'	</div>'
		+'</ngb-modal-window>'
	;
	
	$('.noticePopup').remove();
	
	$("body").append(txt);
	
	
	setTimeout( function() { 
		
		$("body").css("overflow", "hidden") ;
		$(".noticePopup").addClass("show") ;
		
	}
	, 100) ;
	
	
	$('.noticePopup .close-icon ').click(function(e){
		$("body").css("overflow", "") ;
		$(".noticePopup").removeClass("show") ;
		$(".noticePopup").addClass("d-none") ;
		
	});
	
	$('.noticePopup.d-block.modal').click(function(e){
		$('.noticePopup .close-icon ').click();
	
	});
	
	$('.noticePopup.d-block.modal .modal-content').click(function(e){
		e.stopPropagation();
	});
	
	
}

com.msg = function(msg){
	var x = document.getElementById("snackbar");
	x.innerHTML = msg;
	x.className = "show";
	setTimeout(function(){ 
		x.className = x.className.replace("show", ""); 
	}, 2800);
}


com.msgBox = {
    /* Alert */
    alert: function(msg, okhandler) {
        new Promise((resolve, reject) => {
            $("#msg_popup #btn_confirm").hide();
            $("#msg_popup #btn_alert").show();
            
            $("#msg_popup #alert_ok").unbind();
            $("#msg_popup .modal-body").html(msg);
            $('#msg_popup').modal('show');

            $("#msg_popup #alert_ok").click(function() {
                $('#msg_popup').modal('hide');
            });

            $("#msg_popup").on("hidden.bs.modal", function(e) {
                e.stopPropagation();
                if(okhandler != null) resolve();
                else reject();
            });
        }).then(okhandler).catch(function() {});
    },

    /* Confirm */
    confirm: function(msg, yeshandler, nohandler) {
        new Promise((resolve, reject) => {
            var flag = false;
            $("#msg_popup #btn_alert").hide();
            $("#msg_popup #btn_confirm").show();
            
            $("#msg_popup #confirm_yes").unbind();
            $("#msg_popup #confirm_no").unbind();
            $("#msg_popup .modal-body").html(msg);
            $('#msg_popup').modal('show');

            $('#msg_popup').on('keypress', function (e) {
                var keycode = (e.keyCode ? e.keyCode : e.which);
                if(keycode == '13') {
                    flag = true;
                    $('#msg_popup').modal('hide');
                }
            });

            $("#msg_popup #confirm_yes").click(function() {
                flag = true;
            });
            $("#msg_popup #confirm_no").click(function() {
                flag = false;
            });

            $("#msg_popup").on("hidden.bs.modal", function(e) {
                e.stopPropagation();
                if(yeshandler != null && flag == true) resolve(1); 
                else if(nohandler != null && flag == false) resolve(2);
                else reject();
            });

        }).then(function(value) {
            if(value == 1)      yeshandler();
            else if(value == 2) nohandler();
        }).catch(function() {});
    },
	
	toast: function(msg, okhandler) {
        new Promise((resolve, reject) => {
            $("#msg_popup #btn_confirm").hide();
            $("#msg_popup #btn_alert").hide();
			$("#toast_popup").show();
            
            $("#msg_popup #alert_ok").unbind();
			$('#toastMsg').text(msg);
			$('.toast-header').css("display", "none");
			$('.toast').toast('show');

            $("#msg_popup").on("hidden.bs.modal", function(e) {
                e.stopPropagation();
                if(okhandler != null) resolve();
                else reject();
            });
        }).then(okhandler).catch(function() {});
    },
}


com.getParam = function(sname) {

    var params = location.search.substr(location.search.indexOf("?") + 1);
    var sval = "";

    params = params.split("&");

    for (var i = 0; i < params.length; i++) {
        temp = params[i].split("=");
        if ([temp[0]] == sname) { sval = temp[1]; }
    }
    return sval;
}

