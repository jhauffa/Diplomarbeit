package de.tum.in.Util;

public class CharacterTrie<Value> extends Trie<Character, Value> {

	public void set(String path, Value value)
	{
		set(stringToArray(path), value);
	}

	public Value get(String path)
	{
		return get(stringToArray(path));
	}

	private Character[] stringToArray(String s)
	{
		int len = s.length();
		Character[] strArray = new Character[len];
		for (int i = 0; i < len; i++)
			strArray[i] = s.charAt(i);
		return strArray;
	}

}
