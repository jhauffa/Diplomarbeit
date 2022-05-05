package de.tum.in.MPQA;

import java.util.Iterator;
import java.util.ArrayList;
import java.io.File;
import java.io.FileNotFoundException;

import de.tum.in.SentimentAnalysis.Sentence;


public class Corpus implements Iterable<Sentence> {

	public class SentenceIterator implements Iterator<Sentence>
	{
		private Iterator<Sentence> particleIterator;
		private int particleIdx;

		public SentenceIterator()
		{
			particleIdx = 0;
			loadNextParticle();
		}

		private void loadNextParticle()
		{
			particleIterator = null;
			while ((particleIterator == null) &&
				   (particleIdx < particleNames.size()))
			{
				try
				{
					CorpusParticle particle = new CorpusParticle(documentDir,
							annotDir, particleNames.get(particleIdx),
							onlyDirect, onlyExpressive);
					particleIterator = particle.iterator();
				}
				catch (FileNotFoundException e)
				{
					System.err.println("MPQA corpus consistency error: file '" +
							e.getMessage() + "' not found, skipping");
				}
				catch (SyntaxError e)
				{
					System.err.println("Syntax error in " + e.getMessage() +
							", skipping");
				}

				particleIdx++;
			}
		}

		public boolean hasNext()
		{
			return (particleIterator != null);
		}

		public Sentence next()
		{
			if (particleIterator == null)
				return null;

			Sentence s = particleIterator.next();
			if (!particleIterator.hasNext())
				loadNextParticle();
			return s;
		}

		public void remove()
		{
			throw new UnsupportedOperationException();
		}
	}


	private final File documentDir;
	private final File annotDir;
	private final ArrayList<String> particleNames;
	private final boolean onlyDirect;
	private final boolean onlyExpressive;


	public Corpus(File baseDir, boolean onlyDirect, boolean onlyExpressive)
		throws FileNotFoundException
	{
		if (!baseDir.isDirectory())
			throw new FileNotFoundException(baseDir.getPath());
		documentDir = new File(baseDir, "docs");
		if (!documentDir.isDirectory())
			throw new FileNotFoundException(documentDir.getPath());
		annotDir = new File(baseDir, "man_anns");
		if (!annotDir.isDirectory())
			throw new FileNotFoundException(annotDir.getPath());

		particleNames = new ArrayList<String>();
		for (File node : documentDir.listFiles())
		{
			if (node.isDirectory())
			{
				for (File leaf : node.listFiles())
				{
					if (!leaf.isHidden())
					{
						particleNames.add(node.getName() + File.separator +
								leaf.getName());
					}
				}
			}
		}

		this.onlyDirect = onlyDirect;
		this.onlyExpressive = onlyExpressive;
	}


	// inherited from Iterable

	public SentenceIterator iterator()
	{
		return new SentenceIterator();
	}

}
