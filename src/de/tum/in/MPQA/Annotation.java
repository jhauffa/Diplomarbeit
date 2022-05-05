package de.tum.in.MPQA;

public interface Annotation {

	public int getStart();
	public int getEnd();

	public boolean isEmpty();

	public boolean parse(CorpusParticle owner, String line);

}
