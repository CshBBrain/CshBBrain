/**
 * <li>文件名：Service.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-27
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws.biz;

import java.util.HashMap;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.server.Client;
import com.jason.server.Response;
import com.jason.util.MyStringUtil;

/**
 * <li>类型名称：
 * <li>说明：业务处理类
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-27
 * <li>修改人： 
 * <li>修改日期：
 */
public class Service{
	private static Log log = LogFactory.getLog(Service.class);// 日志记录器
	private static Service service = new Service();// 服务单实例;// 服务单实例	
	
	public static Service getInstance(){
		return service;
	}
	
	private Service(){}
		
	/**
	 * 
	 * <li>方法名：service
	 * <li>@param requestData
	 * <li>@return
	 * <li>返回类型：ResponseMessage
	 * <li>说明：业务处理入口方法，对各种接口的请求进行处理
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-5
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public Response service(Client sockector, HashMap<String, String> requestData){
		if(requestData == null){
			return null;
		}
		
		log.info(requestData.get(Constants.FILED_MSG));
		Response responseMessage = null;
		try{
			if(!MyStringUtil.isBlank(requestData.get(Constants.HANDSHAKE))){
				responseMessage = Response.msgOnlyBody(requestData.get(Constants.FILED_MSG));
			}else{
				responseMessage = Response.msgOnlyBody("Hello," + requestData.get(Constants.FILED_MSG));				
			}
		}catch(Exception e){
			e.printStackTrace();
			responseMessage = Response.msgOnlyBody("500处理失败了");			
		}
		
		return responseMessage;
	}
	
	
}
