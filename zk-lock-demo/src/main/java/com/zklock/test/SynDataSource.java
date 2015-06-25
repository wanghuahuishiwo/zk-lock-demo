package com.zklock.test;

import java.util.ArrayList;
import java.util.List;

public class SynDataSource {
	
	private SynDataSource(){}
	
	public static SynDataSource getInstance() {
		return SingtonSynDataSource.obj;
	}
	
	private static class SingtonSynDataSource{
		private static SynDataSource obj = new SynDataSource();
	}
	
	public static int resource = 0;
	public static List<String> datas = new ArrayList<String>();
	
}
