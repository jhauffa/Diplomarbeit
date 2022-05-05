package de.tum.in.Util;

import java.util.Comparator;

public class AuxArrayComparator<T> implements Comparator<Integer> {

	private T[] auxArray;

	public AuxArrayComparator(T[] auxArray)
	{
		this.auxArray = auxArray;
	}

	public int compare(Integer idx1, Integer idx2)
	{
		return ((Comparable) auxArray[idx1]).compareTo(auxArray[idx2]);
	}

}
