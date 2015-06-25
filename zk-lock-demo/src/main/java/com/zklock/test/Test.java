package com.zklock.test;

import java.util.concurrent.CountDownLatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zklock.synlock.ZkSyncLock;
import com.zklock.synlock.ZkSyncLockImpl;

public class Test {
	
	public static Logger log = LoggerFactory.getLogger(Test.class);
	
	public static void main(String[] args) throws InterruptedException {
		final String connectString = "10.233.19.16:2181,10.233.18.118:2181,10.233.18.132:2181";
		final int sessionTimeout = 1000;
		
		int index = 100;
		final CountDownLatch latch = new CountDownLatch(index);
		
		Long start = System.currentTimeMillis();
		for(int i=0; i<index; i++) {
			Thread t = new Thread(new Runnable() {
				public void run() {
					try {
						ZkSyncLock zk = new ZkSyncLockImpl(connectString, sessionTimeout);
						zk.handler();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						latch.countDown();
					}
				}
			});
			t.start();
		}
		latch.await();
		Long end = System.currentTimeMillis();
		
		
		log.info("-------end------ cost:" + (end - start) + "ms.");
		log.info(String.valueOf(SynDataSource.resource));
		for(String s : SynDataSource.datas) {
			log.info(s);
		}
	}
	
}
