/**
 * <li>文件名：ResponseStatus.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-5-20
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;


/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-5-20
 * <li>修改人： 
 * <li>修改日期：
 */
public class ClustersConstants {
	public static final String CLUSTERS_SPLIT_DOT = ":";// 集群协议
	public static final String CLUSTERS = "CshBBrain";// 集群协议
	public static final String HOST = "Host";//客户端主机
	public static final String ACCEPT = "Accept";// 服务器端返回的验证加密串
	public static final String PROTOCOL = "Protocol";// 集群协议
	public static final String KEY = "Key";// 客户端验证key	

	public static final String FILED_IP = "ip";// ip地址
	public static final String FILED_CLIENT_IP = "clientIp";// ip地址
	
	public static final String FILED_MSG = "msg";// 消息内容地址
	
	public static final char BEGIN_CHAR = 0x00;// 开始字符
	public static final char END_CHAR = 0xFF;// 结束字符
	public static final String BEGIN_MSG = String.valueOf(BEGIN_CHAR);// 消息开始
	public static final String END_MSG = String.valueOf(END_CHAR); // 消息结束
	public static final String BLANK = "";// 空白字符串
	public static final String UTF8 = "utf-8";//utf-8编码
	public static final String HANDSHAKE = "handshake";//握手标识
	
	public static final String GUID = "258EAFA5-E914-47DA-95CA-C5AB0DC85B11";
	public static final String HEADER_CODE = "iso-8859-1";
	public static final int SPLITVERSION0 = 0;// 版本0
	public static final int SPLITVERSION6 = 6;// 版本6
	public static final int SPLITVERSION7 = 7;// 版本7
	
	// 集群的数据接口
	public static final int CLUSTERS_REGISTE = 1;// 节点服务器注册接口
	public static final int CLUSTERS_ADD_WORKER = 2;// 增加工作线程接口
	public static final int CLUSTERS_REDUCE_WORKER = 3;// 减少工作线程接口
	public static final int CLUSTERS_ADD_MONITOR = 4;// 增加读写监听线程接口
	public static final int CLUSTERS_REDUCE_MONITOR = 5;// 减少监听线程接口
	public static final int CLUSTERS_DISPATCH = 0;// 获取节点接口
	public static final int CLUSTERS_XCHANGE_MASTER = 6;// 移交管理权限接口
	public static final int CLUSTERS_XCHANGE_DATA = 31;// 节点服务器数据交换接口
	public static final int CLUSTERS_XCHANGE_DATA_COMMAND = 32;// 节点服务器数据交换通知接口
	
	// 参数名称常量
	public static final String FILED_ACTION = "action";// 操作类型
	public static final String FILED_CONTENT = "content";// 内容信息
	public static final String FILED_CORE_COUNT = "coreCount";// 服务器CPU内核数量
	public static final String FILED_READER_WRITER_COUNT = "readerWriterCount";// 服务器读写监听线程
	public static final String FILED_WORKER_COUNT = "workerCount";// 工作处理线程
	public static final String FILED_CLIENT_COUNT = "clientCount";// 客户端连接数量
	public static final String FILED_CLUSTERS_COUNT = "clustersCount";// 服务器集群连接数量
	public static final String FILED_LOCAL_COUNT = "localCount";// 本地连接数量
	public static final String FILED_PORT = "port";// 服务器客户端接入端口
	public static final String FILED_COUNT = "count";// 数量	
}
