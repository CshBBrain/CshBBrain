package com.jason.server.ws;

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
class WebSocketRequest{
	private static Log log = LogFactory.getLog(WebSocketRequest.class);// 日志记录器
	private String requestUri;// 请求地址
    private String host;// 机器地址 
    private String origin;// 源地址
    private String cookie;// cookie
    private Boolean upgrade = false;// 是否更新
    private Boolean connection = false;// 是否保存链接
    private Long key1;// key1
    private Long key2;// key2
    private String digest;// 签名
    private Integer secVersion = 0;//版本，默认为0
    
	public Boolean getConnection() {
		return connection;
	}
	public void setConnection(Boolean connection) {
		this.connection = connection;
	}
	public String getCookie() {
		return cookie;
	}
	public void setCookie(String cookie) {
		this.cookie = cookie;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	public Long getKey1() {
		return key1;
	}
	public void setKey1(Long key1) {
		this.key1 = key1;
	}
	public Long getKey2() {
		return key2;
	}
	public void setKey2(Long key2) {
		this.key2 = key2;
	}
	public String getOrigin() {
		return origin;
	}
	public void setOrigin(String origin) {
		this.origin = origin;
	}
	public String getRequestUri() {
		return requestUri;
	}
	public void setRequestUri(String requestUri) {
		this.requestUri = requestUri;
	}
	public Boolean getUpgrade() {
		return upgrade;
	}
	public void setUpgrade(Boolean upgrade) {
		this.upgrade = upgrade;
	}
	public String getDigest() {
		return digest;
	}
	public void setDigest(String digest) {
		this.digest = digest;
	}
	public Integer getSecVersion() {
		return secVersion;
	}
	public void setSecVersion(Integer secVersion) {
		this.secVersion = secVersion;
	}

}
