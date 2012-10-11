/**
 * <li>文件名：Utf8Coder.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-9-21
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.util;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;

/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-9-21
 * <li>修改人： 
 * <li>修改日期：
 */
public class CoderUtils {
	public static final char BEGIN_CHAR = 0x00;// 开始字符
	public static final char END_CHAR = 0xFF;// 结束字符
	public static final String BEGIN_MSG = String.valueOf(BEGIN_CHAR);// 消息开始
	public static final String END_MSG = String.valueOf(END_CHAR); // 消息结束
	public static final String BLANK_SPACE = " ";// 空白字符串
	public static final String BLANK_LINE = "\r\n\r\n";// 空白行
	public static final String UTF8 = "utf-8";//utf-8编码
	private static Charset charset = Charset.forName("utf-8");

	/**
	 * 
	 * <li>方法名：encode
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：ByteBuffer
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-5-3
	 * <li>修改人： 
	 * <li>修改日期：
	 */ 
	public static ByteBuffer encode(String str) {   
		return charset.encode(str);   
	}
	
	/**
	 * 
	 * <li>方法名：toByte
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：byte[]
	 * <li>说明：获取字符串的utf-8编码
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-12
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static byte[] toByte(String str){
		if(str != null){
			try {
				return str.getBytes(UTF8);
			} catch (UnsupportedEncodingException e) {
				e.printStackTrace();
				return null;
			}
		}
		return null;
	}
	
	/**
	 * 
	 * <li>方法名：toNormalByte
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：byte[]
	 * <li>说明：获取平台相关的字节编码
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static byte[] toNormalByte(String str){
		if(str != null){
			return str.getBytes();			
		}
		return null;
	}
	
	/**
	 * 
	 * <li>方法名：decode
	 * <li>@param bb
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-5-3
	 * <li>修改人： 
	 * <li>修改日期：
	 */  
	public static String decode(ByteBuffer bb){
		return charset.decode(bb).toString();
	}
	
	/**
	 * 
	 * <li>方法名：toLong
	 * <li>@param b
	 * <li>@return
	 * <li>返回类型：long
	 * <li>说明：将字节数组转换为Long型数据，高字节在前，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static long toLong(byte ... b) {
		long l = 0;
		int len = b.length;
		if (len > 0)
			l = b[0];
		if (len > 1)
			l = ((long) l <<  8 | b[1]);
		if (len > 2)
			l = ((long) l << 16 | b[2]);
		if (len > 3)
			l = ((long) l << 24 | b[3]);
		if (len > 4)
			l = ((long) l << 32 | b[3]);
		if (len > 5)
			l = ((long) l << 40 | b[3]);
		if (len > 6)
			l = ((long) l << 48 | b[3]);
		if (len > 7)
			l = ((long) l << 56 | b[3]);
		return l;
	}

	/**
	 * 
	 * <li>方法名：toInt
	 * <li>@param b
	 * <li>@return
	 * <li>返回类型：int
	 * <li>说明：将字节数组转换为整数，高字节在前，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static int toInt(byte ... b) {
		return (int)toLong(b);
	}

	/**
	 * 
	 * <li>方法名：toShort
	 * <li>@param b
	 * <li>@return
	 * <li>返回类型：short
	 * <li>说明：将字节转换为短整数，高字节在前，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static short toShort(byte ... b) {
		return (short)toLong(b);
	}
	
	/**
	 * 
	 * <li>方法名：numberToByte
	 * <li>@param l
	 * <li>@param length
	 * <li>@return
	 * <li>返回类型：byte[]
	 * <li>说明：将数字转换为字节数组；从高位向低位取值，高位在前，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	private static byte[] numberToByte(long l, int length) {
		byte[] bts = new byte[length];
		for(int i=0; i<length; i++){
			bts[i] = (byte) (l >> ((length -i - 1) * 8));
		}
		return bts;
	}
	
	/**
	 * 
	 * <li>方法名：shortToByte
	 * <li>@param i
	 * <li>@return
	 * <li>返回类型：byte[]
	 * <li>说明：短整形转换为字节数组，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static byte[] shortToByte(int i) {
		return numberToByte(i, 2);
	}
	
	/**
	 * 
	 * <li>方法名：intToByte
	 * <li>@param i
	 * <li>@return
	 * <li>返回类型：byte[]
	 * <li>说明：将整数转换为字节数组，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static byte[] intToByte(int i) {
		return numberToByte(i, 4);
	}
	
	/**
	 * 
	 * <li>方法名：longToByte
	 * <li>@param i
	 * <li>@return
	 * <li>返回类型：byte[]
	 * <li>说明：将长整形转换为字节数组，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static byte[] longToByte(long i) {
		return numberToByte(i, 8);
	}
	
	/**
	 * 
	 * <li>方法名：formatBytes
	 * <li>@param bytes
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：将字节数组转换为字符串，主要用于日志的输出，该方法来自网络
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-9-19
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String formatBytes(byte ... bytes) {
		StringBuilder sb = new StringBuilder(bytes.length * 3);
		for (byte byt : bytes) {
			sb.append(String.format("%02X ", byt));
		}
		return sb.toString();
	}
}
