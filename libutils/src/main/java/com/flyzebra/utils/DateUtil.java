package com.flyzebra.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class DateUtil {
	public static String ymdhms="yyyyMMddHHmmss";

	public static String getCurrentDate(){
		SimpleDateFormat sdf = new SimpleDateFormat(ymdhms, Locale.getDefault());
		Date date = new Date(System.currentTimeMillis());
		return sdf.format(date);
	}

	public static String getCurrentDate(String dateFormat){
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat,Locale.getDefault());
		Date date = new Date(System.currentTimeMillis());
		return sdf.format(date);
	}
	
	public static String DateAdd(String StrDate,int add_num, String dateFormat){
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat,Locale.getDefault());		
		try {
			Date date = sdf.parse(StrDate);
			Calendar calendar = Calendar. getInstance(); 
			calendar.setTime(date);
			calendar.add(Calendar.DAY_OF_YEAR , add_num);
			date = calendar.getTime();
			StrDate = sdf.format(date);
		} catch (ParseException e) {
			e.printStackTrace();
		}		
		return StrDate;
	}
	
	public static Date StringToDate(String StrDate,String dateFormat){
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat,Locale.getDefault());		
		try {
			return sdf.parse(StrDate);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static String DateToString(Date date,String dateFormat){
		SimpleDateFormat sdf = new SimpleDateFormat(dateFormat,Locale.getDefault());
		return sdf.format(date);
	}

}
