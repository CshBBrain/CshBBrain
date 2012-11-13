/**
 * <li>文件名：Service.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-27
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;

import java.util.HashMap;

import com.jason.server.Client;
import com.jason.server.Response;
import com.jason.server.ShareDataSet;
import com.jason.util.MyStringUtil;

/**
 * <li>类型名称：
 * <li>说明：业务处理类
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-27
 * <li>修改人： 
 * <li>修改日期：
 */
public class ClustersService{	
	private static ClustersService service = new ClustersService();// 服务单实例;// 服务单实例		
	public static ClustersService getInstance(){
		return service;
	}
	
	private ClustersService(){}
		
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
		
		Response responseMessage = null;
		if(requestData == null){
			responseMessage = Response.msgOnlyBody("错误的请求格式");	
		}else if(MyStringUtil.isBlank(requestData.get(ClustersConstants.FILED_ACTION))){
			if(!MyStringUtil.isBlank(requestData.get(ClustersConstants.HANDSHAKE))){
				responseMessage = Response.msgOnlyBody(requestData.get(ClustersConstants.FILED_MSG));
			}else if(!MyStringUtil.isBlank(requestData.get(ClustersConstants.FILED_MSG))){
				responseMessage = Response.msgOnlyBody(requestData.get(ClustersConstants.FILED_MSG));				
			}else{
				responseMessage = Response.msgOnlyBody("没有指明的请求操作，请求中必须指明action类型");
			}
		}else{		
			try{
				// 获取请求类型
				Integer action = Integer.valueOf(requestData.get(ClustersConstants.FILED_ACTION));
				Integer count = 0;// 数量
				switch(action){
					case 1:// 集群服务器需要想管理服务汇报CPU参数，内存参数，工作线程数量，可以处理的连接极限数量，读写监听线程数量，进入监听线程数量，使用的端口
						ClustersNode clustersNode = ShareDataSet.getInstance().getMapClusters().get(sockector.getRouteAddress());
						if(clustersNode == null){
							clustersNode = new ClustersNode();
							clustersNode.setNode(sockector);// 设置连接对象
							
							ShareDataSet.getInstance().getMapClusters().put(sockector.getRouteAddress(), clustersNode);
						}
						
						Integer coreCount = Integer.valueOf(requestData.get(ClustersConstants.FILED_CORE_COUNT));//集群节点服务器CPU内核数量
						Integer readerWriterCount = Integer.valueOf(requestData.get(ClustersConstants.FILED_READER_WRITER_COUNT));//集群节点服务器读写监听线程数量
						Integer workerCount = Integer.valueOf(requestData.get(ClustersConstants.FILED_WORKER_COUNT));//集群节点服务器工作线程数量
						Integer clientCount = Integer.valueOf(requestData.get(ClustersConstants.FILED_CLIENT_COUNT));//集群节点服务器上客户端连接数量
						Integer clustersCount = Integer.valueOf(requestData.get(ClustersConstants.FILED_CLUSTERS_COUNT));//集群节点服务器上集群节点服务器连接数量
						Integer localCount = Integer.valueOf(requestData.get(ClustersConstants.FILED_LOCAL_COUNT));//集群节点服务器上本地连接数量
						Integer port = Integer.valueOf(requestData.get(ClustersConstants.FILED_PORT));//集群服务器的使用的监听端口
						
						clustersNode.setCoreCount(coreCount);
						clustersNode.setReaderWriterCount(readerWriterCount);
						clustersNode.setWorkerCount(workerCount);
						clustersNode.setClientCount(clientCount);
						clustersNode.setClustersCount(clustersCount);
						clustersNode.setLocalCount(localCount);
						clustersNode.setPort(port);
						
						ShareDataSet.getInstance().getSortClusters().put(clustersNode.getClientCount(), clustersNode);// 此处是否优化可考虑
						
						responseMessage = Response.msgOnlyBody("action=1000");
						break;
					case 2:// 管理服务器要求节点服务器增加工作线程数量
						count = Integer.valueOf(requestData.get(ClustersConstants.FILED_COUNT));//调整的数量
						sockector.getSockectServer().addWorkers(count);
						responseMessage = Response.msgOnlyBody("action=1000");
						break;
					case 3:// 管理服务器要求节点服务器减少工作线程数量
						count = Integer.valueOf(requestData.get(ClustersConstants.FILED_COUNT));//调整的数量
						
						responseMessage = Response.msgOnlyBody("action=1000");
						break;
					case 4:// 管理服务器要求节点服务器增加读写监听线程数量
						count = Integer.valueOf(requestData.get(ClustersConstants.FILED_COUNT));//调整的数量
						sockector.getSockectServer().addReadWriteMonitors(count);
						responseMessage = Response.msgOnlyBody("action=1000");
						break;
					case 5:// 管理服务器要求节点服务器减少读写监听线程数量
						count = Integer.valueOf(requestData.get(ClustersConstants.FILED_COUNT));//调整的数量
						responseMessage = Response.msgOnlyBody("action=1000");
						break;
					case 6:// 管理服务器管理权限移交和接手
						break;
					case 0:// 客户端请求分配服务器和端口地址
						ClustersNode  node = ShareDataSet.getInstance().getSortClusters().firstEntry().getValue();
						responseMessage = Response.msgOnlyBody("{ip:'" + node.getNode().getIp() +  "',port:" + node.getPort() + "}");
						break;
					case 31:// 节点服务器之间的业务数据交换
						String content = requestData.get(ClustersConstants.FILED_CONTENT);// 数据内容
						// 添加数据交换的处理逻辑
						break;
					case 32:// 管理服务器通知节点服务器A向节点服务器B交换数据
						break;
					case 1000:// 请求成功处理
						break;
					case 2000:// 请求处理失败
						break;
					default:// 
						break;
				}
				
				/*if(!MyStringUtil.isBlank(requestData.get(ClustersConstants.HANDSHAKE))){
					responseMessage = Response.msgOnlyBody(requestData.get(ClustersConstants.FILED_MSG));
				}else{
					responseMessage = Response.msgOnlyBody("Hello," + requestData.get(ClustersConstants.FILED_MSG));				
				}*/
			}catch(Exception e){
				e.printStackTrace();
				responseMessage = Response.msgOnlyBody("500处理失败了");			
			}
		}
		
		return responseMessage;
	}
	
	
}
