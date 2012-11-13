/**
 * <li>文件名：CoderHandler.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.hander;

import java.io.IOException;

import com.jason.server.Client;
import com.jason.server.Response;

/**
 * <li>类型名称：
 * <li>说明：网络协议编码接口
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public abstract class CoderHandler {
	/**
	 * 
	 * <li>方法名：process
	 * <li>@param sockector
	 * <li>返回类型：void
	 * <li>说明：对处理后的结果数据进行编码，并将编码后的请求响应设置给Sockector
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public abstract void process(Client sockector);
	
	/**
	 * 
	 * <li>方法名：handShak
	 * <li>@param sockector
	 * <li>返回类型：void
	 * <li>说明：执行握手处理的业务，主要用于连接的客户端向服务器端发起握手处理
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void handShak(Client sockector){
		
	}
	
	public void broundMsg(Client sockector, Response msg) throws IOException{
		
	}
}
