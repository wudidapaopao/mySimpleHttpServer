package com.yxz.myHttpServer.exception;

/**
* @author Yu 
* http报文单行过长异常
*/
public class MissContentLengthException extends RuntimeException{
	
	public MissContentLengthException(String msg) {
		super(msg);
	}
	
}
