package cn.com.yeoman.quorum.flexiable;

import java.util.HashSet;

public interface QuorumVerifier {
	public long getWeight(long id);
	public boolean containsQuorum(HashSet<Long> set);
}
