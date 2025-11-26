package kr.co.scrab.kccfw.common;

import org.springframework.context.ApplicationContext;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

public class BeanUtils {

    public static Object getBean(String beanName) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
 
        //String[] beanDefinitionNames = applicationContext.getBeanDefinitionNames();
        
        //System.out.println("======================" + Arrays.toString(beanDefinitionNames));
        
        
       // ApplicationContext context = new AnnotationConfigApplicationContext(ApplicationConfig.class);
        //String[] beanDefinitionNames = context.getBeanDefinitionNames();
        //System.out.println(Arrays.toString(beanDefinitionNames));
        
        return applicationContext.getBean(beanName);
    }
   
    public static SqlQuery getSqlQuery() {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        		
         return (SqlQuery)applicationContext.getBean("sqlQuery");
    }
    
    public static NamedParameterJdbcTemplate getJdbcTemplate(String jdbcName) {
        ApplicationContext applicationContext = ApplicationContextProvider.getApplicationContext();
        
        //String[] names = applicationContext.getBeanDefinitionNames();
		//for (int i=0; i<names.length; i++) {
		//	System.out.println(names[i]);
		//}
		
         return (NamedParameterJdbcTemplate)applicationContext.getBean(jdbcName);
    }
}