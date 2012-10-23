/**
 * <li>文件名：ClustersHandShak.java
 * <li>说明：
 * <li>创建人： CshBBrain, 技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-10-22
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;

/**
 * <li>类型名称：
 * <li>说明：集群握手
 * <li>创建人： CshBBrain, 技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-10-22
 * <li>修改人： 
 * <li>修改日期：
 */
public class ClustersHandShak {
	private String key;// 原始key
	private String host;//主机
	
	public ClustersHandShak(){
		
	}
	
	public ClustersHandShak(String key, String host){
		this.key = key;
		this.host = host;
	}
	
	public String getKey() {
		return key;
	}
	public void setKey(String key) {
		this.key = key;
	}
	public String getHost() {
		return host;
	}
	public void setHost(String host) {
		this.host = host;
	}
	
}
