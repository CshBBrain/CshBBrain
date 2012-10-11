/**
 * <li>文件名：BufferPool.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-2-2
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server;

import java.nio.ByteBuffer;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.Config;

/**
 * <li>类型名称：
 * <li>说明：直接缓冲区池，重复利用分配的直接缓冲区
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-2-2
 * <li>修改人： 
 * <li>修改日期：
 */
public class BufferPool{
	private static Log log = LogFactory.getLog(BufferPool.class);// 日志记录器
	private static final String MAX_BUFFER_POOL_SIZE = "maxBufferPoolSize";// 直接缓冲区池上限大小
	private static final String MIN_BUFFER_POOL_SIZE = "minBufferPoolSize";// 直接缓冲区池下限大小
	private static final String WRITE_BUFFER = "writeBuffer";
	
	private static int maxBufferPoolSize = 1000;// 默认的直接缓冲区池上限大小1000
	private static int minBufferPoolSize = 1000;// 默认的直接缓冲区池下限大小1000
	private static int writeBufferSize = 64;// 响应缓冲区大小默认为4k
	
	private static BufferPool bufferPool = new BufferPool();// BufferPool的单实例
	
	private AtomicInteger usableCount = new AtomicInteger();// 可用缓冲区的数量
	private AtomicInteger createCount = new AtomicInteger();// 已创建了缓冲区的数量
	private ConcurrentLinkedQueue<ByteBuffer> queue = new ConcurrentLinkedQueue<ByteBuffer>();// 保存直接缓存的队列
	
	static{
		// 设置缓冲区池上限大小
		Integer maxSize = Config.getInt(MAX_BUFFER_POOL_SIZE);// 获取配置中的线程优先级
		if(maxSize != null){
			maxBufferPoolSize = maxSize;
		}
		
		// 设置缓冲区池下限大小
		Integer minSize = Config.getInt(MIN_BUFFER_POOL_SIZE);// 获取配置中的线程优先级
		if(minSize != null){
			minBufferPoolSize = minSize;
		}
		
		// 设置响应缓冲区大小
		Integer bufferSize = Config.getInt(WRITE_BUFFER);
		if(bufferSize != null){
			writeBufferSize = bufferSize;
		}
	}
	
	private BufferPool(){
		// 预先创建直接缓冲区
		for(int i = 0; i < minBufferPoolSize; ++i){
			ByteBuffer bb = ByteBuffer.allocateDirect(writeBufferSize * 1024);
			this.queue.add(bb);
		}
		
		// 设置可用的缓冲区和已创建的缓冲区数量
		this.usableCount.set(minBufferPoolSize);
		this.createCount.set(minBufferPoolSize);
	}
	
	/**
	 * 
	 * <li>方法名：getBuffer
	 * <li>@return
	 * <li>返回类型：ByteBuffer
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public ByteBuffer getBuffer(){
		ByteBuffer bb = this.queue.poll();
		
		if(bb == null){// 如果缓冲区不够则创建新的缓冲区
			bb = ByteBuffer.allocateDirect(writeBufferSize * 1024);
			this.createCount.incrementAndGet();
		}else{
			this.usableCount.decrementAndGet();
		}
		
		return bb;
	}
	
	/**
	 * 
	 * <li>方法名：releaseBuffer
	 * <li>@param bb
	 * <li>返回类型：void
	 * <li>说明：释放缓冲区
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void releaseBuffer(ByteBuffer bb){
		if(this.createCount.intValue() > maxBufferPoolSize && (this.usableCount.intValue() > (this.createCount.intValue() / 2) ) ){
			bb = null;
			this.createCount.decrementAndGet();
		}else{
			this.queue.add(bb);
			this.usableCount.incrementAndGet();
		}
	}
	
	public static BufferPool getInstance(){
		return bufferPool;
	}
}
