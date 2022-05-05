package de.tum.in.SocialNetworks;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;

import de.tum.in.Math.ImageDataSet;


public class WatershedTransform {

	private static class ComparePixel implements Comparator<Integer>
	{
		private ImageDataSet image;

		public ComparePixel(ImageDataSet image)
		{
			this.image = image;
		}

		public int compare(Integer i1, Integer i2)
		{
			return image.getPixel(i1) - image.getPixel(i2);
		}
	}

	public static final int valueMask = -2;
	public static final int valueInit = -1;
	public static final int valueWatershed = 0;

	private static final int markerPixelIdx = -1;

	public static ImageDataSet transform(ImageDataSet in)
	{
		int h = in.height();
		int w = in.width();
		ImageDataSet d = new ImageDataSet(w, h);
		ImageDataSet out = new ImageDataSet(w, h);
		out.fill(valueInit);

		int[] neighborOffsets = in.get4NeighborhoodOffsets();

		// TODO: "sorting by address calculation" might be more efficient 
		int numPixel = h * w;
		Integer[] pixelIdx = new Integer[numPixel];
		for (int i = 0; i < numPixel; i++)
			pixelIdx[i] = i;
		Arrays.sort(pixelIdx, new ComparePixel(in));

		int curLabel = 0;
		LinkedList<Integer> queue = new LinkedList<Integer>();

		int pStart = 0;
		int curIn = in.getPixel(pixelIdx[pStart]);
		while (pStart < numPixel)
		{
			// find range of pixels with same value
			int pEnd = pStart + 1;
			int nextIn = curIn;
			while (pEnd < numPixel)
			{
				nextIn = in.getPixel(pixelIdx[pEnd]);
				if (nextIn != curIn)
					break;
				pEnd++;
			}

			for (int j = pStart; j < pEnd; j++)
			{
				int curIdx = pixelIdx[j];
				out.setPixel(curIdx, valueMask);
				for (int offset : neighborOffsets)
				{
					int nIdx = curIdx + offset;
					if ((nIdx < 0) || (nIdx >= numPixel))
						continue;
					int nOut = out.getPixel(nIdx);
					if ((nOut > 0) || (nOut == valueWatershed))
					{
						d.setPixel(curIdx, 1);
						queue.addFirst(curIdx);
					}
				}
			}

			int curDist = 1;
			queue.addFirst(markerPixelIdx);
			while (true)
			{
				int curIdx = queue.removeLast();
				if (curIdx == markerPixelIdx)
				{
					if (queue.isEmpty())
						break;
					queue.addFirst(markerPixelIdx);
					curDist++;
					curIdx = queue.removeLast();
				}

				for (int offset : neighborOffsets)
				{
					int nIdx = curIdx + offset;
					if ((nIdx < 0) || (nIdx >= numPixel))
						continue;
					int nOut = out.getPixel(nIdx);
					if ((d.getPixel(nIdx) < curDist) &&
						((nOut > 0) || (nOut == valueWatershed)))
					{
						int curOut = out.getPixel(curIdx);
						if (nOut > 0)
						{
							if ((curOut == valueMask) ||
								(curOut == valueWatershed))
								out.setPixel(curIdx, nOut);
							else if (curOut != nOut)
								out.setPixel(curIdx, valueWatershed);
						}
						else if (curOut == valueMask)
							out.setPixel(curIdx, valueWatershed);
					}
					else if ((nOut == valueMask) && (d.getPixel(nIdx) == 0))
					{
						d.setPixel(nIdx, curDist + 1);
						queue.addFirst(nIdx);
					}
				}
			}

			for (int j = pStart; j < pEnd; j++)
			{
				int curIdx = pixelIdx[j];
				d.setPixel(curIdx, 0);
				if (out.getPixel(curIdx) == valueMask)
				{
					curLabel++;
					queue.addFirst(curIdx);
					out.setPixel(curIdx, curLabel);
					while (!queue.isEmpty())
					{
						curIdx = queue.removeLast();
						for (int offset : neighborOffsets)
						{
							int nIdx = curIdx + offset;
							if ((nIdx < 0) || (nIdx >= numPixel))
								continue;
							if (out.getPixel(nIdx) == valueMask)
							{
								queue.addFirst(nIdx);
								out.setPixel(nIdx, curLabel);
							}
						}
					}
				}
			}

			pStart = pEnd;
			curIn = nextIn;
		}

		return out;
	}

}
