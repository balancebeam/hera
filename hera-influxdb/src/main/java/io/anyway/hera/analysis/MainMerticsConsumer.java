package io.anyway.hera.analysis;

import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by yangzz on 16/12/1.
 */
public class MainMerticsConsumer {

    public static void main(String[] args) throws Exception {
        ClassPathXmlApplicationContext ctx= new ClassPathXmlApplicationContext("classpath:applicationContext.xml");
        //ctx.refresh();
        while(true){
            Thread.sleep(5000);
        }
    }
}
