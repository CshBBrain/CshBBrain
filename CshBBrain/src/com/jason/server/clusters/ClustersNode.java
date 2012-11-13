/**
 * <li>文件名：ClustersNode.java
 * <li>说明：
 * <li>创建人： CshBBrain, 技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-10-30
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;

import com.jason.server.Client;

/**
 * <li>类型名称：
 * <li>说明：集群服务器节点
 * <li>创建人： CshBBrain, 技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-10-30
 * <li>修改人： 
 * <li>修改日期：
 */
public class ClustersNode{
	private Integer coreCount = 1;//集群节点服务器CPU内核数量
	private Integer readerWriterCount;//集群节点服务器读写监听线程数量
	private Integer workerCount;//集群节点服务器工作线程数量
	private Integer clientCount;//集群节点服务器上客户端连接数量
	private Integer clustersCount;//集群节点服务器上集群节点服务器连接数量
	private Integer localCount;//集群节点服务器上本地连接数量
	private Integer port;//集群服务器的使用的监听端口
	private Client node;// 集群节点服务器连接
	
	public Client getNode() {
		return node;
	}
	public void setNode(Client node) {
		this.node = node;
	}
	public Integer getCoreCount() {
		return coreCount;
	}
	public void setCoreCount(Integer coreCount) {
		this.coreCount = coreCount;
	}
	public Integer getReaderWriterCount() {
		return readerWriterCount;
	}
	public void setReaderWriterCount(Integer readerWriterCount) {
		this.readerWriterCount = readerWriterCount;
	}
	public Integer getWorkerCount() {
		return workerCount;
	}
	public void setWorkerCount(Integer workerCount) {
		this.workerCount = workerCount;
	}
	public Integer getClientCount() {
		return clientCount;
	}
	public void setClientCount(Integer clientCount) {
		this.clientCount = clientCount;
	}
	public Integer getClustersCount() {
		return clustersCount;
	}
	public void setClustersCount(Integer clustersCount) {
		this.clustersCount = clustersCount;
	}
	public Integer getLocalCount() {
		return localCount;
	}
	public void setLocalCount(Integer localCount) {
		this.localCount = localCount;
	}
	public Integer getPort() {
		return port;
	}
	public void setPort(Integer port) {
		this.port = port;
	}
	
	
}
