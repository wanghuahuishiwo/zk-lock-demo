package com.zklock.synlock;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.Watcher.Event.EventType;
import org.apache.zookeeper.Watcher.Event.KeeperState;
import org.apache.zookeeper.ZooDefs.Ids;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public abstract class ZkSyncLock implements Watcher{
	
	protected Logger log = LoggerFactory.getLogger(ZkSyncLock.class);
	
	protected Long id;
	protected CountDownLatch lockValve = new CountDownLatch(1);
	protected CountDownLatch createZkValve = new CountDownLatch(1);
	protected Long waitTimeOut = 1000l;
	private	Boolean isValid;
	
	protected ZooKeeper zk;
	protected String connectString;
	protected int sessionTimeout;
	
	protected String root_node;
	protected String sub_node;
	
	protected ZkSyncLock(String connectString, int sessionTimeout) 
			throws Exception {
		id = Thread.currentThread().getId();
		this.root_node = "/root_lock";
		this.sub_node = "/lock-";
		this.createZk(connectString, sessionTimeout);
		if(!isValid) {
			return;
		}
		this.createRootNode();
	}
	
	protected ZkSyncLock(String connectString, int sessionTimeout, String root_node, String sub_node) 
			throws Exception {
		id = Thread.currentThread().getId();
		this.root_node = root_node;
		this.sub_node = sub_node;
		this.createZk(connectString, sessionTimeout);
		if(!isValid) {
			return;
		}
		this.createRootNode();
	}
	
	private void createZk(String connectString, int sessionTimeout) throws Exception {
		try {
			zk = new ZooKeeper(connectString, sessionTimeout, this);
			isValid = createZkValve.await(300, TimeUnit.MILLISECONDS);//300, TimeUnit.MILLISECONDS
		} catch (Exception e) {
			log.error(id + "create zk error.",e);
			throw e;
		}
	}
	
	private synchronized void createRootNode() throws Exception {
		try {
			if(!isValid) {
				return;
			}
			Stat stat = zk.exists(root_node, false);
			if(stat == null) {
				zk.create(root_node, new byte[0], Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
				log.info(id + ", create root node.");
			}
		} catch (Exception e) {
			log.error(id + "create root node error.",e);
			throw e;
		}
	}
	
	private void createSubNode() throws Exception {
		try {
			sub_node = zk.create(root_node + sub_node, new byte[0],
					Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
		} catch (Exception e) {
			log.error(id + "create sub node error.",e);
			throw e;
		}
	}
	
	private boolean tryLock() throws Exception {
		log.info(id + ", tryLock.");
		try {
			String myNode = sub_node.substring(sub_node.lastIndexOf("/") + 1);
			List<String> subList = zk.getChildren(root_node, false);
			if(subList.size() == 0) {
				return true;
			}
			Collections.sort(subList); //正序排序
			
			if(myNode.equals(subList.get(0))) 
				return true;
			else 
				return addPreNodeWatcher(subList, myNode);
			
		} catch (Exception e) {
			log.error(id + "create sub node error.",e);
			unlock();
			throw e;
		} 
	}
	
	private boolean addPreNodeWatcher(List<String> subList, String myNode) throws KeeperException, InterruptedException {
		int index = subList.indexOf(myNode);
		String preNode = subList.get(index - 1);
		Stat stat = zk.exists(root_node + "/" + preNode, true);
		if(stat == null) {
			return true;
		} else {
			return false;
		}
	}
	
	private void unlock() {
		try {
			zk.delete(sub_node, -1);
			System.out.println();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void handler(){
		if(!isValid) {
			return;
		}
		try {
			this.createSubNode();
			if(tryLock()) {
				log.info(id + ", get lock sucess.");
				lock();
			} else {
				log.info(id + ", get lock fail.");
				lockValve.await(); 
				lock();
			}
		} catch (Exception e) {
			log.error(id + ", handler error.", e);
		} finally {
			unlock();
		}
	}
	
	public abstract void lock();
	
	public void process(WatchedEvent event) {
		EventType type = event.getType();
		if(KeeperState.SyncConnected.equals(event.getState())) {
			createZkValve.countDown();
			log.info(id + ", type:" + type + ", createZkValve.countDown");
		}
		if((EventType.NodeDeleted).equals(type)) {
			lockValve.countDown();
			log.info(id + ", type:" + type + ", myNode:" + sub_node + ", deleteNode" + event.getPath());
		}
	}
	
}
