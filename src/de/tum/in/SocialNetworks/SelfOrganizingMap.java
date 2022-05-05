package de.tum.in.SocialNetworks;

import java.util.Arrays;

import de.tum.in.Math.Matrix;
import de.tum.in.Math.RowDataSet;
import de.tum.in.Math.ImageDataSet;
import de.tum.in.Math.Vector;
import de.tum.in.Util.AuxArrayComparator;


public class SelfOrganizingMap {

	private static class Topology {
		public enum Type { UNKNOWN, HORIZONTAL, VERTICAL, RADIAL };

		public Type type;
		public boolean isInverted;

		public Topology(Type type, boolean isInverted)
		{
			this.type = type;
			this.isInverted = isInverted;
		}
	}

	public static final double defaultStartLearningRate = 0.1;

	private int width;
	private int height;
	private Vector neurons[];
	private RowDataSet curData;

	private int numClusters;
	private int[] clusterMap;
	private boolean constrainedTopology;

	public SelfOrganizingMap(RowDataSet data, int numClusters, int width,
			int maxIter, boolean findClusters, boolean constrainedTopology)
	{
		this(data, numClusters, width, maxIter, findClusters,
				constrainedTopology, defaultStartLearningRate);
	}
	
	public SelfOrganizingMap(RowDataSet data, int numNeurons, int width,
			int maxIter, boolean findClusters, boolean constrainedTopology,
			double startLearningRate)
	{
		curData = data;
		this.width = width;
		height = numNeurons / width;
		this.constrainedTopology = constrainedTopology;

		double mapRadius = Math.max(width, height) / 2.0;
		double timeConstant = maxIter / Math.log(mapRadius);
		
		// initialize the model vector of each neuron with small random values
		int dim = data.getNumDimensions();
		neurons = new Vector[numNeurons];
		for (int i = 0; i < numNeurons; i++)
		{
			neurons[i] = new Vector(dim);
			for (int j = 0; j < dim; j++)
				neurons[i].set(j, Math.random());
		}
		
		for (int i = 0; i < maxIter; i++)
		{
			// choose random vector from input data
			Vector v = data.copyColumn(
					(int) Math.floor(Math.random() * data.getNumPoints()));

			// find Best Matching Unit
			double minDist = Double.POSITIVE_INFINITY;
			int minDistIdx = 0;
			for (int j = 0; j < numNeurons; j++)
			{
				double d = v.euclideanDistSqr(neurons[j]);
				if (d < minDist)
				{
					minDist = d;
					minDistIdx = j;
				}
			}

			// determine local neighborhood of BMU
			int bmuX = minDistIdx % width;
			int bmuY = minDistIdx / width;
			double r = mapRadius * Math.exp(-((double) i / timeConstant));
			r *= r;  // we use squared euclidean distance
			for (int j = 0; j < numNeurons; j++)
			{
				int x = bmuX - (j % width);
				int y = bmuY - (j / width);
				double d = (x * x) + (y * y);

				if (d < r)  // is within radius r?
				{
					// compute adjustment of model vector
					double learningRate = startLearningRate *
						Math.exp(-((double) i / maxIter));
					learningRate *= Math.exp(-d / (2.0 * r));
					Vector dv = Vector.mult(learningRate,
							Vector.sub(v, neurons[j]));

					if (constrainedTopology)
					{
						// Optionally, only neurons to the left/right of the BMU
						// should be affected by movement towards a lower/higher
						// component average.
						double dvAvg = 0.0;
						for (int k = 0; k < dv.size(); k++)
							dvAvg += dv.get(k);
						dvAvg /= dv.size();
						if (((dvAvg < 0.0) && (x > bmuX)) ||
							((dvAvg > 0.0) && (x < bmuX)))
							continue;
					}

					neurons[j].add(dv);
				}
			}
		}

		if (findClusters)
		{
			// perform watershed segmentation of U matrix to find real clusters
			ImageDataSet img = new ImageDataSet(getUMatrix());
			img.stretchHistogram();
			img.quantize(256);
			ImageDataSet watershedMap = WatershedTransform.transform(img);

			clusterMap = new int[neurons.length];
			numClusters = 0;
			for (int i = 0; i < neurons.length; i++)
			{
				int label = watershedMap.getPixel(i);
				if (label == WatershedTransform.valueWatershed)
				{
					// find most similar neuron in 8-neighborhood that is not a
					// watershed
					double minDist = Double.MAX_VALUE;
					for (int offset : watershedMap.get8NeighborhoodOffsets())
					{
						int curIdx = i + offset;
						if ((curIdx < 0) || (curIdx >= neurons.length))
							continue;
						int curLabel = watershedMap.getPixel(curIdx);
						if (curLabel != WatershedTransform.valueWatershed)
						{
							double dist = neurons[i].euclideanDist(
									neurons[curIdx]);
							if (dist < minDist)
							{
								label = curLabel;
								minDist = dist;
							}
						}
					}
					assert (label != WatershedTransform.valueWatershed);
				}

				clusterMap[i] = label - 1;
				if (label > numClusters)
					numClusters = label;
			}
		}
		else
			numClusters = neurons.length;
	}

	public int getNumClusters()
	{
		return numClusters;
	}

	private double getNeuronComponentAvg(int idx)
	{
		double sumFv = 0.0;
		for (int i = 0; i < neurons[idx].size(); i++)
			sumFv += neurons[idx].get(i);
		return sumFv / neurons[idx].size(); 
	}

	private Topology estimateTopology()
	{
		double center = getNeuronComponentAvg(
				((height / 2) * width) + (width / 2));

		double[] sideAvg = new double[4];
		for (int i = 0; i < width; i++)
		{
			// top and bottom
			sideAvg[0] += getNeuronComponentAvg(i);
			sideAvg[1] += getNeuronComponentAvg(((height - 1) * width) + i);
		}
		sideAvg[0] /= width;
		sideAvg[1] /= width;
		for (int i = 0; i < height; i++)
		{
			// left and right
			sideAvg[2] += getNeuronComponentAvg(i * width);
			sideAvg[3] += getNeuronComponentAvg((i * width) + (width - 1));
		}
		sideAvg[2] /= height;
		sideAvg[3] /= height;

		Topology.Type type = Topology.Type.UNKNOWN;
		boolean isInverted = false;
		if ((sideAvg[0] < center) && (center < sideAvg[1]))
		{
			type = Topology.Type.VERTICAL;
			isInverted = false;
		}
		else if ((sideAvg[1] < center) && (center < sideAvg[0]))
		{
			type = Topology.Type.VERTICAL;
			isInverted = true;
		}
		else if ((sideAvg[2] < center) && (center < sideAvg[3]))
		{
			type = Topology.Type.HORIZONTAL;
			isInverted = false;
		}
		else if ((sideAvg[3] < center) && (center < sideAvg[2]))
		{
			type = Topology.Type.HORIZONTAL;
			isInverted = true;
		}
		else if ((sideAvg[0] < center) && (sideAvg[1] < center) &&
				 (sideAvg[2] < center) && (sideAvg[3] < center))
		{
			type = Topology.Type.RADIAL;
			isInverted = false;
		}
		else if ((sideAvg[0] > center) && (sideAvg[1] > center) &&
				 (sideAvg[2] > center) && (sideAvg[3] > center))
		{
			type = Topology.Type.RADIAL;
			isInverted = true;
		}
		else
			System.err.println("Warning: unknown SOM topology");

		return new Topology(type, isInverted);
	}

	private double computeNeuronDist(int idx, Topology t)
	{
		double dist = 0.0;
		switch (t.type)
		{
		case HORIZONTAL:
			dist = idx % width;
			break;
		case VERTICAL:
			dist = idx / width;
			break;
		case RADIAL:
			int y = (height / 2) - (idx / width);
			int x = (width / 2) - (idx % width);
			dist = Math.sqrt((x * x) + (y * y));
			break;
		}

		if (t.isInverted)
			return -dist;
		return dist;
	}

	public void sortClusters()
	{
		if (!constrainedTopology)
		{
			Topology t = estimateTopology();
			sortClusters(t);
		}
		else if (clusterMap != null)
		{
			Topology t = new Topology(Topology.Type.HORIZONTAL, false);
			sortClusters(t);
		}
	}

	private void sortClusters(Topology t)
	{
		// Sort neurons so that there is a positive association between the
		// cluster index and the distance to the maximum neuron (area).
		Integer[] inverseNeuronMap = new Integer[neurons.length];
		Double[] neuronDist = new Double[neurons.length];
		for (int i = 0; i < neurons.length; i++)
		{
			inverseNeuronMap[i] = i;
			neuronDist[i] = computeNeuronDist(i, t);
		}
		Arrays.sort(inverseNeuronMap, new AuxArrayComparator(neuronDist));
		int curRank = 0;
		int[] neuronRankMap = new int[neurons.length];
		for (int i = 0; i < neurons.length; i++)
		{
			if ((i > 0) && (Math.abs(neuronDist[i] - neuronDist[i - 1]) > 0.01))
				curRank++;
			neuronRankMap[inverseNeuronMap[i]] = curRank;
		}

		if (clusterMap != null)
		{
			Double[] avgClusterRank = new Double[numClusters];
			for (int i = 0; i < numClusters; i++)
				avgClusterRank[i] = 0.0;
			int[] neuronsPerCluster = new int[numClusters];
			for (int i = 0; i < clusterMap.length; i++)
			{
				int idx = clusterMap[i]; 
				avgClusterRank[idx] += neuronRankMap[i];
				neuronsPerCluster[idx]++;
			}
			Integer[] inverseClusterMap = new Integer[numClusters];
			for (int i = 0; i < numClusters; i++)
			{
				avgClusterRank[i] /= neuronsPerCluster[i];
				inverseClusterMap[i] = i;
			}
			Arrays.sort(inverseClusterMap,
					new AuxArrayComparator(avgClusterRank));
			int[] clusterRankMap = new int[numClusters];
			for (int i = 0; i < numClusters; i++)
				clusterRankMap[inverseClusterMap[i]] = i;
			for (int i = 0; i < clusterMap.length; i++)
				clusterMap[i] = clusterRankMap[clusterMap[i]];
		}
		else
		{
			clusterMap = neuronRankMap;
			numClusters = curRank + 1;
		}
	}

	public Vector getAssociationVector()
	{
		int num = curData.getNumPoints();
		Vector assoc = new Vector(num);

		for (int i = 0; i < num; i++)
		{
			Vector v = curData.copyColumn(i);

			// find closest center
			double minDist = Double.POSITIVE_INFINITY;
			int minDistIdx = 0;
			for (int j = 0; j < neurons.length; j++)
			{
				double d = v.euclideanDistSqr(neurons[j]);
				if (d < minDist)
				{
					minDist = d;
					minDistIdx = j;
				}
			}

			if (clusterMap != null)
				minDistIdx = clusterMap[minDistIdx];
			assoc.set(i, minDistIdx);
		}

		return assoc;
	}

	public Vector[] getNeurons()
	{
		return neurons;
	}
	
	public Matrix getUMatrix()
	{
		Matrix u = new Matrix(height, width);
		
		for (int i = 0; i < height; i++)
		{
			for (int j = 0; j < width; j++)
			{
				int numNeighbors = 0;
				double sumDist = 0.0;
				
				int curNeuron = i * width + j;
				if (j > 0)  // left
				{
					sumDist += neurons[curNeuron].euclideanDist(
							neurons[i * width + (j - 1)]);
					numNeighbors++;
				}
				if (i > 0)  // top
				{
					sumDist += neurons[curNeuron].euclideanDist(
							neurons[(i - 1) * width + j]);
					numNeighbors++;
				}
				if (j < (width - 1))  // right
				{
					sumDist += neurons[curNeuron].euclideanDist(
							neurons[i * width + (j + 1)]);
					numNeighbors++;
				}
				if (i < (height - 1))  // bottom
				{
					sumDist += neurons[curNeuron].euclideanDist(
							neurons[(i + 1) * width + j]);
					numNeighbors++;
				}
				
				u.set(i, j, sumDist / numNeighbors);
			}
		}
		
		return u;
	}

}
