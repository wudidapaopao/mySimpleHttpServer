package com.yxz.myHttpServer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.SelectionKey;
import java.util.ArrayList;
import java.util.List;

import com.yxz.myHttpServer.http.HttpBody;
import com.yxz.myHttpServer.http.HttpConstants;
import com.yxz.myHttpServer.http.HttpCookie;
import com.yxz.myHttpServer.http.HttpHeader;
import com.yxz.myHttpServer.http.HttpHeaders;
import com.yxz.myHttpServer.http.HttpRequest;
import com.yxz.myHttpServer.http.HttpResponse;
import com.yxz.myHttpServer.http.HttpStatus;
import com.yxz.myHttpServer.utils.GZipUtils;

/**
* @author Yu 
* http响应编码
*/
public class ResponseHandler {
	
	private HttpRequest httpRequest;
	private HttpResponse httpResponse;
	
	private RequestHandler requestHandler;
	private WrappedChannel wrappedChannel;
	private String filePrefix;
	private boolean encodingEnabled;
	private boolean chunked;

	private static final String COMPRESS = "gzip";
	private static final String TRANSFER = "chunked";
	private static final String NOT_FOUND = "page is not found!";
	
	public ResponseHandler(HttpRequest httpRequest, String filePrefix, WrappedChannel wrappedChannel, RequestHandler requestHandler, boolean chunked) {
		this.requestHandler = requestHandler;
		this.wrappedChannel = wrappedChannel;
		this.httpRequest = httpRequest;
		this.filePrefix = filePrefix;
		this.encodingEnabled = false;
		this.chunked = chunked;
	}
	
	public boolean isEncodingEnabled() {
		return encodingEnabled;
	}
	public void setEncodingEnabled(boolean encodingEnabled) {
		this.encodingEnabled = encodingEnabled;
	}
	public boolean isChunked() {
		return chunked;
	}
	public void setChunked(boolean chunked) {
		this.chunked = chunked;
	}
	
	/*
	 * 构建http响应报文
	 */
	public void buildHttpResponse() {
		httpResponse = new HttpResponse();
		buildLine();
		buildHeaders();
		buildBody();
	}
	
	private void buildLine() {
		httpResponse.setHttpVersion(httpRequest.getHttpVersion());
		httpResponse.setHttpStatus(HttpStatus._200);
	}

	private void buildHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		if(this.encodingEnabled && setEncoding()) { //Content-Encoding首部
			httpHeaders.addHeader(new HttpHeader(HttpHeader.CONTENT_ENCODING, COMPRESS));
		}
		else {
			this.encodingEnabled = false;
		}
		httpHeaders.addHeader(new HttpHeader(HttpHeader.CONTENT_TYPE, "text/html")); //Content-Type首部
		if(this.chunked) {
			httpHeaders.addHeader(new HttpHeader(HttpHeader.TRANSFER_ENCODING, TRANSFER)); //Transfer-Encoding首部
		}
		//httpHeaders.addHeader(new HttpCookie(name, value, domain, path, expire, secure)); // Set-Cookie首部
		this.httpResponse.setHttpHeaders(httpHeaders);
	}
	
	private void buildBody() {
		if(this.chunked) { //chunked分块传输
			buildChunked();
		}
		else if(filePrefix != null) { //常规传输
    		String urlPath = httpRequest.getUrl();
    		urlPath = filePrefix + File.separator +urlPath;
    		if(this.encodingEnabled) {
    			urlPath = urlPath.substring(0, urlPath.lastIndexOf('.') + 1);
    			urlPath += COMPRESS;
    		}
    		File file = new File(urlPath);
    		if(file != null && file.exists() && file.isFile()) {
    			HttpBody httpBody = new HttpBody();
    			httpBody.setFile(file);
    			httpResponse.setHttpBody(httpBody);
    			int len = (int) file.length();
        		String length = String.valueOf(len);
        		HttpHeader httpHeader = new HttpHeader(HttpHeader.CONTENT_LENGTH, length); //Content-Length首部
        		httpResponse.getHttpHeaders().addHeader(httpHeader);
    		}
    		else {
    			httpResponse.setHttpStatus(HttpStatus._404);
    			HttpHeader httpHeader = new HttpHeader(HttpHeader.CONTENT_LENGTH, "0");
    		}
    		wrappedChannel.setFinished(true);
		}
	}
	
	/*
	 * 分块传输动态增加chunk
	 */
	public void addChunked(String chunk, boolean finished) {
		String len = Integer.toHexString(chunk.length()); //chunk的大小要用十六进制字符串表示
		StringBuilder sb = new StringBuilder(len);
		addNewLine(sb);
		sb.append(chunk);
		addNewLine(sb);
		this.wrappedChannel.addSendBuffer(new DefaultSendBuffer(sb.toString().getBytes()));
		if(finished) {
			sb = new StringBuilder("0");
			addNewLine(sb);
			addNewLine(sb);
			this.wrappedChannel.addSendBuffer(new DefaultSendBuffer(sb.toString().getBytes()));
			this.wrappedChannel.setFinished(true);
		}
	}
	
	private void buildChunked() {
		byte[] bytes = (buildMessage()).getBytes();
		this.wrappedChannel.addSendBuffer(new DefaultSendBuffer(bytes));
		this.requestHandler.setInterestOp(SelectionKey.OP_WRITE);
		wrappedChannel.getPoller().wakeup();
	}

	//查看http请求中是否支持服务端的编码方式gzip
	private boolean setEncoding() {
		String acceptEncodoing = httpRequest.getHttpHeaders().getHeaderValue(HttpHeader.ACCEPT_ENCODING);
		String[] encodings = acceptEncodoing.split(",");
		for(String encoding : encodings) {
			if(encoding.trim().equalsIgnoreCase(COMPRESS)) {
				return true;
			}
		}
		return false;
	}

	/*
	 * 发送http响应报文
	 */
	public List<SendBuffer> sendResponse() {
		List<SendBuffer> lists = new ArrayList<>();
		HttpBody httpBody = httpResponse.getHttpBody();
		File file = null;
		byte[] bytes = null;
		if(httpBody == null) { //404
			byte[] gbytes = null;
			if(this.encodingEnabled) {
				try {
					gbytes = GZipUtils.gzip(NOT_FOUND.getBytes());
				} catch (Exception e) {
					//to do
				}
				this.httpResponse.getHttpHeaders().addHeader(new HttpHeader(HttpHeader.CONTENT_LENGTH, String.valueOf(gbytes.length)));
			}
			else {
				this.httpResponse.getHttpHeaders().addHeader(new HttpHeader(HttpHeader.CONTENT_LENGTH, String.valueOf(NOT_FOUND.length())));
			}
			String message = buildMessage();
			bytes = message.getBytes();
			lists.add(new DefaultSendBuffer(bytes));
			if(this.encodingEnabled) {
				lists.add(new DefaultSendBuffer(gbytes));
			}
			else {
				lists.add(new DefaultSendBuffer(NOT_FOUND.getBytes()));
			}
			return lists;
		}
		String message = buildMessage();
		file = httpBody.getFile();
		bytes = message.getBytes();
		lists.add(new DefaultSendBuffer(bytes));
		if(file != null) {
			try {
				lists.add(new FileSendBuffer(new RandomAccessFile(file, "rw")));
			} catch (IOException e) {
				// to do 
			}
		}
		return lists;
	}
	
	//构建响应行和响应首部的字符串
	private String buildMessage() {
		StringBuilder sb = new StringBuilder();
		sb.append(httpResponse.getHttpVersion().getProtocolName());
		sb.append((char)HttpConstants.SP);
		HttpStatus httpStatus = httpResponse.getHttpStatus();
		sb.append(httpStatus.toString() + (char)HttpConstants.SP + httpStatus.getDescription());
		addNewLine(sb);
		HttpHeaders httpHeaders = httpResponse.getHttpHeaders();
		List<HttpHeader> lists = httpHeaders.getLists();
		for (HttpHeader httpHeader : lists) {
			sb.append(httpHeader.getName()).append(": ").append(httpHeader.getValue());
			addNewLine(sb);
		}
		addNewLine(sb);
		return sb.toString();
	}
	
	//crlf换行
	private void addNewLine(StringBuilder sb) {
		sb.append((char)HttpConstants.CR).append((char)HttpConstants.LF);
	}
	
}
