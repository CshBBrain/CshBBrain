/**
 * <li>文件名：FileReceiver.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-12-9
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-12-9
 * <li>修改人： 
 * <li>修改日期：
 */
public class FileTransfer{
	private static Log log = LogFactory.getLog(FileTransfer.class);// 日志记录器
	private Long fileSizeLeft = 0l;// 文件剩余大小
	private String fileName;// 保存的文件的名称
	private File file;// 文件
	private FileOutputStream fileOutputStream; // 文件输出流
	private FileChannel fileChannel;// 文件通道
	private boolean isReadHead = false;
	private Integer sizeWithHead = 0;// 有的头部只携带长度
	
	public FileTransfer(String fileName, Long fileSizeLeft){
		this.fileName = fileName;
		this.fileSizeLeft = fileSizeLeft;
		
		this.file = new File(this.fileName,"");
		try{
		if(!this.file.exists()){
			this.file.createNewFile();
		}
		
		this.fileOutputStream = new FileOutputStream(file,true);// 追加模式写文件
		this.fileChannel = fileOutputStream.getChannel();
		
		}catch(Exception e){
			e.printStackTrace();
		}
	}	
	
	public boolean writeBody(ByteBuffer byteBuffer, Integer contentBegin, Integer size){
		try{			
			System.out.println(byteBuffer.position());
			System.out.println(byteBuffer.limit());
			
			byteBuffer.position(contentBegin);// 设置文件开始缓存
			
			// 如果读取的内容大于文件的内容，则多余的内容必定为文件结尾符，丢弃文件结束符
			/*Integer size = this.fileSizeLeft > byteBuffer.limit() - contentBegin ? byteBuffer.limit() - contentBegin :  this.fileSizeLeft.intValue();
			byteBuffer.limit(this.fileSizeLeft > byteBuffer.limit() - contentBegin ? byteBuffer.limit() : byteBuffer.position() + this.fileSizeLeft.intValue());
			*/
			System.out.println("-------" +size);
			this.fileSizeLeft -= size;
			//byteBuffer.flip();
			this.fileChannel.write(byteBuffer);
			this.fileChannel.force(false);
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			return false;
		}
		byteBuffer.clear();		
		
		if(this.fileSizeLeft <= 0){// 传完，关闭文件
			this.close();
		}
		
		return true;
	}
	
	public boolean writeBody(ByteBuffer byteBuffer, Integer contentBegin){
		try{			
			System.out.println(byteBuffer.position());
			System.out.println(byteBuffer.limit());
			
			byteBuffer.position(contentBegin);// 设置文件开始缓存
			
			System.out.println("-------" +(byteBuffer.limit() - contentBegin));
			this.fileSizeLeft -= (byteBuffer.limit() - contentBegin);			
			this.fileChannel.write(byteBuffer);
			this.fileChannel.force(false);
		} catch (IOException e) {
			e.printStackTrace();
			this.close();
			return false;
		}
		byteBuffer.clear();		
		
		if(this.fileSizeLeft <= 0){// 传完，关闭文件
			this.close();
		}
		
		return true;
	}
	
	/**
	 * 
	 * <li>方法名：finishWrite
	 * <li>@return
	 * <li>返回类型：boolean
	 * <li>说明：是否完成文件的写入
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public boolean finishWrite(){
		return this.fileSizeLeft > 0 ? false : true;
	}
	
	/**
	 * 
	 * <li>方法名：close
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：关闭文件流
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-9
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void close(){
		try{
			this.fileChannel.close();
			this.fileOutputStream.close();
		}catch(Exception e){
			e.printStackTrace();
			
			try{
				this.fileChannel.close();
				this.fileOutputStream.close();
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}		
	}
	
	/**
	 * <li>方法名：forceClose
	 * <li>
	 * <li>返回类型：void
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-13
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void forceClose(){
		try{
			this.fileChannel.close();
			this.fileOutputStream.close();
		}catch(Exception e){
			e.printStackTrace();
			
			try{
				this.fileChannel.close();
				this.fileOutputStream.close();
			}catch(Exception e1){
				e1.printStackTrace();
			}
		}finally{
			this.file.delete();// 丢弃损害的文件
		}	
	}
	

	public String getFileName() {
		return fileName;
	}

	public boolean isReadHead() {
		return isReadHead;
	}

	public void setReadHead(boolean isReadHead) {
		this.isReadHead = isReadHead;
	}

	public Integer getSizeWithHead() {
		return sizeWithHead;
	}

	public void setSizeWithHead(Integer sizeWithHead) {
		this.sizeWithHead = sizeWithHead;
	}
	
	
}
