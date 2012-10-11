/**
 * <li>文件名：BroadThread.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-8-24
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws.biz;

import com.jason.server.Response;
import com.jason.server.MasterServer;

/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-8-24
 * <li>修改人： 
 * <li>修改日期：
 */
public class BroadThread {
	private static BroadThread broadThread= new BroadThread();
	private Thread broadMessageThread;// 发送广播消息的线程
	
	public static BroadThread getInstance(){
		return broadThread;
	}
	
	private BroadThread(){
		//	消息广播线程
		Runnable writeDistributeRunner = new Runnable(){
			public void run(){
				try{
					startBroadMessage();
				}catch(Exception e){
					e.printStackTrace();
				}
			}			
		};
		
		this.broadMessageThread = new Thread(writeDistributeRunner);
		this.broadMessageThread.setName("广播消息生成线程");
		this.broadMessageThread.start();
	}
	
	protected void startBroadMessage(){		
		while(true){
			try{
				Response rs = new Response();
				String msg = "current datetime of server current datetime of server current datetime of server current datetime of server current datetime of server current datetime of server: " + System.currentTimeMillis();
				//System.out.println(msg);
				rs.setBody(msg);
				MasterServer.addBroadMessage(rs);
				
				broadMessageThread.sleep(10000);
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}		
	}
}
