/**
 * <li>文件名：ResponseStatus.java
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-5-20
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason.server.ws.biz;

import java.nio.charset.Charset;


/**
 * <li>类型名称：
 * <li>说明：
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-5-20
 * <li>修改人： 
 * <li>修改日期：
 */
public class Constants {
	// 响应状态码
	public static final String OK_WITHOUT_RS = "1000";//成功无返回：1000
	public static final String OK_RS_STR = "1100";//成功返回字符串:1100
	public static final String OK_RS_ZIP = "1200";//成功返回zip文件:1200
	public static final String OK_RS_IMG = "1300";//成功返回图片文件:1300
	public static final String OK_RS_STR_ZIP = "1400";//返回字符串和文件，字符串长度为4位，1400
	public static final String NO_WITHOUT_RS = "2000";//失败无返回:2000
	public static final String NO_RS_STR = "2100";//失败返回字符串:2100
	public static final String FILE_NOT_EXSIT = "2500";//请求的文件不存在：2500
	public static final String FILE_DEL_SERVER = "2600";//请求的文件被删除，本地不删除：2600
	public static final String FILE_DEL_LOCAL = "2700";//请求的文件被杀出，删除本地文件：2700
	
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
}
