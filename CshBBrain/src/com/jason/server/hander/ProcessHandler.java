/**
 * <li>文件名：ProcessHandler.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.hander;

import com.jason.server.Client;

/**
 * <li>类型名称：
 * <li>说明：服务器业务处理接口
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public abstract class ProcessHandler {
	/**
	 * 
	 * <li>方法名：process
	 * <li>@param sockector
	 * <li>返回类型：void
	 * <li>说明：对用户的请求进行业务上的处理并将处理结果放回到Seockector中，等待编码
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public abstract void process(Client sockector);
}
