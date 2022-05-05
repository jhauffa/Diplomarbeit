package de.tum.in.Util;

import java.util.HashMap;
import java.io.Serializable;

public class Trie<Key, Value> implements Serializable {

	private class TrieNode implements Serializable
	{
		public Value value;
		public HashMap<Key, TrieNode> next;

		public void clear()
		{
			value = null;
			next = null;
		}
	}

	private TrieNode rootNode;
	private int numValueEntries;

	public Trie()
	{
		rootNode = new TrieNode();
		numValueEntries = 0;
	}

	public int size()
	{
		return numValueEntries;
	}

	public void set(Key[] path, Value value)
	{
		TrieNode curNode = rootNode;
		for (int i = 0; i < path.length; i++)
		{
			if (curNode.next == null)
				curNode.next = new HashMap<Key, TrieNode>();
			TrieNode nextNode = curNode.next.get(path[i]);
			if (nextNode == null)
			{
				nextNode = new TrieNode();
				curNode.next.put(path[i], nextNode);
			}
			curNode = nextNode;
		}

		if (curNode.value == null)
			numValueEntries++;
		curNode.value = value;
	}

	public Value get(Key[] path)
	{
		TrieNode curNode = rootNode;
		for (int i = 0; i < path.length; i++)
		{
			if (curNode.next == null)
				return null;
			curNode = curNode.next.get(path[i]);
			if (curNode == null)
				return null;
		}
		return curNode.value;
	}

	public void clear()
	{
		rootNode.clear();
		numValueEntries = 0;
	}

}
