/**
 * <li>文件名：Config.java
 * <li>说明：
 * <li>创建人： CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
package com.jason;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.jason.util.MyStringUtil;

/**
 * <li>类型名称：
 * <li>说明：服务器参数配置文件
 * <li>创建人： CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2011-11-18
 * <li>修改人： 
 * <li>修改日期：
 */
public class Config {
	private static Log log = LogFactory.getLog(Config.class);// 日志记录器
	private static final String CONFIG_FILE = "config.properties";// 数据库连接池配置文件，支持配置多个数据库链接
	private static Properties props = new Properties();// 属性	
	
	static{
		// 读取配置文件，根据配置文件创建配置信息		
		try {
			InputStream fileInput =Config.class.getResourceAsStream(CONFIG_FILE);
			
			// 加载配置文件
			props.load(fileInput);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			log.info("配置文件不存在，请仔细检查");
		} catch (IOException e) {
			e.printStackTrace();
			log.info("配置文件读取错误");
		}
	}
	
	/**
	 * 
	 * <li>方法名：getProperty
	 * <li>@param key
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：获取指定配置信息
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String getStr(String key){
		return props.getProperty(key);
	}
	
	/**
	 * 
	 * <li>方法名：getLong
	 * <li>@param key
	 * <li>@return
	 * <li>返回类型：Long
	 * <li>说明：根据指定的属性获取值为Long的属性
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Long getLong(String key){
		String str = props.getProperty(key);
		if(MyStringUtil.isBlank(str.trim())){
			return null;
		}
		
		return Long.valueOf(str.trim()); 
	}
	
	/**
	 * 
	 * <li>方法名：getInt
	 * <li>@param key
	 * <li>@return
	 * <li>返回类型：Integer
	 * <li>说明：根据指定的属性获取为Integer类型的值
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-11-18
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Integer getInt(String key){
		String str = props.getProperty(key);
		if(MyStringUtil.isBlank(str.trim())){
			return null;
		}
		
		return Integer.valueOf(str.trim()); 
	}
	
	/**
	 * 
	 * <li>方法名：getBoolean
	 * <li>@param key
	 * <li>@return
	 * <li>返回类型：Boolean
	 * <li>说明：获取boolean型参数，0为真，非0为假
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2012-2-29
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Boolean getBoolean(String key){
		String str = props.getProperty(key);
		if(MyStringUtil.isBlank(str)){
			return false;
		}
		
		return Integer.valueOf(str.trim()) > 0 ? true : false;
	}
}
