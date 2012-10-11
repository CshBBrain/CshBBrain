/**
 * <li>文件名：Client.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-9-10
 * <li>修改人： 
 * <li>修改日期：
 */
package com.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.Config;

/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-9-10
 * <li>修改人： 
 * <li>修改日期：
 */
public class Client {
	private static Log log = LogFactory.getLog(Config.class);// 日志记录器
	/**
	 * <li>方法名：main
	 * <li>@param args
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-10
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static void main(String[] args) {
		try {
			Socket sk = new Socket("192.168.1.220",9090);
			sk.getOutputStream().write("GET /echo HTTP/1.1\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Upgrade: websocket\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Connection: Upgrade\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Host: 192.168.1.220:9090\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Origin: null\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Sec-WebSocket-Key: tO03f6yG86XB6K2k0UEfRg==\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Sec-WebSocket-Version: 13\r\n".getBytes("utf-8"));
			sk.getOutputStream().write("Sec-WebSocket-Extensions: x-webkit-deflate-frame\r\n\r\n".getBytes("utf-8"));
			sk.getOutputStream().flush();
			
			Thread.currentThread().sleep(200);
			
			createInputMonitorThread(sk.getInputStream(),sk.getOutputStream());			
			
			/*GET /echo HTTP/1.1
			Upgrade: websocket
			Connection: Upgrade
			Host: 192.168.1.220:9090
			Origin: null
			Sec-WebSocket-Key: tO03f6yG86XB6K2k0UEfRg==
			Sec-WebSocket-Version: 13
			Sec-WebSocket-Extensions: x-webkit-deflate-frame*/
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}
	
	private static void createInputMonitorThread(final InputStream in,final OutputStream out) {
		// 消息广播线程
		Runnable writeDistributeRunner = new Runnable(){
			public void run(){
				try{
					startReceiveMessage(in);
				}catch(Exception e){
					e.printStackTrace();
				}
			}			
		};
		
		Runnable sendRunner = new Runnable(){
			public void run(){
				try{
					startSendMessage(out);
				}catch(Exception e){
					e.printStackTrace();
				}
			}			
		};
		
		Thread receiverThread = new Thread(writeDistributeRunner);
		receiverThread.setName("消息接收线程");
		receiverThread.start();
		
		Thread senderThread = new Thread(sendRunner);
		senderThread.setName("消息发送线程");
		senderThread.start();
	}

	protected static void startReceiveMessage(InputStream in){		
		while(true){
			try{
				byte[] temBuffer = new byte[1024];
				int count = in.read(temBuffer);
				while(count > 0){
					String msg = new String(temBuffer,"utf-8");
					log.info("the count is:\r\n" + count + "\r\nthe message receive: \r\n" + msg);
					temBuffer = new byte[1024];
					count = in.read(temBuffer);
				}
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}
	
	protected static void startSendMessage(OutputStream out){		
		while(true){
			try{
				out.write(0x00);
				out.write(("the time of client is: " + System.currentTimeMillis()).getBytes("utf-8"));
				out.write(0xff);
				out.flush();
				log.info(123);
				Thread.currentThread().sleep(1000);
			}catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}		
	}

}
