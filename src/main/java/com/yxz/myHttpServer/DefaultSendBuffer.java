package com.yxz.myHttpServer;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author Yu 
* 默认发送缓冲类
*/
public class DefaultSendBuffer implements SendBuffer {
	
	private static final Logger logger = LoggerFactory.getLogger(DefaultSendBuffer.class);

	private ByteBuffer byteBuffer;
	
	public DefaultSendBuffer(byte[] bytes) {
		this.byteBuffer = ByteBuffer.wrap(bytes);
	}
	
	@Override
	public boolean isFinished() {
		return !this.byteBuffer.hasRemaining();
	}

	@Override
	public long writeToChannel(SocketChannel socketChannel) {
		try {
			return socketChannel.write(byteBuffer);
		} catch (IOException e) {
			logger.error("", e);
		}
		return -1;
	}

	@Override
	public void close() {
		//nothing
	}
	
}
