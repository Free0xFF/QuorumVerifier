package cn.com.yeoman.quorum.flexiable;

import java.util.HashSet;

/**
 * 对主机不分组，一种最简单的判断方法
 * 来判断一个节点集是否组成一个仲裁集
 * 
 * @author yeoman
 *
 */
public class QuorumMaj implements QuorumVerifier {
	private int half;
	
	public QuorumMaj(int n) {
		this.half = n / 2;
	}
	
	@Override
	public long getWeight(long id) {
		// 如果不分组，每个节点的权重是1
		return (long) 1;
	}

	@Override
	public boolean containsQuorum(HashSet<Long> set) {
		//如果不分组，是否组成一个仲裁，只需要判断set的数量是否大于一半以上，也就是half
		return set.size() > half;
	}

}
