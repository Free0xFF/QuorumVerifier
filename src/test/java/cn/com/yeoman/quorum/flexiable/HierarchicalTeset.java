package cn.com.yeoman.quorum.flexiable;

import java.util.HashSet;

import org.junit.Test;

import cn.com.yeoman.quorum.flexiable.Hierarchical.ConfigException;
import junit.framework.TestCase;

public class HierarchicalTeset {
	QuorumVerifier quorumVerifier;
	
	@Test
	public void testGetWeight() throws ConfigException {
		quorumVerifier = new Hierarchical("config.ini");
		TestCase.assertEquals(5, quorumVerifier.getWeight((long)5));
	}
	
	@Test
	public void testContainsQuorum() throws ConfigException {
		quorumVerifier = new Hierarchical("config.ini");
		HashSet<Long> set = new HashSet<Long>();
		set.add((long)1);
		set.add((long)2);
		set.add((long)3);
		TestCase.assertFalse(quorumVerifier.containsQuorum(set));
		
		set.add((long)4);
		TestCase.assertFalse(quorumVerifier.containsQuorum(set));
		
		set.add((long)5);
		TestCase.assertTrue(quorumVerifier.containsQuorum(set));
	}
}
