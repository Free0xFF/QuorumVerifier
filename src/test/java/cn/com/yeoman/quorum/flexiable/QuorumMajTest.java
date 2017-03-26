package cn.com.yeoman.quorum.flexiable;

import java.util.HashSet;

import org.junit.Test;

import junit.framework.TestCase;

public class QuorumMajTest {
	
	QuorumVerifier quorumVerifier;
	
	@Test
	public void testGetWeight() {
		quorumVerifier = new QuorumMaj(10);
		TestCase.assertEquals(1, quorumVerifier.getWeight(1));
	}
	
	@Test
	public void testContainsQuorum() {
		quorumVerifier = new QuorumMaj(10);
		HashSet<Long> set = new HashSet<Long>();
		set.add((long)1);
		set.add((long)2);
		set.add((long)3);
		set.add((long)4);
		set.add((long)5);
		TestCase.assertFalse(quorumVerifier.containsQuorum(set));
		
		set.add((long)6);
		TestCase.assertTrue(quorumVerifier.containsQuorum(set));
	}
}
