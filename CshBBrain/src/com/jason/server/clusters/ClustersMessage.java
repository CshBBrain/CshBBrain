/**
 * <li>文件名：MessageFrame.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-9-12
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.clusters;

import java.nio.ByteBuffer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.jason.util.CoderUtils;

/**
 * <li>类型名称：
 * <li>说明：cluster协议消息帧格式
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-9-12
 * <li>修改人： 
 * <li>修改日期：
 */
public class ClustersMessage{
	private static Log log = LogFactory.getLog(ClustersMessage.class);// 日志记录器
	public static final byte FIN = (byte) 0x80; // 1000 0000
	public static final byte RSV1 = 0x70; // 0111 0000
	public static final byte RSV2 = 0x30; // 0011 0000
	public static final byte RSV3 = 0x10; // 0001 0000
	public static final byte OPCODE = 0x0F;// 0000 1111
	public static final byte MASK = (byte) 0x80;// 1000 0000
	public static final byte PAYLOADLEN = 0x7F;// 0111 1111
	public static final byte HAS_EXTEND_DATA = 126;
	public static final byte HAS_EXTEND_DATA_CONTINUE = 127;
	
	public static final byte TXT = 0x01;// 0000 0001
	public static final byte CLOSE = 0x08;// 0000 1000

	private byte fin;// 1bit
	private byte rsv1 = 0;// 1bit
	private byte rsv2 = 0;// 1bit
	private byte rsv3 = 0;// 1bit
	private byte opcode = 1;// 4bit
	private byte mask;// 1bit
	private byte payloadLen = 1;// 7bit解决半包时，只读取到消息帧一个字节的情况
	private short payloadLenExtended = 0;// 16bit
	private long payloadLenExtendedContinued = 0L;// 64bit
	private byte[] maskingKey = null;// 32bit

	private byte[] payloadData;
	
	private int readCount = 0;// 已经读取的消息头字节数量
	private boolean readFinish = false;//是否读取完毕，默认否
	private byte[] headers = new byte[8];// 数据帧的头部信息
	private int dataLengthByte = 0;//表示数据长度的字节数量

	public boolean isReadFinish() {
		return readFinish;
	}

	public void setReadFinish(boolean readFinish) {
		this.readFinish = readFinish;
	}
	
	public ClustersMessage(){
		
	}

	public byte getFin() {
		return fin;
	}

	public void setFin(byte fin) {
		this.fin = fin;
	}

	public byte getMask() {
		return mask;
	}
	
	public boolean isMask() {
		return 0 == (mask ^ MASK);
	}

	public void setMask(byte mask) {
		this.mask = mask;
	}

	public byte[] getMaskingKey() {
		return maskingKey;
	}

	public void setMaskingKey(byte... maskingKey) {
		this.maskingKey = maskingKey;
	}

	public byte getOpcode() {
		return opcode;
	}

	public void setOpcode(byte opcode) {
		this.opcode = opcode;
	}

	public byte[] getPayloadData() {
		return payloadData;
	}

	public void setPayloadData(byte[] payloadData) {
		this.payloadData = payloadData;
	}

	public byte getPayloadLen() {
		return payloadLen;
	}

	public void setPayloadLen(byte payloadLen) {
		this.payloadLen = payloadLen;
	}

	public short getPayloadLenExtended() {
		return payloadLenExtended;
	}

	public void setPayloadLenExtended(short payloadLenExtended) {
		this.payloadLenExtended = payloadLenExtended;
	}

	public long getPayloadLenExtendedContinued() {
		return payloadLenExtendedContinued;
	}

	public void setPayloadLenExtendedContinued(long payloadLenExtendedContinued) {
		this.payloadLenExtendedContinued = payloadLenExtendedContinued;
	}

	public byte getRsv1() {
		return rsv1;
	}

	public void setRsv1(byte rsv1) {
		this.rsv1 = rsv1;
	}

	public byte getRsv2() {
		return rsv2;
	}

	public void setRsv2(byte rsv2) {
		this.rsv2 = rsv2;
	}

	public byte getRsv3() {
		return rsv3;
	}

	public void setRsv3(byte rsv3) {
		this.rsv3 = rsv3;
	}

	/**
	 * 
	 * <li>方法名：getDateLength
	 * <li>@return
	 * <li>返回类型：long
	 * <li>说明：获取数据长度
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public long getDateLength() {
		if (this.getPayloadLenExtendedContinued() > 0){
			return this.getPayloadLenExtendedContinued();
		}
		
		if (this.getPayloadLenExtended() > 0){
			return this.getPayloadLenExtended();
		}
		if (this.getPayloadLen() == HAS_EXTEND_DATA || this.getPayloadLen() == HAS_EXTEND_DATA_CONTINUE){
			return 0l;
		}
		
		return this.getPayloadLen();
	}
	
	/**
	 * 
	 * <li>方法名：setDateLength
	 * <li>@param len
	 * <li>返回类型：void
	 * <li>说明：设置数据长度
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void setDateLength(long len) {
		if (len < HAS_EXTEND_DATA) {
			this.payloadLen = (byte) len;
			this.payloadLenExtended = 0;
			this.payloadLenExtendedContinued = 0;
		} else if (len < 1 * Short.MAX_VALUE * 2) {// UNSIGNED
			this.payloadLen = HAS_EXTEND_DATA;
			this.payloadLenExtended = (short) len;
			this.payloadLenExtendedContinued = 0;
		} else {
			this.payloadLen = HAS_EXTEND_DATA_CONTINUE;
			this.payloadLenExtended = 0;
			this.payloadLenExtendedContinued = len;
		}
	}
	
		/**
	 * 
	 * <li>方法名：computeCount
	 * <li>@param buffer
	 * <li>@param count
	 * <li>@return
	 * <li>返回类型：int
	 * <li>说明：计算获取数据的长度
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-11-27
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public int computeCount(ByteBuffer buffer, int count){
		return (buffer.remaining() >= count) ? count : buffer.remaining();
	}
	
	/**
	 * 
	 * <li>方法名：parseMessageHeader
	 * <li>@param buffer
	 * <li>返回类型：void
	 * <li>说明：解析消息头部信息
	 * <li>创建人：CshBBrain, 技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-11-27
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public void parseMessageHeader(ByteBuffer buffer){		   
		int bt, b2;		
		
		switch(this.readCount){
		case 0://没有读取过字节
			if(buffer.remaining() > 0){
				 bt = buffer.get();
				 ++this.readCount;
				 
				this.setFin((byte) (bt & FIN));// 后面是否有续帧数据标识
				this.setRsv1((byte) (bt & RSV1));// 保留标识1
				this.setRsv2((byte) (bt & RSV2));// 保留标识2
				this.setRsv3((byte) (bt & RSV3));// 保留标识3
				this.setOpcode((byte) (bt & OPCODE));//标识数据的格式，以及帧的控制，如：01标识数据内容是 文本，08标识：要求远端去关闭当前连接。
			}else{
				return;
			}			
		case 1://读取过一个字节
			if(buffer.remaining() > 0){
				 bt = buffer.get();
				 ++this.readCount;
				 
				 this.setMask((byte) (bt & MASK));// 是否mask标识
					
				/*如果小于126 表示后面的数据长度是 [Payload len] 的值。（最大125byte） 
			          等于 126 表示之后的16 bit位的数据值标识数据的长度。（最大65535byte） 
			          等于 127 表示之后的64 bit位的数据值标识数据的长度。（一个有符号长整型的最大值）*/
				this.setDateLength(bt & PAYLOADLEN);// 数据长度位数
			}else{
				return;
			}			
		case 2://读取过2个字节
		case 3:
			if(this.getDateLength() == HAS_EXTEND_DATA) {// read next 16 bit
				this.dataLengthByte = 2;// 数据字节长度为2个字节
				int count = this.computeCount(buffer, (2 - (this.readCount -2)));// 2个字节 减去（总共读取的字节数-2个字节）
				if(count <= 0){
					return;
				}
				
				buffer.get(headers, (this.readCount -2), count);// 读取2位数字
				
				this.readCount += count;
				if(this.readCount - 2 >= 2){
					bt = headers[0];
					b2 = headers[1];
					this.setDateLength(CoderUtils.toLong((byte)bt, (byte)b2));
				}else{
					return;
				}
			}
		case 4:
		case 5:
		case 6:
		case 7:
		case 8:
		case 9:		
			if (this.getDateLength() == HAS_EXTEND_DATA_CONTINUE) {// read next 32 bit
				this.dataLengthByte = 8;// 数据字节长度为2个字节
				int count = this.computeCount(buffer, (8 - (this.readCount -2)));// 2个字节 减去（总共读取的字节数-2个字节）
				if(count <= 0){
					return;
				}
				
				buffer.get(headers, (this.readCount -2), count);// 读取2位数字
				
				this.readCount += count;
				if(this.readCount - 2 >= 8){
					this.setDateLength(CoderUtils.toLong(headers));
				}else{
					return;
				}
			}
		case 10:
		default:
			if (this.isMask()){
				int count = this.computeCount(buffer, (4 - (this.readCount - 2 - this.dataLengthByte)));// 2个字节 减去（总共读取的字节数-2个字节）
				if(count <= 0){
					return;
				}
				
				buffer.get(headers, (this.readCount - 2 - this.dataLengthByte), count);// 读取2位数字
				
				this.readCount += count;
				if((this.readCount - 2 - this.dataLengthByte) >= 4){
					this.setMaskingKey(headers[0],headers[1],headers[2],headers[3]);
					this.readFinish = true;
				}else{
					return;
				}				
			}else{
				this.readFinish = true;// 读取完毕
			}
		}
	}
}
