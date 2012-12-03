/**
 * <li>文件名：HttpServer.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws;

import java.io.IOException;

import com.jason.server.MasterServer;
import com.jason.server.ws.biz.BroadThread;

/**
 * <li>类型名称：
 * <li>说明：websocket服务器入口类。
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public class Server{

	/**
	 * <li>方法名：main
	 * <li>@param args
	 * <li>返回类型：void
	 * <li>说明：WebSocket服务器，设置websocketdecoder,websocketProcess,websocketcoder给服务器
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static void main(String[] args){
		try{
			new MasterServer(new WebSocketCoder(), new WebSocketDecoder(), new WebSocketProcesser());
			//BroadThread.getInstance();
		}catch(IOException e){		
			e.printStackTrace();
		}
	}
}
