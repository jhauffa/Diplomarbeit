package de.tum.in.SocialNetworks;

import java.io.PrintStream;
import java.util.ArrayList;

import de.tum.in.Math.RowDataSet;
import de.tum.in.Math.Vector;


public class FeatureGenerator {

	private ArrayList<MessageFeature> featuresMsg;
	private ArrayList<RelationshipFeature> featuresRel;

	public FeatureGenerator()
	{
		featuresMsg = new ArrayList<MessageFeature>();
		featuresRel = new ArrayList<RelationshipFeature>();
	}

	public void addMsgFeature(MessageFeature f)
	{
		featuresMsg.add(f);
	}

	public void addRelFeature(RelationshipFeature f)
	{
		featuresRel.add(f);
	}

	public int getNumFeatures()
	{
		return featuresMsg.size() + featuresRel.size();
	}

	public RowDataSet getFeatureVectors(SocialNetwork net)
	{
		RowDataSet featureVectors = new RowDataSet(getNumFeatures(),
				net.getNumEdges());
		int idx = 0;
		for (Relationship r : net)
		{
			Vector v = getFeatureVector(r);
			featureVectors.setColumn(idx++, v);
		}
		return featureVectors;
	}

	public Vector getFeatureVector(Relationship r)
	{
		Vector fv = new Vector(getNumFeatures());

		int numMsgFeatures = featuresMsg.size();
		for (int i = 0; i < numMsgFeatures; i++)
		{
			MessageFeature f = featuresMsg.get(i);
			double v = 0.0;
			for (ProcessedMessage msg : r.messages)
				v += f.getValue(msg);
			fv.set(i, v / r.messages.size());
		}

		for (int i = 0; i < featuresRel.size(); i++)
		{
			double v = featuresRel.get(i).getValue(r);
			fv.set(numMsgFeatures + i, v);
		}

		return fv;
	}

	public void printConfiguration(PrintStream out)
	{
		int i1;
		for (i1 = 0; i1 < featuresMsg.size(); i1++)
			out.printf("%d\t%s\n", i1+1, featuresMsg.get(i1).getDescription());
		int i2;
		for (i2 = 0; i2 < featuresRel.size(); i2++)
			out.printf("%d\t%s\n", i1+i2, featuresMsg.get(i2).getDescription());
	}

	public void printFeatureVectors(PrintStream out, RowDataSet featureVectors,
			boolean printFeatureName)
	{
		int numMsgFeatures = featuresMsg.size();
		for (int i = 0; i < featureVectors.getNumDimensions(); i++)
		{
			if (printFeatureName)
			{
				String desc;
				if (i < numMsgFeatures)
					desc = featuresMsg.get(i).getDescription();
				else
					desc = featuresRel.get(i - numMsgFeatures).getDescription();
				out.printf("%s;", desc);
			}
			else
				out.printf("%d;", i + 1);

			for (int j = 0; j < featureVectors.getNumPoints(); j++)
				out.printf("%f;", featureVectors.get(i, j));
			out.println();
		}
		out.println();
		out.flush();
	}

}
