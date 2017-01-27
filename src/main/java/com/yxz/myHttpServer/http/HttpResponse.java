package com.yxz.myHttpServer.http;
/**
* @author Yu 
* 2016年11月29日 下午9:35:17
* http响应包装类
*/
public class HttpResponse {
	
	private HttpVersion httpVersion;
	private HttpStatus httpStatus;
	private HttpHeaders httpHeaders;
	private HttpBody httpBody;
	
	public HttpBody getHttpBody() {
		return httpBody;
	}
	public void setHttpBody(HttpBody httpBody) {
		this.httpBody = httpBody;
	}
	public HttpVersion getHttpVersion() {
		return httpVersion;
	}
	public void setHttpVersion(HttpVersion httpVersion) {
		this.httpVersion = httpVersion;
	}
	public HttpStatus getHttpStatus() {
		return httpStatus;
	}
	public void setHttpStatus(HttpStatus httpStatus) {
		this.httpStatus = httpStatus;
	}
	public HttpHeaders getHttpHeaders() {
		return httpHeaders;
	}
	public void setHttpHeaders(HttpHeaders httpHeaders) {
		this.httpHeaders = httpHeaders;
	}
	
}
