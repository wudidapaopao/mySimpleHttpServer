package com.yxz.myHttpServer;

import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WrappedChannel {
	
	private SocketChannel socketChannel;
	private int interest;
	private RequestHandler handler;
	private Queue<SendBuffer> sendBuffers = new ConcurrentLinkedQueue<>();
	private long time;
	private Selector selector;
	private Poller poller;
	private volatile boolean finished;
	
	public boolean isFinished() {
		return finished;
	}
	public void setFinished(boolean finished) {
		this.finished = finished;
	}
	public Poller getPoller() {
		return poller;
	}
	public void setPoller(Poller poller) {
		this.poller = poller;
	}
	public Selector getSelector() {
		return selector;
	}
	public void setSelector(Selector selector) {
		this.selector = selector;
	}
	public long getTime() {
		return time;
	}
	public void setTime(long time) {
		this.time = time;
	}
	public Queue<SendBuffer> getSendBuffers() {
		return this.sendBuffers;
	}
	public void addSendBuffer(SendBuffer sendBuffer) {
		this.sendBuffers.add(sendBuffer);
	}
	public WrappedChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
	public RequestHandler getHandler() {
		return handler;
	}
	public void setHandler(RequestHandler handler) {
		this.handler = handler;
	}
	public int getInterest() {
		return interest;
	}
	public void setInterest(int interest) {
		this.interest = interest;
	}
	public SocketChannel getSocketChannel() {
		return socketChannel;
	}
	public void setSocketChannel(SocketChannel socketChannel) {
		this.socketChannel = socketChannel;
	}
	
}
