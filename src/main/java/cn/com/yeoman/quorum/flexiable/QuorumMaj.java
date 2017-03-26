package cn.com.yeoman.quorum.flexiable;

import java.util.HashSet;

/**
 * �����������飬һ����򵥵��жϷ���
 * ���ж�һ���ڵ㼯�Ƿ����һ���ٲü�
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
		// ��������飬ÿ���ڵ��Ȩ����1
		return (long) 1;
	}

	@Override
	public boolean containsQuorum(HashSet<Long> set) {
		//��������飬�Ƿ����һ���ٲã�ֻ��Ҫ�ж�set�������Ƿ����һ�����ϣ�Ҳ����half
		return set.size() > half;
	}

}
