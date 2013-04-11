package com.jason.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.Config;
import com.jason.server.clusters.ClustersCoder;
import com.jason.server.clusters.ClustersDecoder;
import com.jason.server.clusters.ClustersProcesser;
import com.jason.server.hander.CoderHandler;
import com.jason.server.hander.DecoderHandler;
import com.jason.server.hander.ProcessHandler;
import com.jason.util.MyStringUtil;

/**
 * 
 * <li>类型名称：
 * <li>说明：基于JAVA NIO 的面向TCP/IP的,非阻塞式Sockect服务器框架主类
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-9
 * <li>修改人： 
 * <li>修改日期：
 */
public class MasterServer {
	private static Log log = LogFactory.getLog(MasterServer.class);// 日志记录器
	// 系统配置参数参数名
	private static final String PORT_NAME = "port";// 端口
	private static final String MAX_PRIORITY = "maxPriority";// 线程优先级
	private static final String REQUEST_WORKER = "requestWorker";// 请求处理线程因子/核
	private static final String MONITOR_WORKER = "monitorWorker";// 数据接收监听线程因子/核
	private static final String SOCKECT_SEND_BUFFER_SIZE = "sockectSendBufferSize";// sockect读取缓冲区大小
	private static final String SOCKECT_RECVEID_BUFFER_SIZE = "sockectReceiveBufferSize";// sockect发送缓冲区大小
	
	// 集群相关参数
	private static final String CLUSTERS_SWITCH = "clustersSwitch";// 集群开关
	private static final String CLUSTERS_PORT = "clustersPort";// 集群端口
	private static final String CLUSTERS_ROLE = "clustersRole";// 集群服务器责任
	private static final String MASTER_SERVER = "masterServer";// 集群管理服务器
	
	private static final String BROAD_SWITCH = "broadSwitch";// 广播开关
	private static final String KEEP_CONNECT = "keepConnect";
	private static final String TIME_OUT = "timeOut";// timeout 参数
	public static boolean keepConnect = false;// 是否保持链接
	
	private int sockectSendBufferSize = 64;// 默认为64k
	private int sockectReceiveBufferSize = 5;// 默认为5k
	private Boolean broadSwitch = false;//默认广播开关关闭
	private Integer timeOut = 10;// 默认没有收到数据的超时阀值为10分钟，超过指定时间没有收到数据立即关闭连接
	
	// 定义编码处理器，业务处理器，解码处理器
	private CoderHandler coderHandler;// 编码处理器
	private DecoderHandler decoderHandler;// 解码处理器
	private ProcessHandler processHandler;// 业务处理器
	
	// 定义集群编码处理器，集群处理器和集群解码处理器
	private ClustersCoder clustersCoder;// 集群编码处理器
	private ClustersDecoder clustersDecoder;// 集群解码处理器
	private ClustersProcesser clustersProcesser;// 集群处理器
	private Client localClient;// 连接到集群服务器的客户端
	
	private static volatile LinkedBlockingQueue<Response> broadMessages = new LinkedBlockingQueue<Response>();// 广播消息队列 
	private String stockData;// 股指数据，多个股指之间用逗号分隔
	
	private ConcurrentHashMap<Object, Client> clients = new ConcurrentHashMap<Object, Client>();// 客户端链接映射表
	private ConcurrentHashMap<Object, Client> clustersClients = new ConcurrentHashMap<Object, Client>();// 集群服务上的所有节点服务器客户端链接映射表
	private ConcurrentHashMap<Object, Client> localClients = new ConcurrentHashMap<Object, Client>();// 连接到其他非管理服务器上的所有本地链接映射表
	
	private final AtomicInteger connectIndex = new AtomicInteger();// 连接序号
	private final AtomicInteger threadIndex = new AtomicInteger();// 线程索引号
	private final AtomicInteger keyIndex = new AtomicInteger();// 连接序号
	
	private volatile Integer port;// 业务处理服务器端口
	
	// 服务器集群参数变量
	private Boolean clustersSwitch = false;// 集群开关,默认不开启
	private Integer clustersPort = 9292;// 集群端口，默认为9292
	private Integer clustersRole = 1;// 集群服务器责任，默认为普通业务服务器
	private String masterServer;// 集群管理服务器
	
	private Thread connectMonitor;// 连接处理线程
	private Thread clustersMonitor;// 集群连接处理线程
	
	private int coreCount = 1;// CPU内核数量
	private int readerWriterCount = 2;// 读写监听线程数量
	private int workerCount = 1;// 工作线程数量

	private ArrayList<ReadWriteMonitor> readWriteMonitors;// 客户发送请求监督程序
	
	private Thread broadMessageThread;// 发送广播消息的线程
	private Thread clustersClientThread;// 发送广播消息的线程
	
	private Thread clientMonitor;// 客户端连接数据接收状况监听，对于超过时限没有接收到数据的客户端关闭连接
	
	private volatile BlockingQueue<Worker> workers;// 读取的工作线程
	private volatile ArrayList<Worker> workersList;// 读取线程列表
		
	private volatile boolean noStopRequested = true;// 循环控制变量
	
	public MasterServer(CoderHandler coderHandler, DecoderHandler decoderHandler, ProcessHandler processHandler)throws IOException{
		// 设置端口
		this.port = Config.getInt(PORT_NAME);// 从配置中获取端口号
		if(this.port == null){
			this.port = 9090;// 设置默认端口为9090
		}
		
		// 设置超时
		this.timeOut = Config.getInt(TIME_OUT);// 获取配置中的超时设置
		if(this.timeOut == null){
			this.timeOut = 10;// 设置默认超时时限为10分钟
		}
		
		// 设置线程优先级
		Integer serverPriority = Config.getInt(MAX_PRIORITY);// 获取配置中的线程优先级
		if(serverPriority == null){
			serverPriority = 5;// 设置默认线程优先级
		}
		
		// 设置请求读取处理线程每个核心的线程数
		Integer requestWorker = Config.getInt(REQUEST_WORKER);
		if(requestWorker == null){
			requestWorker = 5;
		}
		
		// 设置请求响应处理线程每个核心的线程数
		Integer monitorWorker = Config.getInt(MONITOR_WORKER);
		if(monitorWorker == null){
			monitorWorker = 1;
		}
		
		// 设置sockect数据接收缓冲区大小
		Integer receiveBuffer = Config.getInt(SOCKECT_RECVEID_BUFFER_SIZE);
		if(receiveBuffer != null){
			this.sockectReceiveBufferSize = receiveBuffer;
		}
		
		// 设置sockect数据读取缓冲区大小
		Integer sendBuffer = Config.getInt(SOCKECT_SEND_BUFFER_SIZE);
		if(sendBuffer != null){
			this.sockectSendBufferSize = sendBuffer;
		}
		
		//设置是否保存长连接
		Integer keepAlive = Config.getInt(KEEP_CONNECT);
		if(keepAlive != null){
			keepConnect = (keepAlive == 1 ? true : false);
		}
		
		// 设置广播开关
		Boolean broad = Config.getBoolean(BROAD_SWITCH);
		if(broad != null){
			this.broadSwitch = broad;
		}
		
		// 设置集群开关
		Boolean clustersSwitch = Config.getBoolean(CLUSTERS_SWITCH);
		if(clustersSwitch != null){
			this.clustersSwitch = clustersSwitch;
		}
		
		// 设置集群端口
		Integer clustersPort = Config.getInt(CLUSTERS_PORT);
		if(clustersPort != null){
			this.clustersPort = clustersPort;
		}
		
		// 设置服务器的职责
		Integer clustersRole = Config.getInt(CLUSTERS_ROLE);
		if(clustersRole != null){
			this.clustersRole = clustersRole;
		}
		
		// 设置集群中的上级服务器
		String masterServer = Config.getStr(MASTER_SERVER);
		if(masterServer != null){
			this.masterServer = masterServer;
		}
		
		// 设置解码器，编码器和业务处理器
		this.coderHandler = coderHandler;
		this.decoderHandler = decoderHandler;
		this.processHandler = processHandler;
		
		this.coreCount = Runtime.getRuntime().availableProcessors();
		this.readerWriterCount = coreCount * monitorWorker;
		this.workerCount = coreCount * requestWorker;
		
		this.workers = new ArrayBlockingQueue<Worker>(this.workerCount);
		this.workersList = new ArrayList<Worker>(this.workerCount);//响应处理线程列表
		
		for(int i = 0; i < this.workerCount; ++i){
			workersList.add(new Worker(serverPriority,workers));			
		}		
		
		readWriteMonitors = new ArrayList<ReadWriteMonitor>(this.readerWriterCount);
		for(int i = 0; i < this.readerWriterCount;++i){
			readWriteMonitors.add(create(serverPriority));// 创建请求发送监听线程
		}
		
		if(this.broadSwitch){// 根据开关是否创建广播线程
			this.createBroadMessageThread(serverPriority);//创建广播消息线程
		}
		
		this.createConnectDistributeThread(serverPriority);// 创建业务连接建立监听线程
		
		if(this.timeOut > 0){
			this.createClientMonitorThread(serverPriority);// 创建客户端数据接收状态监听线程
		}		
		
		// 处理集群初始化
		if(this.clustersSwitch){
			// 创建集群编码解码器和集群业务处理器
			this.clustersCoder = new ClustersCoder();
			this.clustersDecoder = new ClustersDecoder();
			this.clustersProcesser = new ClustersProcesser();
			
			this.createClustersDistributeThread(serverPriority);// 创建集群连接建立监听线程
			
			// 连接到自己的直接管理服务器节点
			if((this.clustersRole == 1 || this.clustersRole == 3) && !MyStringUtil.isBlank(this.masterServer)){
				this.createClustersClientThread(serverPriority);// 创建集群通信客户端消息处理线程
			}
		}
	}
	
	/**
	 * 
	 * <li>方法名：createClustersClientThread
	 * <li>@param serverPriority
	 * <li>返回类型：void
	 * <li>说明：创建集群服务器客户端处理线程
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void createClustersClientThread(int serverPriority) {
		// 集群服务器通信处理客户端线程
		Runnable writeDistributeRunner = new Runnable(){
			public void run(){
				try{
					startClustersMessage();
				}catch(Exception e){
					e.printStackTrace();
				}
			}			
		};
		
		this.clustersClientThread = new Thread(writeDistributeRunner);
		this.clustersClientThread.setName("集群通信客户端消息处理线程");
		this.clustersClientThread.setPriority(serverPriority);
		this.clustersClientThread.start();
		log.info("集群通信客户端消息处理线程创建完毕");
	}
	
	/**
	 * 
	 * <li>方法名：startClustersMessage
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：开始处理集群通信客户端的消息
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	protected void startClustersMessage(){
		Boolean isConnect = true;// 是否连接集群服务器
		SocketChannel socketChannel = null;// 连接客户端通道
		
		String[] address = this.masterServer.split(":");// 集群服务器地址
		if(address.length < 2){// 集群服务器地址错误
			log.info("集群服务器地址配置错误，集群服务器地址格式为：ip:port，例如：192.168.2.32：9090，请重新配置后再启动");
			return;
		}
		 
		if(MyStringUtil.isBlank(address[0]) || MyStringUtil.isBlank(address[1])){// 集群服务器ip地址错误
			log.info("集群服务器ip地址配置错误，集群服务器ip地址格式为：ip:port，例如：192.168.2.32：9090，请重新配置后再启动");
			return;
		}					
		 
		InetSocketAddress socketAddress = new InetSocketAddress(address[0], Integer.parseInt(address[1]));
		 
		while(noStopRequested){
			try{				
				if(isConnect && socketChannel == null){// 第一次连接或连接断开都需要连接到集群服务器上 
				try{					 
					 if(socketChannel != null && socketChannel.isConnected() && !socketChannel.isOpen()){
						 socketChannel.close();  
						 log.info("已经连接到集群服务器，但连接断开，重新连接");  
					 }
					 socketChannel = SocketChannel.open();
					 socketChannel.configureBlocking(false);
					 socketChannel.socket().setSoTimeout(0);
					 socketChannel.socket().setTcpNoDelay(true);
			         socketChannel.socket().setReuseAddress(true); 
			         
			         socketChannel.socket().setReceiveBufferSize(this.sockectReceiveBufferSize * 1024);// 设置接收缓存
			         socketChannel.socket().setSendBufferSize(this.sockectSendBufferSize * 1024);// 设置发送缓存
			 		
			         socketChannel.connect(socketAddress);
			         
			         if(socketChannel.finishConnect()){
			        	 //socketChannel.finishConnect();// 连接建立完成
			        	 isConnect = false;// 不需要做连接服务器了的操作了，已经连接上集群服务器了  			        	 
			        	 this.readWriteMonitors.get(Math.abs(this.connectIndex.getAndIncrement()) 
			    			     % this.readWriteMonitors.size()).registeClient(socketChannel);			        	 
			        	 log.info("成功连接到集群服务器  " + address[0] + " 的端口:" + address[1]); 
			        	 Thread.sleep(60 * 1000);// 休眠60秒钟
		            }
			         
			        }catch(ClosedChannelException e){  
			            log.info("连接集群服务器失败");  
			        }catch(IOException e){  
			            log.info("连接集群服务器失败");  
			        }
				}else{// 处理消息监听
					if(isConnect && socketChannel.isConnectionPending()){// 正在连接服务器
						Thread.sleep(1000);// 正在连接，等待30秒
					}
					
					try {
						if(isConnect && socketChannel.finishConnect()){// 已经建立连接
							 try{
								socketChannel.finishConnect();// 连接建立完成
						    	isConnect = false;// 不需要做连接服务器了的操作了，已经连接上集群服务器了  	
								this.readWriteMonitors.get(Math.abs(this.connectIndex.getAndIncrement()) 
									     % this.readWriteMonitors.size()).registeClient(socketChannel);
							}catch(IOException e){								
								log.warn(e.getMessage());
							}			        	 
							log.info("成功连接到集群服务器  ");
							Thread.sleep(60 * 1000);// 休眠60秒钟
						}
					} catch (IOException e) {
						log.warn(e.getMessage());
					}
					
					if(!isConnect && !socketChannel.isConnected()){// 到集群服务器的连接端口，重新连接
						isConnect = true;
						try{
							socketChannel.connect(socketAddress);
						}catch(IOException e){
							log.warn(e.getMessage());
						}// 重新连接
						continue;
					}
					
					if(!isConnect && socketChannel.isConnected()){
						// 汇报本服务的负载情况到集群管理服务器
						if(this.localClient != null){// 已经连接好，等待通信
							StringBuilder sb = new StringBuilder();
							sb.append("节点服务器：").append(this.localClient.getLocalAddress()).append("\r\n")
							.append("服务器CPU内核数量：").append(this.coreCount).append("\r\n")
							.append("服务器读写监听线程数量：").append(this.readerWriterCount).append("\r\n")
							.append("服务器工作线程数量：").append(this.workerCount).append("\r\n")
							.append("活跃连接客户端数量：").append(this.clients.keySet().size()).append("\r\n")
							.append("活跃集群连接客户端数量：").append(this.clustersClients.keySet().size()).append("\r\n")
							.append("活跃本地连接客户端数量：").append(this.localClients.keySet().size()).append("\r\n");
							
							log.info(sb.toString());
							StringBuilder msg = new StringBuilder("action=1&");
							msg.append("coreCount=").append(this.coreCount).append("&")
							.append("readerWriterCount=").append(this.readerWriterCount).append("&")
							.append("workerCount=").append(this.workerCount).append("&")
							.append("clientCount=").append(this.clients.keySet().size()).append("&")
							.append("clustersCount=").append(this.clustersClients.keySet().size()).append("&")
							.append("port=").append(this.port).append("&")
							.append("localCount=").append(this.localClients.keySet().size());
							
							Response response = new Response();
							response.setBody(msg.toString());
							this.localClient.sendMessage(response);
							//coreCount=4&readerWriterCount=8&workerCount=32&clientCount=10000&clustersCount=5&localCount=4&port=9191
						}
						Thread.sleep(30 * 1000);//10分钟检查一次
					}
				}				
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}		
	}
		
	/**
	 * 
	 * <li>方法名：createBroadMessageThread
	 * <li>@param serverPriority
	 * <li>返回类型：void
	 * <li>说明：创建广播线程
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void createBroadMessageThread(int serverPriority) {
		// 消息广播线程
		Runnable writeDistributeRunner = new Runnable(){
			public void run(){
				try{
					startBroadMessage();
				}catch(Exception e){
					e.printStackTrace();
				}
			}			
		};
		
		this.broadMessageThread = new Thread(writeDistributeRunner);
		this.broadMessageThread.setName("消息广播线程");
		this.broadMessageThread.setPriority(serverPriority);
		this.broadMessageThread.start();
		log.info("消息广播线程创建完毕");
	}
	
	/**
	 * 
	 * <li>方法名：addReadWriteMonitors
	 * <li>@param count
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-30
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public Boolean addReadWriteMonitors(Integer count){		
		for(int i = 0; i < count;++i){
			readWriteMonitors.add(create(5));// 创建请求发送监听线程
		}
		
		return true;
	}
	
	/**
	 * 
	 * <li>方法名：addWorkers
	 * <li>@param count
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-30
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public Boolean addWorkers(Integer count){
		for(int i = 0; i < this.workerCount; ++i){
			workersList.add(new Worker(5,workers));			
		}
		
		return true;
	}

	/**
	 * 
	 * <li>方法名：startBroadMessage
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：开始处理广播消息
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	protected void startBroadMessage(){		
		while(noStopRequested){
			try{
				Response msg = broadMessages.take();//获取广播消息
				Iterator<Client> it = this.clients.values().iterator();
				boolean isCode = false;			
				while(it.hasNext()){
					//log.info(msg.getBody());
					Client socketer = it.next();
					if(!isCode){
						isCode = true;
						msg.codeMsg(socketer);
					}
					
					socketer.addBroadMsg(Response.msgRespose(msg));
					try{
						workers.take().processResponse(socketer);
					}catch(Exception e){
						e.printStackTrace();
					}
				}
				
			}catch(InterruptedException e){
				e.printStackTrace();
			}
		}		
	}
		
	/**
	 * 
	 * <li>方法名：startInputMonitor
	 * <li>@throws InterruptedException
	 * <li>@throws IOException
	 * <li>返回类型：void
	 * <li>说明：如果有客户端发送数据给服务器，服务器将放入到读取缓存池
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-29
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	
	public ReadWriteMonitor create(int serverPriority){
		ReadWriteMonitor inputMonitorThread = new ReadWriteMonitor();
		inputMonitorThread.setPriority(serverPriority);
		inputMonitorThread.start();
		
		return inputMonitorThread;
	}
	
	/**
	 * 
	 * <li>类型名称：SockectServer
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-8
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public class ReadWriteMonitor extends Thread{		
		private final AtomicBoolean wakenUp = new AtomicBoolean();		
		private Selector selector;// 选择器
		private ConcurrentLinkedQueue<SocketChannel> localSocketChannels = new ConcurrentLinkedQueue<SocketChannel>();//本地连接通道注册队列
		private ConcurrentLinkedQueue<SocketChannel> socketChannels = new ConcurrentLinkedQueue<SocketChannel>();//通道注册队列
		private ConcurrentLinkedQueue<SocketChannel> clustersSocketChannels = new ConcurrentLinkedQueue<SocketChannel>();//集群通道注册队列
		private ConcurrentLinkedQueue<SocketChannel> clientSocketChannels = new ConcurrentLinkedQueue<SocketChannel>();//集群客户端或其他客户端通道注册队列
		private ConcurrentLinkedQueue<SelectionKey> readKeys = new ConcurrentLinkedQueue<SelectionKey>();// 选择键注册read队列
		private ConcurrentLinkedQueue<SelectionKey> writeKeys = new ConcurrentLinkedQueue<SelectionKey>();// 选择键注册write队列
		private volatile LinkedBlockingQueue<SelectionKey> readPool = new LinkedBlockingQueue<SelectionKey>();// 读取队列
		private Thread processDistributer;// 客户请求响应分配线程
		
		public ReadWriteMonitor(){
			try {
				this.selector = Selector.open();
				this.setName("请求数据传输监听线程" + threadIndex.getAndIncrement());
				log.info("数据读取回写监听线程创建成功：" + this.getName());
				this.createProcessDistributer();// 创建回写响应线程
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		/**
		 * 
		 * <li>方法名：registe
		 * <li>@param socketChannel
		 * <li>@throws IOException
		 * <li>返回类型：void
		 * <li>说明：将sockect channel注册到轮询器上
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-2-2
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void registe(SocketChannel socketChannel) throws IOException {		
			// Register the new SocketChannel with our Selector, indicating
			// we'd like to be notified when there's data waiting to be read			
			this.socketChannels.offer(socketChannel);
			
			if (wakenUp.compareAndSet(false, true)) {
	            selector.wakeup();
	        }
		}
		
		/**
		 * 
		 * <li>方法名：registeClusters
		 * <li>@param socketChannel
		 * <li>@throws IOException
		 * <li>返回类型：void
		 * <li>说明：注册集群客户端连接通道
		 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-10-22
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void registeClusters(SocketChannel socketChannel) throws IOException {		
			// Register the new SocketChannel with our Selector, indicating
			// we'd like to be notified when there's data waiting to be read			
			this.clustersSocketChannels.offer(socketChannel);
			
			if (wakenUp.compareAndSet(false, true)) {
	            selector.wakeup();
	        }
		}
		
		/**
		 * 
		 * <li>方法名：registeLocalClient
		 * <li>@param socketChannel
		 * <li>@throws IOException
		 * <li>返回类型：void
		 * <li>说明：注册本地连接通道
		 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-10-28
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void registeLocalClient(SocketChannel socketChannel) throws IOException {		
			// Register the new SocketChannel with our Selector, indicating
			// we'd like to be notified when there's data waiting to be read			
			this.localSocketChannels.offer(socketChannel);
			
			if (wakenUp.compareAndSet(false, true)) {
	            selector.wakeup();
	        }
		}
		
		/**
		 * 
		 * <li>方法名：registeClient
		 * <li>@param socketChannel
		 * <li>@throws IOException
		 * <li>返回类型：void
		 * <li>说明：注册连接到集群服务器的本地连接通道
		 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-10-28
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void registeClient(SocketChannel socketChannel) throws IOException {		
			// Register the new SocketChannel with our Selector, indicating
			// we'd like to be notified when there's data waiting to be read			
			this.clientSocketChannels.offer(socketChannel);
			
			if (wakenUp.compareAndSet(false, true)) {
	            selector.wakeup();
	        }
		}
		
		/**
		 * 
		 * <li>方法名：interestWriteOps
		 * <li>@param key
		 * <li>@throws IOException
		 * <li>返回类型：void
		 * <li>说明：注册写事件
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-2-2
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void registeWrite(SelectionKey key){					
			this.writeKeys.offer(key);
			
			if (wakenUp.compareAndSet(false, true)) {
	            selector.wakeup();
	        }
		}
		
		/**
		 * 
		 * <li>方法名：interestReadOps
		 * <li>@param key
		 * <li>@throws IOException
		 * <li>返回类型：void
		 * <li>说明：注册读取事件
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-2-2
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void registeRead(SelectionKey key) throws IOException {					
			this.readKeys.offer(key);
			
			if (wakenUp.compareAndSet(false, true)) {
	            selector.wakeup();
	        }
		}
		
		/**
		 * 
		 * <li>方法名：processRegisteAnd
		 * <li>
		 * <li>返回类型：void
		 * <li>说明：处理读取通道注册和读取事件注册
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2011-11-21
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		private void processRegisteAndOps(){
			SocketChannel socketChannel = this.clustersSocketChannels.poll();
			// 集群服务器连接通道注册
			while(socketChannel != null){// 处理新建立的链接
				SelectionKey sk = null;
				try {
					sk = socketChannel.register(this.selector, SelectionKey.OP_READ);					
					Client sockector = new Client(sk,MasterServer.this,this);
						
					// 根据key中的标识位判断是否为集群
					sockector.registeHandler(clustersCoder, clustersDecoder, clustersProcesser);// 注册集群处理器
					
					sk.attach(sockector);					
					MasterServer.this.clustersClients.put(sockector.getRouteAddress(), sockector);
				}catch(Exception e){
					e.printStackTrace();
					try {
						if(sk != null ){
							sk.interestOps(0);
							sk.cancel();
						}
						socketChannel.close();
					}catch(IOException e1){
						e1.printStackTrace();
					}
				}// 将事件注册放到一个容器中统一进行处理
				socketChannel = this.clustersSocketChannels.poll();
			}
			
			// 注册本地连接通道
			socketChannel = this.localSocketChannels.poll();
			// 集群服务器连接通道注册
			while(socketChannel != null){// 处理新建立的链接
				SelectionKey sk = null;
				try {
					sk = socketChannel.register(this.selector, SelectionKey.OP_READ);					
					Client sockector = new Client(sk,MasterServer.this,this);
						
					// 根据key中的标识位判断是否为集群
					sockector.registeHandler(clustersCoder, clustersDecoder, clustersProcesser);// 注册集群处理器
					
					sk.attach(sockector);					
					MasterServer.this.localClients.put(sockector.getRouteAddress(), sockector);
				}catch(Exception e){
					e.printStackTrace();
					try {
						if(sk != null ){
							sk.interestOps(0);
							sk.cancel();
						}
						socketChannel.close();
					}catch(IOException e1){
						e1.printStackTrace();
					}
				}// 将事件注册放到一个容器中统一进行处理
				socketChannel = this.localSocketChannels.poll();
			}
			
			// 注册集群客户端请求通道的选择器
			socketChannel = this.clientSocketChannels.poll();
			while(socketChannel != null){// 处理新建立的链接
				SelectionKey sk = null;
				try {
					sk = socketChannel.register(this.selector, SelectionKey.OP_WRITE | SelectionKey.OP_READ);
					Client sockector = new Client(sk,MasterServer.this,this);
					
					// 根据key中的标识位判断是否为集群
					sockector.registeHandler(clustersCoder, clustersDecoder, clustersProcesser);// 注册集群处理器
					
					sk.attach(sockector);
					
					sockector.requestHandShak();// 发起握手请求				
					MasterServer.this.localClient = sockector;
				}catch(Exception e){
					e.printStackTrace();
					try {
						if(sk != null ){
							sk.interestOps(0);
							sk.cancel();
						}
						socketChannel.close();
					}catch(IOException e1){
						e1.printStackTrace();
					}
				}// 将事件注册放到一个容器中统一进行处理
				socketChannel = this.clientSocketChannels.poll();
			}
			
			// 注册业务请求通道的选择器
			socketChannel = this.socketChannels.poll();
			while(socketChannel != null){// 处理新建立的链接
				SelectionKey sk = null;
				try {
					sk = socketChannel.register(this.selector, SelectionKey.OP_READ);
					Client sockector = new Client(sk,MasterServer.this,this);
					
					// 根据key中的标识位判断是否为集群
					//sockector.registeHandler(clustersCoder, clustersDecoder, clustersProcesser);// 注册集群处理器
					if(MasterServer.this.clustersSwitch && MasterServer.this.clustersRole == 2){
						sockector.registeHandler(coderHandler, decoderHandler, clustersProcesser);// 注册业务处理器
					}else{
						sockector.registeHandler(coderHandler, decoderHandler, processHandler);// 注册业务处理器
					}											
					
					sk.attach(sockector);
					Integer index = keyIndex.incrementAndGet();
					sockector.setIndex(index);// 设置索引
					clients.put(index, sockector);// 放入到连接中					
				}catch(Exception e){
					e.printStackTrace();
					try {
						if(sk != null ){
							sk.interestOps(0);
							sk.cancel();
						}
						socketChannel.close();
					}catch(IOException e1){
						e1.printStackTrace();
					}
				}// 将事件注册放到一个容器中统一进行处理
				socketChannel = this.socketChannels.poll();
			}
			
			// 处理注册读取监听事件
			SelectionKey key = this.readKeys.poll();// 处理已经建立链接，上次请求读取完毕，再次注册读取事件监听器
			while(key != null){
				try{
					if(key.isValid()){
						key.interestOps(key.interestOps() | SelectionKey.OP_READ);// 关注读取事件						
					}
				}catch(Exception e){
					Client.getSockector(key).close();
					e.printStackTrace();
				}
				
				key = this.readKeys.poll();
			}
			
			// 处理注册回写监听事件
			key = this.writeKeys.poll();// 处理已经建立链接，注册写事件
			while(key != null){
				try{
					if(key.isValid()){
						key.interestOps(key.interestOps() | SelectionKey.OP_WRITE);// 关注读取事件
					}
				}catch(Exception e){
					Client.getSockector(key).close();
					e.printStackTrace();
				}
				
				key = this.writeKeys.poll();
			}
			
			wakenUp.set(false);// 设置睡眠标识
		}
		
		public void run(){
			try{
				startInputMonitor();
			}catch(Exception e){
				e.printStackTrace();
			}
		}
		
		private void startInputMonitor(){		
			while(noStopRequested){	
				try{
					//this.processRegisteAndOps();// 处理通道监听注册和读取事件注册
					
					int num = 0;
					//this.selector.selectedKeys().clear();// 清除所有key
					// Wait for an event one of the registered channels
					num = this.selector.select(50);
					
					//num = this.selector.selectNow();
					if (num > 0){
						// Iterate over the set of keys for which events are available
						Iterator selectedKeys = this.selector.selectedKeys().iterator();
						while (selectedKeys.hasNext()) {
							SelectionKey key = (SelectionKey) selectedKeys.next();
							selectedKeys.remove();
							
							if (!key.isValid()) {
								Client sk = Client.getSockector(key);
								if(sk != null){
									sk.close();
								}
								continue;
							}
		
							if (key.isReadable()){
								Client sockector = Client.getSockector(key);								
								sockector.unregisteRead();// 解除监听器对该连接输入数据的监听
								//key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
								addToReadPoll(key);// 添加到读取请求队列中
							}
							
							if (key.isWritable()){
								Client.getSockector(key).sendMsgs();
							}
						}
					}
					
					this.processRegisteAndOps();// 处理通道监听注册和读取事件注册
				}catch(Exception e){
					e.printStackTrace();
				}
			}		
		}
		
		/**
		 * 
		 * <li>方法名：addToReadPoll
		 * <li>@param key
		 * <li>返回类型：void
		 * <li>说明：将需要读取请求，进行处理的通道放入队列中
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-2-6
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		public void addToReadPoll(SelectionKey key){
			// 如果客户端链接还没有放入读取等待队列或没有正在处理，则将key放入读取队列，否则不做任何处理
			if(Client.getSockector(key).getInRead().compareAndSet(false, true)){
				this.readPool.add(key);
			}
		}
		
		/**
		 * 
		 * <li>方法名：createWriteDistributeThread
		 * <li>@return
		 * <li>返回类型：Thread
		 * <li>说明：创建响应回写处理线程，负责调度响应回写任务
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-1-19
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		private Thread createProcessDistributer(){
			// 创建请求响应分配线程
			Runnable writeDistributeRunner = new Runnable(){
				public void run(){
					try{
						startProcessDistribute();
					}catch(Exception e){
						e.printStackTrace();
					}
				}
			};
			
			this.processDistributer = new Thread(writeDistributeRunner);
			this.processDistributer.setName("响应处理线程");
			log.info("请求处理调度线程创建完毕");
			this.processDistributer.setPriority(Thread.NORM_PRIORITY);// 普通优先级
			this.processDistributer.start();
			
			return processDistributer;
		}
		
		/**
		 * 
		 * <li>方法名：startProcessDistribute
		 * <li>
		 * <li>返回类型：void
		 * <li>说明：响应调度处理程序
		 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
		 * <li>创建日期：2012-2-6
		 * <li>修改人： 
		 * <li>修改日期：
		 */
		private void startProcessDistribute(){				
			while(noStopRequested){
				try{
					workers.take().processResponse(this.readPool.take());
				}catch(Exception e){
					e.printStackTrace();
				}
			}	
		}
	}
	
	/**
	 * 
	 * <li>方法名：addBroadMessage
	 * <li>@param msg
	 * <li>返回类型：void
	 * <li>说明：将要广播的消息放到广播线程中，有广播线程负责广播
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-10-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static void addBroadMessage(Response msg){
		broadMessages.add(msg);
	}
	
	// 创建链接调度线程
	private void createClientMonitorThread(int serverPriority){
		//创建监听线程
		noStopRequested = true;
		Runnable monitorRunner = new Runnable(){
			public void run(){
				try{
					startClientMonitor();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		
		this.clientMonitor = new Thread(monitorRunner);
		this.clientMonitor.setName("客户端数据接收状况监听主线程");
		log.info("客户端数据接收状况监听主线程创建成功");
		this.clientMonitor.setPriority(serverPriority);
		this.clientMonitor.start();
	}
	
	/**
	 * 
	 * <li>方法名：startClientMonitor
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：开始执行客户端数据发送状况监听
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-12
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void startClientMonitor(){			
		while(noStopRequested){
			try {
				if(this.timeOut > 0){// 超时阀值				
					Iterator<Object> it = clients.keySet().iterator();
					while(it.hasNext()){
						try{
							Object key = it.next();
							Client client = clients.get(key);
							
							if(client.getReadStatus() == 1){// 将没有上传过数据的连接状态由1设置为4，如果下个周期检查到状态还为4，则果断关闭连接
								client.setReadStatus(4);
							}
							
							if(client.getReadStatus() == 2){// 将上传过数据的连接状态由2设置为3，如果下个周期检查到状态还为3，则果断关闭连接
								client.setReadStatus(3);
							}
							
							if(client.getReadStatus() == 3){// 关闭超时还没有交互的连接
								client.close();// 关闭连接
								clients.remove(key);// 从映射表中删除连接
							}
							
							if(client.getReadStatus() == 4){// 关闭一直没有发送数据的连接
								client.close();// 关闭连接
								clients.remove(key);// 从映射表中删除连接
							}
						}catch(Exception e){
							
						}				
					}
					
					this.clientMonitor.sleep(this.timeOut * 60 * 1000);// 暂停10分钟
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	/**
	 * 
	 * <li>方法名：createConnectDistributeThread
	 * <li>@param serverPriority
	 * <li>返回类型：void
	 * <li>说明：创建连接建立请求监听线程
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void createConnectDistributeThread(int serverPriority){
		//创建监听线程
		noStopRequested = true;
		Runnable monitorRunner = new Runnable(){
			public void run(){
				try{
					startMonitor();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		
		this.connectMonitor = new Thread(monitorRunner);
		this.connectMonitor.setName("sockent 接收主线程1");
		log.info("连接监听线程创建成功");
		this.connectMonitor.setPriority(serverPriority);
		this.connectMonitor.start();
	}
	
	/**
	 * 
	 * <li>方法名：createClustersDistributeThread
	 * <li>@param serverPriority
	 * <li>返回类型：void
	 * <li>说明：创建集群连接建立请求监听线程
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void createClustersDistributeThread(int serverPriority){
		//创建监听线程
		noStopRequested = true;
		Runnable monitorRunner = new Runnable(){
			public void run(){
				try{
					startClustersMonitor();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		};
		
		this.clustersMonitor = new Thread(monitorRunner);
		this.clustersMonitor.setName("sockent 集群接收主线程");
		log.info("集群连接监听线程创建成功");
		this.clustersMonitor.setPriority(serverPriority);
		this.clustersMonitor.start();
	}
	
	/**
	 * 
	 * <li>方法名：startClustersMonitor
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：开始监听集群服务器节点中的连接请求
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void startClustersMonitor(){		
		try{
			// Create a new selector
			Selector selector = Selector.open();
			
			// Create a new non-blocking server socket channel
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			
			InetSocketAddress isa = new InetSocketAddress(this.clustersPort);// 集群端口
			serverChannel.socket().bind(isa);			
						
			// Register the server socket channel, indicating an interest in 
			// accepting new connections
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			log.info("集群服务器准备就绪，等待集群请求到来");
			while(noStopRequested){
				try {					
					int num = 0;
					// Wait for an event one of the registered channels
					num = selector.select(100);
					if (num > 0) {
						// Iterate over the set of keys for which events are available
						Iterator<SelectionKey> selectedKeys = selector.selectedKeys().iterator();
						while (selectedKeys.hasNext()) {
							SelectionKey key = (SelectionKey) selectedKeys.next();
							selectedKeys.remove();
							
							if (!key.isValid()) {
								continue;
							}
		
							// Check what event is available and deal with it
							if (key.isAcceptable()) {
								this.accept(key,true);// 注册集群连接
							}
						}
					}					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			serverChannel.close();// 关闭服务器
			
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Clusters Server start up fail");
		}
	}
	
	/**
	 * 
	 * <li>方法名：startMonitor
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：开始监听业务请求处理
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-9-28
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void startMonitor(){		
		try{
			// Create a new selector
			Selector selector = Selector.open();
			
			// Create a new non-blocking server socket channel
			ServerSocketChannel serverChannel = ServerSocketChannel.open();
			serverChannel.configureBlocking(false);
			
			InetSocketAddress isa = new InetSocketAddress(this.port);
			serverChannel.socket().bind(isa);			
						
			// Register the server socket channel, indicating an interest in 
			// accepting new connections
			serverChannel.register(selector, SelectionKey.OP_ACCEPT);
			
			log.info("服务器准备就绪，等待请求到来");
			while(noStopRequested){
				try {					
					int num = 0;
					// Wait for an event one of the registered channels
					num = selector.select(100);
					if (num > 0) {
						// Iterate over the set of keys for which events are available
						Iterator selectedKeys = selector.selectedKeys().iterator();
						while (selectedKeys.hasNext()) {
							SelectionKey key = (SelectionKey) selectedKeys.next();
							selectedKeys.remove();
							
							if (!key.isValid()) {
								continue;
							}
		
							// Check what event is available and deal with it
							if (key.isAcceptable()) {
								this.accept(key,false);
							}
						}
					}					
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			
			serverChannel.close();// 关闭服务器
			
		}catch(Exception e){
			e.printStackTrace();
			System.out.println("Server start up fail");
		}
	}
	
	/**
	 * 
	 * <li>方法名：accept
	 * <li>@param key
	 * <li>@param isClusters
	 * <li>@throws IOException
	 * <li>返回类型：void
	 * <li>说明：根据请求创建连接，可以是来自客户端的请求，也可能是来自集群中其他服务器的请求
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private void accept(SelectionKey key,Boolean isClusters) throws IOException {
		// For an accept to be pending the channel must be a server socket channel.
		ServerSocketChannel serverSocketChannel = (ServerSocketChannel) key.channel();

		// Accept the connection and make it non-blocking
		SocketChannel socketChannel = serverSocketChannel.accept();
		socketChannel.configureBlocking(false);// 设置为非阻塞式
		
		
		socketChannel.socket().setReceiveBufferSize(this.sockectReceiveBufferSize * 1024);// 设置接收缓存
		socketChannel.socket().setSendBufferSize(this.sockectSendBufferSize * 1024);// 设置发送缓存
		
		socketChannel.socket().setSoTimeout(0);
		
		socketChannel.socket().setTcpNoDelay(true);
		if(keepConnect){//使用长连接，保持连接状态
			socketChannel.socket().setKeepAlive(true);
		}
		
		// 将客户端sockect通道注册到指定的输入监听线程上
		if(isClusters){// 集群请求通道注册
			this.readWriteMonitors.get(Math.abs(this.connectIndex.getAndIncrement()) 
			     % this.readWriteMonitors.size()).registeClusters(socketChannel);
		}else{// 业务请求通道注册
			this.readWriteMonitors.get(Math.abs(this.connectIndex.getAndIncrement()) 
			     % this.readWriteMonitors.size()).registe(socketChannel);
		}
	}
	
	
	public boolean isAlive(){
		return this.connectMonitor.isAlive();
	}
    
    public void closeServer(){
		//this.serverSocket.close();//关闭链接
		
		//停止接收主线程
		this.noStopRequested = false;
		this.connectMonitor.interrupt();		
    }

	public String getStockData() {
		return stockData;
	}

	public void setStockData(String stockData) {
		this.stockData = stockData;
	}
	
	/**
	 * 
	 * <li>方法名：clearSocket
	 * <li>@param index
	 * <li>返回类型：void
	 * <li>说明：删除指定的连接
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-10-22
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void clearSocket(Object index){
		Client client = this.localClients.remove(index);// 从本地连接中清除
		if(client != null){
			return;
		}
		
		client = this.clustersClients.remove(index);// 从集群连接中清除
		if(client != null){
			return;
		}
				
		client = this.clients.remove(index);// 从客户端连接中清除
		if(client != null){
			return;
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}

	public void setLocalClient(Client localClient) {
		this.localClient = localClient;
	}
	
	public ConcurrentHashMap<Object, Client> getClustersClients() {
		return clustersClients;
	}

	public void setClustersClients(ConcurrentHashMap<Object, Client> clustersClients) {
		this.clustersClients = clustersClients;
	}
	
	public ConcurrentHashMap<Object, Client> getLocalClients() {
		return localClients;
	}

	public void setLocalClients(ConcurrentHashMap<Object, Client> localClients) {
		this.localClients = localClients;
	}
}
