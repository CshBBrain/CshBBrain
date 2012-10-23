/**
 * <li>文件名：DecoderHandler.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.hander;

import java.nio.ByteBuffer;

import com.jason.server.Client;

/**
 * <li>类型名称：
 * <li>说明：网络协议解码接口
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public abstract class DecoderHandler {
	/**
	 * 
	 * <li>方法名：process
	 * <li>@param byteBuffer
	 * <li>@param sockector
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-8-20
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public abstract void process(ByteBuffer byteBuffer,Client sockector);
	
	/**
	 * 
	 * <li>方法名：handShak
	 * <li>@param sockector
	 * <li>返回类型：void
	 * <li>说明：执行握手处理的业务，主要用于服务器端处理客户端发起的握手处理
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void handShak(Client sockector){
		
	}
	
}
