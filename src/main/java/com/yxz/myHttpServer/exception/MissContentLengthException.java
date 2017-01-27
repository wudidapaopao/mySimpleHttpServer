package com.yxz.myHttpServer.exception;
/**
* @author Yu 
* 2016年12月2日 下午11:10:45
* http报文单行过长异常
*/
public class MissContentLengthException extends RuntimeException{
	
	public MissContentLengthException(String msg) {
		super(msg);
	}
	
}
