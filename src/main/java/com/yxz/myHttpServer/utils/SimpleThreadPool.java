package com.yxz.myHttpServer.utils;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
* @author Yu 
* 简易线程池工具类
*/
public class SimpleThreadPool implements Executor {

	private List<Thread> workers;
	private BlockingQueue<Runnable> waitingTasks;
	private volatile int poolSize;
	private volatile int maxpoolSize;
	private volatile int corePoolSize;
	private volatile long aliveTime;
	private volatile boolean closed;
	private final boolean preStarted;
	private static final String POOL_NAME_PREFIX = "pool";
	private static AtomicInteger index = new AtomicInteger(0);
	
	public SimpleThreadPool(List<Thread> workers, BlockingQueue<Runnable> waitingTasks, int maxpoolSize,
					int corePoolSize, long aliveTime, boolean preStarted) {
		this.workers = workers;
		this.waitingTasks = waitingTasks;
		this.maxpoolSize = maxpoolSize;
		this.corePoolSize = corePoolSize;
		this.aliveTime = aliveTime;
		this.preStarted = preStarted;
		this.closed = false;
		this.poolSize = 0;
		init();
	}

	private void init() {
		if(preStarted) {
			for(int i = 0; i < corePoolSize; i++) {
				Thread t = creatThread(null);
				workers.add(t);
				t.start();
			}
		}
	}

	private Thread creatThread(Runnable task) {
		Thread t = new TaskThread(task);
		t.setName(this.POOL_NAME_PREFIX + this.index.getAndIncrement());
		t.setDaemon(false);
		return t;
	}

	@Override
	public synchronized void execute(Runnable task) {//线程数量小于core，就新建线程，不然加入阻塞队列，不然再新建线程，若线程数量大于max，则拒绝这个任务
		if(!closed && poolSize < corePoolSize) {
			Thread t = creatThread(task);
			workers.add(t);
			t.start();
			poolSize++;
			return;
		}
		if(!waitingTasks.offer(task)) {
			if(poolSize < maxpoolSize) {
				Thread t = creatThread(task);
				workers.add(t);
				t.start();
				poolSize++;
			}
			else {
				// to do 任务被拒绝
			}
		}
	}
	
	class TaskThread extends Thread {

		private Runnable task;
		
		public TaskThread(Runnable runnable) {
			this.task = runnable;
		}
		
		@Override
		public void run() {
			while(task != null || (task = getTask()) != null) {
				try {
					task.run();
				} catch (Exception e) {
					
				} finally {
					task = null;
				}
			}
		}

		private Runnable getTask() {
			try {
    			if(aliveTime > 0) {
    				return waitingTasks.poll(aliveTime, TimeUnit.MILLISECONDS);
    			}
    			else {
    				return waitingTasks.take();
    			}
			} catch(InterruptedException e) {
				return null;
			}
		}
		
	}
	
	public void close() {
		this.closed = true;
	}

}
