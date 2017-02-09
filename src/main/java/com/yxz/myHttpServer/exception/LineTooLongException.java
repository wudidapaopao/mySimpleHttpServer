package com.yxz.myHttpServer.exception;

/**
* @author Yu 
* http报文单行过长异常
*/
public class LineTooLongException extends RuntimeException{
	
	public LineTooLongException(String msg) {
		super(msg);
	}
	
}
