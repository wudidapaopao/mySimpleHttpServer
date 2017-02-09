package com.yxz.myHttpServer;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

/**
* @author Yu 
* 文件发送缓冲类
*/
public class FileSendBuffer implements SendBuffer {

	private RandomAccessFile file;
	private FileChannel fileChannel;
	private long position;
	private long fileLength;
	
	public FileSendBuffer(RandomAccessFile file) throws IOException {
		this.file = file;
		this.fileChannel = file.getChannel();
		this.fileLength = fileChannel.size();
		position = 0;
	}
	
	@Override
	public boolean isFinished() {
		return (fileLength - position) <= 0;
	}

	@Override
	public long writeToChannel(SocketChannel socketChannel) throws Exception {
		long remain = fileLength - position;
		if(remain <= 0) {
			return 0;
		}
		long writeOnce = this.fileChannel.transferTo(position, remain, socketChannel);
		position += writeOnce;
		return writeOnce;
	}

	@Override
	public void close() {
		try {
			fileChannel.close();
			file.close();
		} catch (IOException e) {
			
		}		
	}

}
