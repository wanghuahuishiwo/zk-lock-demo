package com.zklock.synlock;

import com.zklock.test.SynDataSource;

public class ZkSyncLockImpl extends ZkSyncLock{

	
	public ZkSyncLockImpl(String connectString, int sessionTimeout)
			throws Exception {
		super(connectString, sessionTimeout);
	}


	@Override
	public void lock() {
		SynDataSource.getInstance().resource ++;
		String date =  id + ", myNode:" + sub_node;
		SynDataSource.getInstance().datas.add(date);
		log.info( id + ", 执行handler!");
	}

}
