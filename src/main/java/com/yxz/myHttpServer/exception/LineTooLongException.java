package com.yxz.myHttpServer.exception;
/**
* @author Yu 
* 2016年12月2日 下午11:10:45
* http报文单行过长异常
*/
public class LineTooLongException extends RuntimeException{
	
	public LineTooLongException(String msg) {
		super(msg);
	}
	
}