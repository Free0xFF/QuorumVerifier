package cn.com.yeoman.quorum.flexiable;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map.Entry;

import java.util.Properties;

/**
 * 这个类实现分层仲裁的验证器， 将所有几点分成多个组，每个节点有自己的权重
 * 对于所有这些组，如果获取到改组权重和的一般以上，
 * 然后该组仲裁成功，当所有仲裁组成功的数量超过所有组的一般，则验证通过
 *
 * 这种分层体系的仲裁验证器主要使用两个参数：group和weight。
 *  group.1=1:2:3
 *  group.2=4:5:6
 *  group.3=7:8:9
 *
 *  weight.1=1
 *  weight.2=1
 *  weight.3=1
 *  weight.4=1
 *  weight.5=1
 *  weight.6=1
 *  weight.7=1
 *  weight.8=1
 *  weight.9=1
 * 
 * @author yeoman
 *
 */

public class Hierarchical implements QuorumVerifier {
	private HashMap<Long, Long> serverGroup;
	private HashMap<Long, Long> serverWeight;
	private HashMap<Long, Long> groupWeight;
	private int numGroups;
	
	/**
	 * 传递一个配置文件
	 * 
	 * @param configFile
	 * @throws ConfigException 
	 */
	public Hierarchical(String configFile) throws ConfigException {
		this.serverGroup = new HashMap<Long, Long>();
		this.serverWeight = new HashMap<Long, Long>();
		this.groupWeight = new HashMap<Long, Long>();
		this.numGroups = 0;
		readConfigFile(configFile);
	}
	
	/**
	 * 传递一个已经解析成key-value对的配置参数
	 * @param cfg
	 */
	public Hierarchical(Properties cfg) {
		this.serverGroup = new HashMap<Long, Long>();
		this.serverWeight = new HashMap<Long, Long>();
		this.groupWeight = new HashMap<Long, Long>();
		this.numGroups = 0;
		parse(cfg);
	}
	
	/**
	 * 直接传递已经解析的参数
	 * 
	 * @param numGroups
	 * @param serverGroup
	 * @param serverWeight
	 */
	public Hierarchical(int numGroups, 
									HashMap<Long, Long> serverGroup, 
									HashMap<Long, Long> serverWeight) {
		this.numGroups = numGroups;
		this.serverGroup = serverGroup;
		this.serverWeight = serverWeight;
		this.groupWeight = new HashMap<Long, Long>();
		computeGroupWeight();
	}
	
	/**
	 * 读取文件
	 * @param configFile
	 * @throws ConfigException
	 */
	public void readConfigFile(String configFile) throws ConfigException {
		File f = new File(configFile);
		if(!f.exists()) {
			throw new IllegalArgumentException("路径错误："+configFile);
		}
		
		try {
			InputStream in = new FileInputStream(f);
			Properties cfg = new Properties();
			try {
				cfg.load(in);
			} finally {
				in.close();
			}
			
			parse(cfg);
			
		} catch (IOException e) {
			throw new ConfigException("Error processing "+configFile, e);
		}
	}
	
	/**
	 * 解析参数
	 * @param cfg
	 */
	public void parse(Properties cfg) {
		for(Entry<Object, Object> entry : cfg.entrySet()) {
			String key = entry.getKey().toString().trim();
			String value = entry.getValue().toString().trim();
			if(key.startsWith("group.")) {
				int dot = key.indexOf('.');
				if(dot < 0) {
					throw new IllegalArgumentException("格式有误: "+key+"="+value);
				}
				Long gid = Long.valueOf(key.substring(dot + 1));
				
				numGroups++;
				
				String[] sids = value.split(":");
				for(String s: sids) {
					Long sid = Long.valueOf(s);
					serverGroup.put(sid, gid);
				}
			} else if(key.startsWith("weight.")) {
				int dot = key.indexOf('.');
				if(dot < 0) {
					throw new IllegalArgumentException("格式有误: "+key+"="+value);
				}
				Long sid = Long.valueOf(key.substring(dot + 1));
				Long weight = Long.valueOf(value);
				serverWeight.put(sid, weight);
			} else {
				//其他参数设置为系统变量
				System.setProperty(key, value);
			}
		}
		computeGroupWeight();
	}
	
	/**
	 * 计算组权重
	 */
	public void computeGroupWeight() {
		for(long sid : serverWeight.keySet()) {
			long gid = serverGroup.get(sid);
			if(!groupWeight.containsKey(gid)) {
				groupWeight.put(gid, serverWeight.get(sid));
			} else {
				long totalWeight = groupWeight.get(gid) + serverWeight.get(sid);
				groupWeight.put(gid, totalWeight);
			}
		}
		
		//考虑到有些组的权重为0，重新计算numGroups
		for(long weight : groupWeight.values()) {
			if(weight == (long) 0) {
				numGroups--;
			}
		}
	}
	
	@SuppressWarnings("serial")
	public static class ConfigException extends Exception {
		public ConfigException(String msg) {
			super(msg);
		}
		
		public ConfigException(String msg, Throwable e) {
			super(msg, e);
		}
	}

	@Override
	public long getWeight(long id) {
		return serverWeight.get(id);
	}

	@Override
	public boolean containsQuorum(HashSet<Long> set) {
		//首先对该集合进行分组，计算每组的组权限
		HashMap<Long, Long> expansion = new HashMap<Long, Long>();
		for(Long sid : set) {
			Long gid = serverGroup.get(sid);
			if(!expansion.containsKey(gid)) {
				expansion.put(gid, serverWeight.get(sid));
			} else {
				long totalWeight = expansion.get(gid) + serverWeight.get(sid);
				expansion.put(gid, Long.valueOf(totalWeight));
			}
		}
		
		//然后计算符合大多数集的组数
		int majGroupCounter = 0;
		for(Long gid : expansion.keySet()) {
			long expWeight = expansion.get(gid);
			long totalWeight = groupWeight.get(gid);
			if(expWeight > totalWeight/2) {
				majGroupCounter++;
			}
		}
		
		//在判断是否是总组数的大多数集
		if(majGroupCounter > numGroups/2) {
			return true;
		}
		return false;
	}
}
