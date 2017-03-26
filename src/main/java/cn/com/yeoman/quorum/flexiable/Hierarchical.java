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
 * �����ʵ�ֲַ��ٲõ���֤���� �����м���ֳɶ���飬ÿ���ڵ����Լ���Ȩ��
 * ����������Щ�飬�����ȡ������Ȩ�غ͵�һ�����ϣ�
 * Ȼ������ٲóɹ����������ٲ���ɹ������������������һ�㣬����֤ͨ��
 *
 * ���ֲַ���ϵ���ٲ���֤����Ҫʹ������������group��weight��
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
	 * ����һ�������ļ�
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
	 * ����һ���Ѿ�������key-value�Ե����ò���
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
	 * ֱ�Ӵ����Ѿ������Ĳ���
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
	 * ��ȡ�ļ�
	 * @param configFile
	 * @throws ConfigException
	 */
	public void readConfigFile(String configFile) throws ConfigException {
		File f = new File(configFile);
		if(!f.exists()) {
			throw new IllegalArgumentException("·������"+configFile);
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
	 * ��������
	 * @param cfg
	 */
	public void parse(Properties cfg) {
		for(Entry<Object, Object> entry : cfg.entrySet()) {
			String key = entry.getKey().toString().trim();
			String value = entry.getValue().toString().trim();
			if(key.startsWith("group.")) {
				int dot = key.indexOf('.');
				if(dot < 0) {
					throw new IllegalArgumentException("��ʽ����: "+key+"="+value);
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
					throw new IllegalArgumentException("��ʽ����: "+key+"="+value);
				}
				Long sid = Long.valueOf(key.substring(dot + 1));
				Long weight = Long.valueOf(value);
				serverWeight.put(sid, weight);
			} else {
				//������������Ϊϵͳ����
				System.setProperty(key, value);
			}
		}
		computeGroupWeight();
	}
	
	/**
	 * ������Ȩ��
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
		
		//���ǵ���Щ���Ȩ��Ϊ0�����¼���numGroups
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
		//���ȶԸü��Ͻ��з��飬����ÿ�����Ȩ��
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
		
		//Ȼ�������ϴ������������
		int majGroupCounter = 0;
		for(Long gid : expansion.keySet()) {
			long expWeight = expansion.get(gid);
			long totalWeight = groupWeight.get(gid);
			if(expWeight > totalWeight/2) {
				majGroupCounter++;
			}
		}
		
		//���ж��Ƿ����������Ĵ������
		if(majGroupCounter > numGroups/2) {
			return true;
		}
		return false;
	}
}
