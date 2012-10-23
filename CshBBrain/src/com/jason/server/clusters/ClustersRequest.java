package com.jason.server.clusters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * <li>类型名称：ParamterWrap
 * <li>说明：请求信息bean，用于解析握手信息
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-9-21
 * <li>修改人： 
 * <li>修改日期：
 */
class ClustersRequest{
	private static Log log = LogFactory.getLog(ClustersRequest.class);// 日志记录器
    private String host;// 机器地址 
    private String digest;// 签名
    private String protocol;// 协议
    
	public String getProtocol() {
		return protocol;
	}
	public void setProtocol(String protocol) {
		this.protocol = protocol;
	}	
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
	public String getDigest() {
		return digest;
	}
	public void setDigest(String digest) {
		this.digest = digest;
	}	

}
