/**
 * <li>文件名：MessageFrame.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-9-12
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2012-9-12
 * <li>修改人： 
 * <li>修改日期：
 */
public class WebSocketMessage{
	private static Log log = LogFactory.getLog(WebSocketMessage.class);// 日志记录器
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
	private byte payloadLen = 0;// 7bit
	private short payloadLenExtended = 0;// 16bit
	private long payloadLenExtendedContinued = 0L;// 64bit
	private byte[] maskingKey = null;// 32bit

	private byte[] payloadData;
	
	public WebSocketMessage(){
		
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
	
	
}
