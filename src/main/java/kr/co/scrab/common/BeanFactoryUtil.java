package kr.co.scrab.common;

import org.springframework.context.ApplicationContext;

import kr.co.scrab.kccfw.common.ApplicationContextProvider;

public class BeanFactoryUtil {

    public static Object getBean(String componentName, String beanName) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
 
        //beanName = beanName.substring(0, 1).toLowerCase() + beanName.substring(1);
        if(beanName.endsWith("Facade")) {
        	 beanName = componentName ; //SD 에서 호출시 컴포넌트만 찾기
        }else {
        	 beanName = componentName + "." + beanName;
        }
                
        return applicationContext.getBean(beanName);
    }
    
    public static Object getBean(String componentName) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
 
        return applicationContext.getBean(componentName);
    }
    
    public static Object getSqlQuery() {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        		
         return applicationContext.getBean("sqlQuery");
    }
    
    public static Object getJdbcTemplate(String jdbcName) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        
        //String[] names = applicationContext.getBeanDefinitionNames();
		//for (int i=0; i<names.length; i++) {
		//	System.out.println(names[i]);
		//}
		
         return applicationContext.getBean(jdbcName);
    }
   
}