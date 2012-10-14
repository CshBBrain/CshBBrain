package com.jason.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
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
import com.jason.server.hander.CoderHandler;
import com.jason.server.hander.DecoderHandler;
import com.jason.server.hander.ProcessHandler;

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
	
	private static volatile LinkedBlockingQueue<Response> broadMessages = new LinkedBlockingQueue<Response>();// 广播消息队列 
	private String stockData;// 股指数据，多个股指之间用逗号分隔
	
	private ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap<Integer, Client>();// 客户端链接映射表
	
	private final AtomicInteger connectIndex = new AtomicInteger();// 连接序号
	private final AtomicInteger threadIndex = new AtomicInteger();// 线程索引号
	private final AtomicInteger keyIndex = new AtomicInteger();// 连接序号
	
	private volatile Integer port;
	
	private Thread connectMonitor;// 连接处理线程

	private ReadWriteMonitor[] readWriteMonitors;// 客户发送请求监督程序
	
	private Thread broadMessageThread;// 发送广播消息的线程
	
	private Thread clientMonitor;// 客户端连接数据接收状况监听，对于超过时限没有接收到数据的客户端关闭连接
	
	private volatile BlockingQueue<Worker> workers;// 读取的工作线程
	private volatile Worker[] workersList;// 读取线程列表
		
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
		
		// 设置解码器，编码器和业务处理器
		this.coderHandler = coderHandler;
		this.decoderHandler = decoderHandler;
		this.processHandler = processHandler;
		
		this.workers = new ArrayBlockingQueue<Worker>(Runtime.getRuntime().availableProcessors() * requestWorker);
		this.workersList = new Worker[Runtime.getRuntime().availableProcessors() * requestWorker];//响应处理线程列表
		
		for(int i = 0; i < Runtime.getRuntime().availableProcessors()*requestWorker; ++i){
			this.workersList[i] = new Worker(serverPriority,workers);
		}		
		
		readWriteMonitors = new ReadWriteMonitor[Runtime.getRuntime().availableProcessors() * monitorWorker];
		for(int i = 0; i < Runtime.getRuntime().availableProcessors() * monitorWorker;++i){
			readWriteMonitors[i] = create(serverPriority);// 创建请求发送监听线程
		}
		
		if(this.broadSwitch){// 根据开关是否创建广播线程
			this.createBroadMessageThread(serverPriority);//创建广播消息线程
		}
		
		this.createConnectDistributeThread(serverPriority);// 创建链接调度线程
		
		if(this.timeOut > 0){
			this.createClientMonitorThread(serverPriority);// 创建客户端数据接收状态监听线程
		}
	}
	
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

	protected void startBroadMessage(){		
		while(noStopRequested){
			try{
				Response msg = broadMessages.take();//获取广播消息
				Iterator<Client> it = this.clients.values().iterator();
				while(it.hasNext()){
					log.info(msg.getBody());
					Client socketer = it.next();
					socketer.receiveMessage(msg);
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
		private ConcurrentLinkedQueue<SocketChannel> socketChannels = new ConcurrentLinkedQueue<SocketChannel>();//通道注册队列
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
			SocketChannel socketChannel = this.socketChannels.poll();
			while(socketChannel != null){// 处理新建立的链接
				SelectionKey sk = null;
				try {
					sk = socketChannel.register(this.selector, SelectionKey.OP_READ);					
				}catch(ClosedChannelException e){
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
					int num = 0;
					//this.selector.selectedKeys().clear();// 清除所有key
					// Wait for an event one of the registered channels
					num = this.selector.select(50);
					
					this.processRegisteAndOps();// 处理通道监听注册和读取事件注册
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
								if(sockector == null){ // 如果没有进行过处理创建业务处理相关对象
									sockector = new Client(key,MasterServer.this,this);
									sockector.registeHandler(coderHandler, decoderHandler, processHandler);// 注册处理器
									key.attach(sockector);
									clients.put(keyIndex.incrementAndGet(), sockector);// 放入到连接中
								}
								
								sockector.unregisteRead();// 解除监听器对该连接输入数据的监听
								//key.interestOps(key.interestOps() ^ SelectionKey.OP_READ);
								addToReadPoll(key);// 添加到读取请求队列中
							}
							
							if (key.isWritable()){
								Client.getSockector(key).sendMsgs();
							}
						}
					}					
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
					Iterator<Integer> it = clients.keySet().iterator();
					while(it.hasNext()){
						Integer key = it.next();
						Client client = clients.get(key);
						if(!client.isReadDataFlag()){// 超时没有收到数据
							client.close();// 关闭连接
							clients.remove(key);// 从映射表中删除连接
						}else{
							client.setReadDataFlag(false);// 将读取数据标识设置为false
						}
					}
					
					this.clientMonitor.sleep(this.timeOut * 60 * 1000);// 暂停10分钟
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// 创建链接调度线程
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
	 * <li>方法名：startMonitor
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：
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
								this.accept(key);
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
			System.out.println("HttpServer start up fail");
		}
	}
	
	private void accept(SelectionKey key) throws IOException {
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
		this.readWriteMonitors[Math.abs(this.connectIndex.getAndIncrement()) 
		                        % this.readWriteMonitors.length].registe(socketChannel);		
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
}
