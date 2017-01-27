package com.yxz.myHttpServer;

import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.yxz.myHttpServer.exception.LineTooLongException;
import com.yxz.myHttpServer.exception.MissContentLengthException;
import com.yxz.myHttpServer.http.HttpConstants;
import com.yxz.myHttpServer.http.HttpHeader;
import com.yxz.myHttpServer.http.HttpMethod;
import com.yxz.myHttpServer.http.HttpRequest;
import com.yxz.myHttpServer.http.HttpVersion;

/**
* @author Yu 
* 2016年11月29日 上午2:07:12
* 编码解码调度类
*/
public class RequestHandler implements Runnable {
	
	private static final Logger logger = LoggerFactory.getLogger(RequestHandler.class);
	
	private static final int START = 1;
	private static final int LINE = 2;
	private static final int HEADER = 3;
	private static final int BODY = 4;
	private static final int FINISHED = 5;

	private byte[] bytes = new byte[8192];
	private int read;
	private int write;
	private HttpRequest httpRequest = new HttpRequest();
	private int state;	
	private int maxLineLength = 1024;
	private int maxHeaderLength = 1024;
	private LineParser lineParser = new LineParser(maxLineLength);
	private HeaderParser headerParser = new HeaderParser(maxHeaderLength);
	private BodyParser bodyParser = new BodyParser();
	private boolean chunked = true;
	
	private WrappedChannel wrappedChannel;

	public RequestHandler() {
		init();
	}
	
	private void init() {
		this.read = 0;
		this.write = 0;
		httpRequest.init();
		lineParser.init();
		headerParser.init();
		bodyParser.init();
		state = START;
	}

	public WrappedChannel getWrappedChannel() {
		return wrappedChannel;
	}

	public void setWrappedChannel(WrappedChannel wrappedChannel) {
		this.wrappedChannel = wrappedChannel;
	}
	
	@Override
	public void run() {
		try {
    		switch(state) {
    			case START: {
    				if(!skipControlCharacter())
    					return;
    				state = LINE;
    			}
    			case LINE: {
    				String line = null;
    				try {
    					line = lineParser.parse();
    				} catch(Exception e) {
    					logger.error(e.getMessage());
    					return;
    				}
    				if(line == null)
    					return;
    				String[] lines = line.split(new String() + (char)HttpConstants.SP);
    				if(lines == null || lines.length != 3) {
    					init();
    					return;
    				}
    				try {
    					doLine(lines);
    				} catch (Exception e) {
    					init();
    					return;
    				}
    				state = HEADER;
    			} 
    			case HEADER: {
    				while(true) {
        				String line = null;
        				try {
        					line = this.headerParser.parse();
        				} catch(Exception e) {
        					logger.error(e.getMessage());
        					return;
        				}
        				if(line == null) {//进入body解析
        					break;
        				}
        				String[] lines = line.split(new String() + (char)HttpConstants.COLON, 2);
        				if(lines == null || lines.length != 2) {
        					return;
        				}
        				HttpHeader httpHeader = doHeader(lines);
        				httpRequest.getHttpHeaders().addHeader(httpHeader);
        				if(state == BODY) {
        					break;
        				}
        				else if(read < write) {
        					continue;
        				}
        				else {
        					return;
        				}
    				}
    			}
    			case BODY: {
    				String body = null;
    				int len = httpRequest.getContentLength();
    				HttpMethod httpMethod = httpRequest.getHttpMethod();
    				if(httpMethod == HttpMethod.GET) {
    					// to do 
    					this.state = FINISHED;
    				}
    				else if(httpMethod == HttpMethod.POST){
    					bodyParser.setLength(len);
    					try {
    						body = this.bodyParser.parse();
    					} catch(Exception e) {
    						logger.error(e.getMessage());
    						return;
    					}
    					if(body == null)
    						return;
    					httpRequest.setRequestBody(body);
    					this.state = FINISHED;
    				}
    				else {
    					//to do
    				}
    			}
    		}
		} finally {	
			if(this.state == FINISHED) { //request完成,处理request
				httpRequest.process();
				ResponseHandler responseHandler = new ResponseHandler(httpRequest, NioHttpServer.pathPrefix, wrappedChannel, this, chunked);
				responseHandler.buildHttpResponse();
				if(!chunked) {
    				List<SendBuffer> sendBuffers;
    				sendBuffers = responseHandler.sendResponse();
    				for(int i = 0; i < sendBuffers.size(); i++) {
    					wrappedChannel.addSendBuffer(sendBuffers.get(i));
    				}
    				setInterestOp(SelectionKey.OP_WRITE);
    				wrappedChannel.getPoller().wakeup();
    				this.state = START;
    				this.httpRequest.init();
				}
			}
			else { //request未完成，继续read
    			setInterestOp(SelectionKey.OP_READ);
    			wrappedChannel.getPoller().wakeup();
			}
		}
	}
	
	void setInterestOp(int op) {
		SocketChannel socketChannel = wrappedChannel.getSocketChannel();
		Selector selector = wrappedChannel.getSelector();
		Poller poller = wrappedChannel.getPoller();
		SelectionKey key = socketChannel.keyFor(selector);
		key.interestOps(key.interestOps() | op);
	}

	private HttpHeader doHeader(String[] lines) {
		String name = lines[0];
		String value = lines[1];
		HttpHeader httpHeader = new HttpHeader(name, value);
		return httpHeader;
	}

	private void doLine(String[] lines) {
		String s1 = lines[0];
		HttpMethod method = HttpMethod.getMethod(s1);
		String s2 = lines[1];
		String s3 = lines[2];
		HttpVersion version = HttpVersion.getHttpVersion(s3);
		httpRequest.setHttpMethod(method);
		httpRequest.setUrl(s2);
		httpRequest.setHttpVersion(version);
	}

	private boolean skipControlCharacter() {
		while(read < write) {
			if(isControlCharacter(bytes[read]))
				read++;
			else
				return true;
		}
		return false;
	}

	private boolean isControlCharacter(byte b) {
		if((b >= 0 && b <= 32) || b == 127)
			return true;
		return false;
	}

	public void add(ByteBuffer bb) {
		if(bb.remaining() > this.bytes.length - write) {
			compact();
		}
		if(bb.remaining() > this.bytes.length - write) {
			expand(bb.remaining() - (this.bytes.length - write));
		}
		int before = bb.remaining();
		bb.get(bytes, write, before);
		int after = bb.remaining();
		this.write += before - after;
	}
	
	private void compact() {
		System.arraycopy(bytes, read, bytes, 0, write - read);
		read = 0;
		write = write - read;
	}

	private void expand(int add) {
		byte[] tmp = null;
		if(add <= bytes.length) {
			tmp = new byte[bytes.length * 2];
		}
		else {
			tmp = new byte[bytes.length + add];
		}
		System.arraycopy(bytes, read, tmp, 0, write - read);
		read = 0;
		write = write - read;
		this.bytes = tmp;
	}

	class LineParser {
		
		protected StringBuilder sb = new StringBuilder();
		private int maxLength;
		
		protected LineParser(int maxLength) {
			this.maxLength = maxLength;
		}
		
		public String parse() {
			while(read < write) {
				if(bytes[read] == HttpConstants.CR) {
					read++;
					continue;
				}
				if(bytes[read] == HttpConstants.LF) {
					read++;
					String ret = sb.toString();
					if(sb.length() == 0) {
						state = BODY;	//解析header出现空行，进入实体部分
						return null;
					}
					init();
					return ret;
				}
				sb.append((char)bytes[read++]);
				if(sb.length() > this.maxLength) {
					init();
					doException();
				}
			}
			return null;
		}
		
		protected void doException() {
			throw new LineTooLongException("the request line is too long");
		}

		protected void init() {
			sb = new StringBuilder();
		}
		
	}
	
	class HeaderParser extends LineParser {
		
		protected HeaderParser(int maxLength) {
			super(maxLength);
		}
		
		@Override
		protected void doException() {
			throw new LineTooLongException("the head line is too long");
		}
		
	}
	
	class BodyParser extends LineParser {
		
		protected BodyParser() {
			super(-1);
		}
		
		protected void setLength(int length) {
			super.maxLength = length;
		}

		public String parse() {
			if(super.maxLength == -1)
				doException();
			while(read < write) {
				sb.append((char)bytes[read++]);
				if(sb.length() == super.maxLength) {
					String ret = sb.toString();
					init();
					return ret;
				}
			}
			return null;
		}
		
		protected void doException() {
			throw new MissContentLengthException("GET method miss ContentLength");
		}
		
	}
	
}
