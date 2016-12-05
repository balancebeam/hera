package io.anyway.hera.analysis.act;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;

import javax.jms.*;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by yangzz on 16/9/13.
 */
public class JMSConsumer {

    private static final String USERNAME = ActiveMQConnection.DEFAULT_USER;//默认连接用户名
    private static final String PASSWORD = ActiveMQConnection.DEFAULT_PASSWORD;//默认连接密码
    private static final String BROKEURL = ActiveMQConnection.DEFAULT_BROKER_URL;//默认连接地址

    private static ExecutorService executorService= Executors.newCachedThreadPool();

    public static void main(String[] args) {
        Connection connection = null;//连接

        Session session;//会话 接受或者发送消息的线程
        Destination destination;//消息的目的地

        MessageConsumer messageConsumer;//消息的消费者

        //实例化连接工厂
        final ConnectionFactory connectionFactory = new ActiveMQConnectionFactory(JMSConsumer.USERNAME, JMSConsumer.PASSWORD, JMSConsumer.BROKEURL);

        for(int i=0;i<2;i++) {
            final int index= i;
            executorService.submit(new Runnable() {
                @Override
                public void run() {
                    newConsumer(connectionFactory,index);
                }
            });
        }

//
//        try {
//            //通过连接工厂获取连接
//            connection = connectionFactory.createConnection();
//            //启动连接E
//            connection.start();
//            //创建session
//            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
//            //创建一个连接HelloWorld的消息队列
//            destination = session.createQueue("HelloWorld");
//            //创建消息消费者
//            messageConsumer = session.createConsumer(destination);
//
//            while (true) {
//                TextMessage textMessage = (TextMessage) messageConsumer.receive(100000);
//                if(textMessage != null){
//                    System.out.println("收到的消息:" + textMessage.getText());
//                }else {
//                    break;
//                }
//            }
//
//
//        } catch (JMSException e) {
//            e.printStackTrace();
//        }

    }

    private static void newConsumer(ConnectionFactory connectionFactory,final int i){

        Connection connection = null;//连接

        Session session;//会话 接受或者发送消息的线程
        Destination destination;//消息的目的地

        MessageConsumer messageConsumer;//消息的消费者
        try {
            //通过连接工厂获取连接
            connection = connectionFactory.createConnection();
            //启动连接
            connection.start();
            //创建session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //创建一个连接HelloWorld的消息队列
            destination = session.createQueue("HelloWorld");
            //创建消息消费者
            messageConsumer = session.createConsumer(destination);
            messageConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(i==0){
                        System.out.println("收到的消息:" + message.toString());
                    }
                    else{
                        System.err.println("收到的消息:" + message.toString());
                    }
                }
            });

//            while (true) {
//                TextMessage textMessage = (TextMessage) messageConsumer.receive(100);
//                if(textMessage != null){
//                    if(i==0){
//                        System.out.println("收到的消息:" + textMessage.getText());
//                    }
//                    else{
//                        System.err.println("收到的消息:" + textMessage.getText());
//                    }
//                }else {
//                    break;
//                }
//                Thread.sleep((long)new Random().nextInt(200));
//            }
            while(true){
                Thread.sleep(500);
            }
            //connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void doTopic(ConnectionFactory connectionFactory,final int i){
        Connection connection = null;//连接

        Session session;//会话 接受或者发送消息的线程
        Destination destination;//消息的目的地

        MessageConsumer messageConsumer;//消息的消费者
        try {
            //通过连接工厂获取连接
            connection = connectionFactory.createConnection();
            //启动连接
            connection.start();
            //创建session
            session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //创建一个连接HelloWorld的消息队列
            destination = session.createTopic("test");
            //创建消息消费者
            messageConsumer = session.createConsumer(destination);
            messageConsumer.setMessageListener(new MessageListener() {
                @Override
                public void onMessage(Message message) {
                    if(i==0){
                        System.out.println("收到的消息:" + message.toString());
                    }
                    else{
                        System.err.println("收到的消息:" + message.toString());
                    }
                }
            });


            while(true){
                Thread.sleep(500);
            }
            //connection.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
