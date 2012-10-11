package com.jason.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.UnsupportedCharsetException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

/**
 * 
 * <li>类型名称：
 * <li>说明：公司自己编写的特有的字符串处理工具。
 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
 * <li>创建日期：2008-10-14
 * <li>修改人： 
 * <li>修改日期：
 */
public class MyStringUtil {
	public static SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	public static SimpleDateFormat formatTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
	private static final Pattern PARAM_PATTERN = Pattern.compile("([^=]*)=([^&]*)&*");
	/**
	 * <li>方法名：Long2Int
	 * <li>@param ldata
	 * <li>@return
	 * <li>返回类型：int
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static int Long2Int(long ldata)
	{	
		return Integer.parseInt(String.valueOf(ldata));
	}
	
	/**
	 * <li>方法名：Int2Long
	 * <li>@param ldata
	 * <li>@return
	 * <li>返回类型：int
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-11-10
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Long Int2Long(Integer intS)
	{	
		return Long.parseLong(intS.toString());
	}
	
	/**
	 * <li>方法名：Str2Long
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：Long
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-11-10
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Long Str2Long(String str){
		if(isBlank(str)){
			return 0l;
		}
		
		return Long.parseLong(str.trim());
	}
	            
	
	public static String getDatetime(Date date){
		return format.format(date);
	}
	/**
	 * <li>方法名：string2BigDecimal
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：BigDecimal
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static BigDecimal String2BigDecimal(String str) 
	{
		BigDecimal bigDecimal = null;
		if (str != null && str.trim().length() != 0)
		{
			bigDecimal = new BigDecimal(str);
		}
		return bigDecimal;
	}
	
	/**
	 * <li>方法名：DateParse
	 * <li>@param str
	 * <li>@return
	 * <li>@throws Exception
	 * <li>返回类型：Date
	 * <li>说明：字符格式转换为日期类型
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-11-25
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Date DateParse(String str){
		if(MyStringUtil.isBlank(str)){
			return null;
		}
		
		try{
			return format.parse(str);
		}catch(Exception e){
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * <li>方法名：date2str
	 * <li>@param date
	 * <li>@param formatString
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String Date2Str(java.util.Date date, String formatString)
	{
		if(formatString!=null && !formatString.equals("")){
			format = new SimpleDateFormat(formatString);
		}
		return format.format(date);
	}
	
	/**
	 * <li>方法名：DateFormat
	 * <li>@param date
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：日期格式化为yyyy-MM-dd形式
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-11-25
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String DateFormat(Date date){
		return format.format(date);
	}
	
	/**
	 * <li>方法名：Str2Date
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：Date
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Date Str2Date(String str){
		Date date = null;
		try{
			if(str != null && !str.trim().equals("") && isDate(str))
				date = format.parse(str);
		}catch(Exception e){
		}
		return date;
	}
	
	
	public static Date Str2Date(String str, String formatString){
		if(isBlank(formatString)){
			formatString = "yyyy-MM-dd HH:mm:ss";
		}
		SimpleDateFormat format=new SimpleDateFormat(formatString.split(";")[0]);
		Date date = null;
		try{
			if(!isBlank(str))
				date = format.parse(str);
		}catch(Exception e){
			e.printStackTrace();
			return new Date();			
		}
		return date;
	}
	
	
	/**
	 * <li>方法名：isDate
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：boolean
	 * <li>说明：判断是否是日期字符串
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static boolean isDate(String str){
		try{
			if(!isBlank(str))
				format.parse(str);
		}catch(Exception e){
			return false;
		}
		return true;
	}
	
	/**
	 * <li>方法名：Str2Int
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：int
	 * <li>说明：数据类型转换
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static int Str2Int(String str)
	{
		if (str == null || "".equals(str))
			return 0;
		return Integer.parseInt(str);
	}
	
	/**
	 * <li>方法名：StrFill
	 * <li>@param fillStr 用来补位的字符
	 * <li>@param oldStr 需要补位的字符串
	 * <li>@param length 补位后的总长度
	 * <li>@param place 补位位置:left or right
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：用指定的字符为需要补位的字符串补位
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-10-21
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String StrFill(String fillStr ,String oldStr ,int length ,String place)
	{
		StringBuffer sb =  new StringBuffer();
		if("right".equals(place)){
			sb.append(oldStr);
		}
		for(int i=0; i < (length - oldStr.length());i++){
			sb.append(fillStr);
		}
		if("left".equals(place)){
			sb.append(oldStr);
		}
		return sb.toString();
	}
	
	/**
	 * <li>方法名：isBlank
	 * <li>@param str
	 * <li>@return
	 * <li>返回类型：boolean
	 * <li>说明：判断字符串是否为空,为空就返回true,不为空返回false
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2008-11-26
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static boolean isBlank(String str){
		if(str==null){
			return true;
		}
		if(str.trim().length()<1){
			return true;
		}
		
		if(str.trim().equals("")){
			return true;
		}
		
		if(str.trim().toLowerCase().equals("null")){
			return true;
		}
		return false;
	}
	
	/**
	 * 
	 * <li>方法名：getURL
	 * <li>@param entityName
	 * <li>@param menuId
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：根据指定的实体类名和方法名组合出url
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2009-9-15
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String getURL(String entityName,String methodName,String paramers,String url){
		StringBuilder sb = new StringBuilder();
		if(!isBlank(entityName)){
			sb.append(entityName).append("!").append(methodName);
		}else if(!isBlank(url)){
			sb.append(url);
		}
		
		if(!isBlank(paramers)){
			sb.append("?").append(paramers);
		}
		return sb.toString();
	}

	public static String toUTF8(String str){
		if(str == null){
			return null;
		}
		StringBuffer sb = new StringBuffer();
		for (int i=0; i< str.length(); i++) {
			char c = str.charAt(i);
			if(c >= 0 && c <= 256){
				sb.append(c);
			}
			else{
				try{
					byte[] b = Character.toString(c).getBytes("UTF-8");
					for (int j = 0; j < b.length; j++) {
						int k = b[j];
						if(k<0){
							k = k + 256;
						}
						sb.append("%" + Integer.toHexString(k).toUpperCase());
					}
				}
				catch (Exception e) {
					System.out.println(e);
				}
			}
		}
		
		return sb.toString();
	}
	
	public static String decodeUTF8(String s) {
		if (s == null)
			return "";

		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			switch (c) {
			case '+':
				sb.append(' ');
				break;
			case '%':
				try {
					// 将16进制的数转化为十进制
					sb.append((char) Integer.parseInt(
							s.substring(i + 1, i + 3), 16));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException();
				}
				i += 2;
				break;
			default:
				sb.append(c);
				break;
			}
		}

		String result = sb.toString();
		try {
			result = new String(result.getBytes("8859_1"),"UTF-8");
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		return result;
	}
	
	public static String addBrackets(String value){
		return new StringBuilder().append("[").append(value).append("]").toString();
	}
	
	/**
	 * 
	 * <li>方法名：toHqlInConditionWithName
	 * <li>@param alias
	 * <li>@param puriveCode
	 * <li>@param propertName
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：组织in条件sql串
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2010-2-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String toHqlInConditionWithName(String alias,String puriveCode,String propertName){
		String[] conditions = puriveCode.split(",");
		if(isBlank(alias)){
			alias = "";
		}else{
			alias += ".";
		}
		StringBuilder sb = new StringBuilder();
		sb.append(alias).append(propertName).append(" in ( ");
		for(String s: conditions){
			if(s != null){
				sb.append("'").append(s).append("',");
			}
		}
		sb.deleteCharAt(sb.length() -1 );
		sb.append(" )");
		return sb.toString();
	}
	
	/**
	 * 
	 * <li>方法名：toHqlOrConditionWithName
	 * <li>@param alias
	 * <li>@param puriveCode
	 * <li>@param propertName
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：组织or条件sql串
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2010-2-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String toHqlOrConditionWithName(String alias,String puriveCode,String propertName){
		boolean hasElement = false;
		if(isBlank(alias)){
			alias = "";
		}else{
			alias += ".";
		}
		String[] conditions = puriveCode.split(",");
		StringBuilder sb = new StringBuilder();	
		sb.append(" ( ");
		for(String s: conditions){
			if(!isBlank(s)){
				hasElement = true;
				sb.append(alias).append(propertName).append("= '").append(s).append("' or ");
			}
		}
		sb.delete(sb.length() - 3,sb.length() - 1 );
		sb.append(" ) ");
		
		if(hasElement){
			return sb.toString();
		}else{
			return null;
		}
	}
	
	/**
	 * 
	 * <li>方法名：toHqlOrConditionWithName
	 * <li>@param alias
	 * <li>@param propertName
	 * <li>@param puriveCode
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2010-2-2
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String toHqlOrConditionWithName(String alias,String propertName,String ...puriveCode){
		boolean hasElement = false;
		if(isBlank(alias)){
			alias = "";
		}else{
			alias += ".";
		}
		StringBuilder sb = new StringBuilder();	
		sb.append(" ( ");
		for(String s: puriveCode){
			if(!isBlank(s)){
				hasElement = true;
				sb.append(alias).append(propertName).append("= '").append(s).append("' or ");
			}
		}
		sb.delete(sb.length() - 3,sb.length() - 1 );
		sb.append(" ) ");
		
		if(hasElement){
			return sb.toString();
		}else{
			return null;
		}
	}
	
	public static String likePurviewCode(String alias,String puriveCode,String columnName){
		if(puriveCode == null){
			return "";
		}
		if(isBlank(alias)){
			alias = "";
		}else{
			alias += ".";
		}	
		
		StringBuilder sb = new StringBuilder();			
		sb.append(" ( ");
		sb.append(alias).append(columnName).append(" like '").append(puriveCode).append("%'");
		sb.append(" ) ");
		
		return sb.toString();
	}
	
	public static String joinStrings(String...strs){
		if(strs!=null){
			StringBuilder sb = new StringBuilder();
			for(String str:strs){
				sb.append(str);
			}
			return sb.toString();
		}else{
			return "";
		}
	}
	
	public static String splitCasCode(String casCode){
		String result = "";
		if(isBlank(casCode)){
			return result;
		}
		for(int i =0; i <casCode.length()/3;++i){
			result += "," + casCode.substring(i*3, (i+1)*3);
		}
		
		return result;
	}
	
	/**
	 * 
	 * <li>方法名：createStaticsDate
	 * <li>@return
	 * <li>返回类型：Date
	 * <li>说明：获取广告统计日期
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-5
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Date createStaticsDate(){
		Date d = new Date();
		d.setHours(1);
		
		return d;
	}
	
	/**
	 * 
	 * <li>方法名：dateIncrense
	 * <li>@param staticsDate
	 * <li>@return
	 * <li>返回类型：Boolean
	 * <li>说明：判断缓存中的广告统计时间是否为当天
	 * <li>创建人：CshBBrain;技术博客：http://cshbbrain.iteye.com/
	 * <li>创建日期：2011-12-5
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static Boolean dateIncrense(Date staticsDate){
		Date d = new Date();
		Date d1 = new Date();
		Date d2 = new Date();
		d1.setHours(0);
		d1.setMinutes(0);
		d1.setSeconds(0);
		
		d2.setHours(23);
		d2.setMinutes(59);
		d2.setSeconds(59);
		
		if(staticsDate.after(d1) && staticsDate.before(d2)){
			return false;
		}
		
		return true;
	}
	/**
	 * <li>方法名：long2date
	 * <li>@param datelong
	 * <li>@param format
	 * <li>@return 
	 * <li>返回类型：String
	 * <li>说明：将long型，转为时间
	 * <li>创建人：袁军
	 * <li>创建日期：Feb 29, 2012
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String long2date(long datelong, String format) {
		if(isBlank(format)){
			format = "yyyy-MM-dd HH:mm:ss";
		}
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		Date date = new Date();
		date.setTime(datelong);
		return sdf.format(date);
	}

	public static void main(String[] params){
		String str = "您还阿富汗123！";
		System.out.println(str);
		str = toUTF8(str);
		System.out.println(str);
		str = decodeUTF8(str);
		System.out.println(str);
		System.out.println(getWeekOfDate(new Date()));
		
//		String a ="akdslkafk,alsdlksfk,2dadaf,849949,84399,,,";
	}
	
	public static String arrayToString(Collection<String> colls){
		return colls.toString().replace(",", "','").replace("[", "('").replace("]", "')").replace(" ", "");
	}
	
	public static String fixBitCode(Serializable souce,Integer bits){		
		return StringUtils.leftPad(souce.toString(), bits, '0');
	}
	/**
	 * <li>方法名：getWeekOfDate
	 * <li>@param date
	 * <li>@return
	 * <li>返回类型：String
	 * <li>说明：根据日期得到周
	 * <li>创建人：袁军
	 * <li>创建日期：Jul 31, 2012
	 * <li>修改人： 
	 * <li>修改日期：
	 */
	public static String getWeekOfDate(Date date) { 
	  String[] weekDaysName = { "星期日", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六" }; 
	  //String[] weekDaysCode = { "0", "1", "2", "3", "4", "5", "6" }; 
	  Calendar calendar = Calendar.getInstance(); 
	  calendar.setTime(date); 
	  int intWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; 
	  return weekDaysName[intWeek]; 
	} 

	/**
	 * 
	 * <li>方法名：decodeParams
	 * <li>@param msg
	 * <li>@param requestData
	 * <li>返回类型：void
	 * <li>说明：解析请求参数键值对
	 * <li>创建人：CshBBrain
	 * <li>创建日期：2011-11-25
	 * <li>修改人： 
	 * <li>修改日期：
	 */
    public static HashMap<String,String> parseKeyValue(String msg){    	
    	if(isBlank(msg)){
            return null;
        }
    	
    	String values = null;
        try{
        	values = URLDecoder.decode(msg, CoderUtils.UTF8);
        }catch(UnsupportedEncodingException e){
            throw new UnsupportedCharsetException(CoderUtils.UTF8);
        }
        
        HashMap<String,String> requestData = new HashMap<String,String>();
    	
        Matcher m = PARAM_PATTERN.matcher(values);
        int pos = 0;
        while (m.find(pos)) {
            pos = m.end();            
            requestData.put(m.group(1), m.group(2)); 
        }
        
        return requestData;
    }
}
