/**
 * <li>文件名：ShareDataSet.java
 * <li>说明：
 * <li>创建人： CshBBrain, 技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-11-2
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server;

import java.util.HashMap;
import java.util.TreeMap;

import com.jason.server.clusters.ClustersNode;

/**
 * <li>类型名称：
 * <li>说明：共享数据集，集群服务器与业务服务之间的共享数据
 * <li>创建人： CshBBrain, 技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-11-2
 * <li>修改人： 
 * <li>修改日期：
 */
public class ShareDataSet {
	private static ShareDataSet shareDataSet = new ShareDataSet();// 服务单实例;// 服务单实例	
	private TreeMap<Integer,ClustersNode> sortClusters = new TreeMap<Integer,ClustersNode>();// 排序的节点服务器	
	private HashMap<String,ClustersNode> mapClusters = new HashMap<String,ClustersNode>();// 节点服务器映射表
	public static ShareDataSet getInstance(){
		return shareDataSet;
	}
	
	private ShareDataSet(){
		
	}
	
	public TreeMap<Integer, ClustersNode> getSortClusters() {
		return sortClusters;
	}

	public void setSortClusters(TreeMap<Integer, ClustersNode> sortClusters) {
		this.sortClusters = sortClusters;
	}

	public HashMap<String, ClustersNode> getMapClusters() {
		return mapClusters;
	}

	public void setMapClusters(HashMap<String, ClustersNode> mapClusters) {
		this.mapClusters = mapClusters;
	}
	
	
}
