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
import com.yxz.myHttpServer.http.HttpHeader;
import com.yxz.myHttpServer.http.HttpHeaders;
import com.yxz.myHttpServer.http.HttpRequest;
import com.yxz.myHttpServer.http.HttpResponse;
import com.yxz.myHttpServer.http.HttpStatus;
import com.yxz.myHttpServer.utils.GZipUtils;

/**
* @author Yu 
* http响应处理类
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
	
	public void buildHttpResponse() {
		httpResponse = new HttpResponse();
		buildLine();
		buildHeaders();
		buildBody();
	}

	private void buildBody() {
		if(this.chunked) {
			buildChunked();
		}
		else if(filePrefix != null) {
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
        		HttpHeader httpHeader = new HttpHeader(HttpHeader.CONTENT_LENGTH, length);
        		httpResponse.getHttpHeaders().addHeader(httpHeader);
    		}
    		else {
    			httpResponse.setHttpStatus(HttpStatus._404);
    			HttpHeader httpHeader = new HttpHeader(HttpHeader.CONTENT_LENGTH, "0");
    		}
    		wrappedChannel.setFinished(true);
		}
	}
	
	//临时测试chunked
	private void buildChunked() {
		byte[] bytes = (buildMessage()).getBytes();
		this.wrappedChannel.addSendBuffer(new DefaultSendBuffer(bytes));
		this.requestHandler.setInterestOp(SelectionKey.OP_WRITE);
		wrappedChannel.getPoller().wakeup();
		for(int i = 0; i < 5; i++) {
			StringBuilder sb = new StringBuilder("5");
			addNewLine(sb);
			sb.append("hello");
			addNewLine(sb);
			this.wrappedChannel.addSendBuffer(new DefaultSendBuffer(sb.toString().getBytes()));
		}
		StringBuilder sb = new StringBuilder("0");
		addNewLine(sb);
		addNewLine(sb);
		this.wrappedChannel.addSendBuffer(new DefaultSendBuffer(sb.toString().getBytes()));
		this.wrappedChannel.setFinished(true);
	}

	private void buildHeaders() {
		HttpHeaders httpHeaders = new HttpHeaders();
		if(this.encodingEnabled && setEncoding()) {
			httpHeaders.addHeader(new HttpHeader(HttpHeader.CONTENT_ENCODING, COMPRESS));
		}
		else {
			this.encodingEnabled = false;
		}
		httpHeaders.addHeader(new HttpHeader(HttpHeader.CONTENT_TYPE, "text/html"));
		if(this.chunked) {
			httpHeaders.addHeader(new HttpHeader(HttpHeader.TRANSFER_ENCODING, TRANSFER));
		}
		this.httpResponse.setHttpHeaders(httpHeaders);
	}

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

	private void buildLine() {
		httpResponse.setHttpVersion(httpRequest.getHttpVersion());
		httpResponse.setHttpStatus(HttpStatus._200);
	}
	
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

	public List<SendBuffer> sendResponse() {
		List<SendBuffer> lists = new ArrayList<>();
		HttpBody httpBody = httpResponse.getHttpBody();
		File file = null;
		byte[] bytes = null;
		if(httpBody == null) {
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
	
}
