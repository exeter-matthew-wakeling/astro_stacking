import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StitchBayer
{
	public static final double[] BLUR_CIRCLE = new double[20];
	static {
		for (int i = 0; i < 10; i++) {
			BLUR_CIRCLE[i * 2] = Math.sin(i * Math.PI / 5.0);
			BLUR_CIRCLE[i * 2 + 1] = Math.cos(i * Math.PI / 5.0);
		}
	}

	public static volatile long timeRemap = 0L;
	public static volatile long timeReadPixels = 0L;
	public static volatile long timeSort = 0L;
	public static volatile long timeAverage = 0L;

	public static void main(String args[]) throws Exception {
		int xSize = 0;
		int ySize = 0;
		double oWidth = 0.0;
		TreeMap<Double, List<DoTransform>> eevTrans = new TreeMap<Double, List<DoTransform>>();
		TreeMap<Double, List<String>> eevFilenames = new TreeMap<Double, List<String>>();
		List<DoTransform> trans = new ArrayList<DoTransform>();
		BufferedReader project = new BufferedReader(new FileReader(args[0]));
		String outputBase = args[1];
		Mask mask = null;
		boolean blackFiles = false;
		boolean flatFiles = false;
		boolean flatDarkFiles = false;
		List<String> blackFileList = new ArrayList<String>();
		List<String> flatFileList = new ArrayList<String>();
		List<String> flatDarkFileList = new ArrayList<String>();
		int threadCount = 1;
		double redMag = 1.0;
		double blueMag = 1.0;
		double circleRadius = 0.6;
		for (int i = 2; i < args.length; i++) {
			if ("-mask".equals(args[i])) {
				i++;
				mask = new Mask(args[i]);
			} else if ("-black".equals(args[i])) {
				blackFiles = true;
				flatFiles = false;
				flatDarkFiles = false;
			} else if ("-flat".equals(args[i])) {
				blackFiles = false;
				flatFiles = true;
				flatDarkFiles = false;
			} else if ("-flatdark".equals(args[i])) {
				blackFiles = false;
				flatFiles = false;
				flatDarkFiles = true;
			} else if ("-threads".equals(args[i])) {
				i++;
				threadCount = Integer.parseInt(args[i]);
			} else if ("-C".equals(args[i])) {
				i++;
				redMag = Double.parseDouble(args[i]);
				i++;
				blueMag = Double.parseDouble(args[i]);
			} else if ("-circle".equals(args[i])) {
				i++;
				circleRadius = Double.parseDouble(args[i]);
			} else {
				if (blackFiles) {
					blackFileList.add(args[i]);
				} else if (flatFiles) {
					flatFileList.add(args[i]);
				} else if (flatDarkFiles) {
					flatDarkFileList.add(args[i]);
				} else {
					System.err.println("Unrecognised option " + args[i]);
					System.exit(1);
				}
			}
		}
		System.err.println("Loading dark frame from " + blackFileList.size() + " images");
		BayerBlackFrame bayerBlackFrame = new BayerBlackFrame(blackFileList);
		BayerBlackFrame bayerFlatFrame = null;
		if (!flatFileList.isEmpty()) {
			System.err.println("Loading flat frame from " + flatFileList.size() + " images");
			bayerFlatFrame = new BayerBlackFrame(flatFileList);
		}
		BayerBlackFrame bayerFlatDarkFrame = null;
		if (!flatDarkFileList.isEmpty()) {
			System.err.println("Loading flat-dark frame from " + flatDarkFileList.size() + " images");
			bayerFlatDarkFrame = new BayerBlackFrame(flatDarkFileList);
		}
		String line = project.readLine();
		System.err.println("Loading project file");
		while (line != null) {
			if (line.startsWith("p")) {
				String tokens[] = line.split(" ");
				for (String token : tokens) {
					if (token.startsWith("w")) {
						xSize = Integer.parseInt(token.substring(1));
					} else if (token.startsWith("h")) {
						ySize = Integer.parseInt(token.substring(1));
					} else if (token.startsWith("v")) {
						oWidth = Double.parseDouble(token.substring(1));
					}
				}
			} else if (line.startsWith("i")) {
				String tokens[] = line.split(" ");
				double a, b, c, d, e, w, h, p, r, y, v, eev;
				a = b = c = d = e = w = h = p = r = y = v = eev = 0.0;
				String filename = null;
				for (String token : tokens) {
					if (token.startsWith("a=")) {
						a = trans.get(Integer.parseInt(token.substring(2))).getA();
					} else if (token.startsWith("a")) {
						a = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("b=")) {
						b = trans.get(Integer.parseInt(token.substring(2))).getB();
					} else if (token.startsWith("b")) {
						b = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("c=")) {
						c = trans.get(Integer.parseInt(token.substring(2))).getC();
					} else if (token.startsWith("c")) {
						c = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("d=")) {
						d = trans.get(Integer.parseInt(token.substring(2))).getD();
					} else if (token.startsWith("d")) {
						d = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("e=")) {
						e = trans.get(Integer.parseInt(token.substring(2))).getE();
					} else if (token.startsWith("e")) {
						e = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("w=")) {
						w = trans.get(Integer.parseInt(token.substring(2))).getIXsize();
					} else if (token.startsWith("w")) {
						w = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("h=")) {
						h = trans.get(Integer.parseInt(token.substring(2))).getIYsize();
					} else if (token.startsWith("h")) {
						h = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("p=")) {
						p = trans.get(Integer.parseInt(token.substring(2))).getPitch();
					} else if (token.startsWith("p")) {
						p = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("r=")) {
						r = trans.get(Integer.parseInt(token.substring(2))).getRoll();
					} else if (token.startsWith("r")) {
						r = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("y=")) {
						y = trans.get(Integer.parseInt(token.substring(2))).getYaw();
					} else if (token.startsWith("y")) {
						y = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("v=")) {
						v = trans.get(Integer.parseInt(token.substring(2))).getIWidth();
					} else if (token.startsWith("v")) {
						v = Double.parseDouble(token.substring(1));
					} else if (token.startsWith("Eev")) {
						eev = Double.parseDouble(token.substring(3));
					} else if (token.startsWith("n\"")) {
						filename = token.substring(2, token.length() - 1);
					}
				}
				List<DoTransform> trans2 = eevTrans.get(eev);
				List<String> filenames = eevFilenames.get(eev);
				if (trans2 == null) {
					trans2 = new ArrayList<DoTransform>();
					eevTrans.put(eev, trans2);
					filenames = new ArrayList<String>();
					eevFilenames.put(eev, filenames);
				}
				trans2.add(new DoTransform(w, h, v, xSize, ySize, oWidth, y, p, r, a, b, c, d, e));
				trans.add(new DoTransform(w, h, v, xSize, ySize, oWidth, y, p, r, a, b, c, d, e));
				filenames.add(filename);
			}
			line = project.readLine();
		}
		double lowestEev = eevTrans.firstKey();
		//System.out.println(trans);
		//System.out.println(filenames);
		for (Map.Entry<Double, List<DoTransform>> entry : eevTrans.entrySet()) {
			System.err.printf("Eev " + entry.getKey() + " (factor %1.2f) has " + entry.getValue().size() + " images\n", Math.pow(2.0, entry.getKey() - lowestEev));
			System.err.flush();
		}


		trans = eevTrans.firstEntry().getValue();
		List<String> filenames = eevFilenames.firstEntry().getValue();

		System.err.println("Opening image files");
		System.err.flush();
		BayerImage[] images = new BayerImage[filenames.size()];
		float[][] darkOffset = new float[3][];
		darkOffset[0] = new float[filenames.size()];
		darkOffset[1] = new float[filenames.size()];
		darkOffset[2] = new float[filenames.size()];
		for (int i = 0; i < filenames.size(); i++) {
		//for (int i = 0; i < 1; i++) {
			//DoTransform tran = trans.get(i);
			String filename = filenames.get(i);
			//System.err.println("Processing file \"" + filename + "\" with transform " + tran);
			images[i] = new BayerImage(filename);
			int[] red = new int[65536];
			int[] green = new int[65536];
			int[] blue = new int[65536];
			long total = ((long) images[i].getYSize()) * ((long) images[i].getXSize());
			for (int y = 0; y < images[i].getYSize(); y++) {
				for (int x = 0; x < images[i].getXSize(); x++) {
					int v = images[i].getPixel(x, y);
					int pixelChannel = ((x % 2 == 0) && (y % 2 == 0)) ? 0 : (((x % 2 == 1) && (y % 2 == 1)) ? 2 : 1);
					switch (pixelChannel) {
						case 0:
							red[v]++;
							break;
						case 1:
							green[v]++;
							break;
						case 2:
							blue[v]++;
							break;
					}
				}
			}
			darkOffset[0][i] = findMean(total / 4, red);
			darkOffset[1][i] = findMean(total / 2, green);
			darkOffset[2][i] = findMean(total / 4, blue);
			System.err.println(filename + " brightness " + darkOffset[0][i] + " " + darkOffset[1][i] + " " + darkOffset[2][i]);
		}
		float meanRed = 0.0F;
		float meanGreen = 0.0F;
		float meanBlue = 0.0F;
		for (int i = 0; i < filenames.size(); i++) {
			meanRed += darkOffset[0][i];
			meanGreen += darkOffset[1][i];
			meanBlue += darkOffset[2][i];
		}
		meanRed = meanRed / filenames.size();
		meanGreen = meanGreen / filenames.size();
		meanBlue = meanBlue / filenames.size();
		for (int i = 0; i < filenames.size(); i++) {
			darkOffset[0][i] -= meanRed;
			darkOffset[1][i] -= meanGreen;
			darkOffset[2][i] -= meanBlue;
		}

		int iXsize = images[0].getXSize();
		int iYsize = images[0].getYSize();
		int maxVal = images[0].getMaxVal();
		float[] output = new float[xSize * ySize * 3];
		int[] sourceCount = new int[xSize * ySize];
		System.err.println("Building image");
		System.err.flush();
		long startTime = System.currentTimeMillis();
		Runner[] runners = new Runner[threadCount];
		for (int i = 0; i < threadCount; i++) {
			System.err.println("Starting thread with y from " + (ySize * i / threadCount) + " to " + (ySize * (i + 1) /threadCount));
			Runner runner = new Runner(xSize, ySize, iXsize, iYsize, trans, images, bayerBlackFrame, bayerFlatFrame, bayerFlatDarkFrame, output, ySize * i / threadCount, ySize * (i + 1) / threadCount, redMag, blueMag, circleRadius, sourceCount, darkOffset);
			runners[i] = runner;
			Thread thread = new Thread(runner);
			thread.start();
		}
		System.err.println("---------------------------------------------------------------------");
		for (int i = 0; i < threadCount; i++) {
			runners[i].waitForFinish();
		}
		System.err.println("took " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
		System.err.println("Time taken\t" + timeRemap + "\t" + timeReadPixels + "\t" + timeSort + "\t" + timeAverage);
		//buildOutputBayer(xSize, ySize, iXsize, iYsize, trans, images, bayerBlackFrame, bayerFlatFrame, output);
		/*PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputBase + "_image1.pnm")));
		out.println("PF " + xSize + " " + ySize);
		DataOutputStream dout = new DataOutputStream(out);
		for (int i = 0; i < xSize * ySize * 3; i++) {
			dout.writeFloat(output[i]);
			output[i] -= 5.0F;
			if (output[i] < 0.0) output[i] = 0.0F;
			if (output[i] > 65535.0) output[i] = 65535.0F;
		}
		dout.close();*/
/*		Iterator<Double> iter = eevTrans.keySet().iterator();
		iter.next();
		float lastMaxValue = 32000.0F;
		while (iter.hasNext()) {
			Double eev = iter.next();
			Double factor = Math.pow(2.0, eev - lowestEev);
			System.err.printf("Processing Eev " + eev + " (factor %1.2f)\n", factor);
			trans = eevTrans.get(eev);
			filenames = eevFilenames.get(eev);
			images = new Image[filenames.size()];
			for (int i = 0; i < filenames.size(); i++) {
				String filename = filenames.get(i);
				images[i] = new Image(filename);
			}
			for (int i = 0; i < iXsize * iYsize * 3; i++) {
				b1[i] = 0.0F;
				weights[i] = 1.0F;
			}
			buildOutput(xSize, ySize, iXsize, iYsize, trans, images, b1, weights, output, iVariation, lastMaxValue, factor);
			lastMaxValue = (float) (factor * 32000.0);
		}*/
		PrintStream out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputBase + "_image.pfm")));
		out.println("PF " + xSize + " " + ySize);
		DataOutputStream dout = new DataOutputStream(out);
		for (int i = 0; i < xSize * ySize * 3; i++) {
			dout.writeFloat(output[i]);
		}
		dout.close();
		out = new PrintStream(new BufferedOutputStream(new FileOutputStream(outputBase + "_sourceCount.pnm")));
		out.println("P5 " + xSize + " " + ySize + " " + images.length);
		for (int i = 0; i < xSize * ySize; i++) {
			if (images.length > 255) {
				out.write(sourceCount[i] / 256);
				out.write(sourceCount[i] & 0xFF);
			} else {
				out.write(sourceCount[i]);
			}
		}
		out.close();
	}

	public static void buildOutputBayer(int xSize, int ySize, int iXsize, int iYsize, List<DoTransform> trans, BayerImage[] images, BayerBlackFrame bayerBlackFrame, BayerBlackFrame bayerFlatFrame, BayerBlackFrame bayerFlatDarkFrame, float[] output, int ymin, int ymax, double redMag, double blueMag, double circleRadius, int[] sourceCount, float[][] darkOffset) {
		List<WeightedValue>[] values = new List[3];
		int dots = (70 * ymin) / ySize;
		long startTime = System.currentTimeMillis();
		long lTimeRemap = 0L;
		long lTimeReadPixels = 0L;
		long lTimeSort = 0L;
		long lTimeAverage = 0L;
		for (int y = ymin; y < ymax; y++) {
			for (int x = 0; x < xSize; x++) {
				for (int i = 0; i < 3; i++) {
					values[i] = new ArrayList<WeightedValue>();
				}
				int oIndex = 3 * (x + y * xSize);
				int sourceCountInt = 0;
				for (int i = 0; i < images.length; i++) {
					long time1 = System.currentTimeMillis();
					BayerImage image = images[i];
					double xtMin = Double.MAX_VALUE;
					double xtMax = -Double.MAX_VALUE;
					double ytMin = Double.MAX_VALUE;
					double ytMax = -Double.MAX_VALUE;
					for (int o = 0; o < 10; o++) {
						DoTransform.Point p = trans.get(i).outToIn(x + circleRadius * BLUR_CIRCLE[o * 2], y + circleRadius * BLUR_CIRCLE[o * 2 + 1]);
						xtMin = Math.min(xtMin, p.getX());
						xtMax = Math.max(xtMax, p.getX());
						ytMin = Math.min(ytMin, p.getY());
						ytMax = Math.max(ytMax, p.getY());
					}
					long time2 = System.currentTimeMillis();
					//System.err.println("xtMin: " + xtMin + ", xtMax: " + xtMax + ", ytMin: " + ytMin + ", ytMax: " + ytMax);
					boolean usedSource = false;
					for (int channel = 0; channel < 3; channel++) {
						double mag = channel == 0 ? redMag : (channel == 1 ? 1.0 : blueMag);
						int xtMinInt = (int) ((xtMin - circleRadius - (iXsize / 2.0)) / mag + (iXsize / 2.0));
						int xtMaxInt = (int) ((xtMax + circleRadius - (iXsize / 2.0)) / mag + (iXsize / 2.0));
						int ytMinInt = (int) ((ytMin - circleRadius - (iYsize / 2.0)) / mag + (iYsize / 2.0));
						int ytMaxInt = (int) ((ytMax + circleRadius - (iYsize / 2.0)) / mag + (iYsize / 2.0));
						if ((ytMaxInt - ytMinInt) * (xtMaxInt - xtMinInt) > 100) {
							System.err.println("Too many pixels: x = " + x + ", y = " + y + ", xtMin = " + xtMin + ", xtMax = " + xtMax + ", ytMin = " + ytMin + ", ytMax = " + ytMax);
						}
						for (int yt = ytMinInt; yt <= ytMaxInt; yt++) {
							for (int xt = xtMinInt; xt <= xtMaxInt; xt++) {
								if ((xt >= 0) && (xt < iXsize) && (yt >= 0) && (yt < iYsize)) {
									int index = xt + yt * iXsize;
									int pixelChannel = ((xt % 2 == 0) && (yt % 2 == 0)) ? 0 : (((xt % 2 == 1) && (yt % 2 == 1)) ? 2 : 1);
									if (channel == pixelChannel) {
										usedSource = true;
										float black = bayerBlackFrame.getMean(xt, yt);
										float weight = bayerBlackFrame.getStddev(xt, yt);
										weight = 1.0f / weight / weight;
										float flatBlack = bayerFlatDarkFrame == null ? black : bayerFlatDarkFrame.getMean(xt, yt);
										float white = bayerFlatFrame == null ? 1.0f : (bayerFlatFrame.getMean(xt, yt) - flatBlack);
										if (white > 0.0) {
											values[channel].add(new WeightedValue((image.getPixel(xt, yt) - black - darkOffset[channel][i]) / white, weight));
										}
									}
								}
							}
						}
					}
					if (usedSource) {
						sourceCountInt++;
					}
					long time3 = System.currentTimeMillis();
					lTimeRemap += time2 - time1;
					lTimeReadPixels += time3 - time2;
				}
				boolean infinite = false;
				for (int channel = 0; channel < 3; channel++) {
					long time1 = System.currentTimeMillis();
					Collections.sort(values[channel]);
					long time2 = System.currentTimeMillis();
					int clip = values[channel].size() / 15;
					float sum = 0.0F;
					float weight = 0.0F;
					for (int i = clip; i < values[channel].size() - clip; i++) {
						WeightedValue v = values[channel].get(i);
						if (v.getWeight() > 0.0) {
							sum += v.getValue() * v.getWeight();
							weight += v.getWeight();
						}
					}
					if (weight == 0.0) {
						output[3 * (x + y * xSize) + channel] = (channel == 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY);
						infinite = true;
					} else if (weight > 0.0) {
						output[3 * (x + y * xSize) + channel] = (float) (sum / weight);
					}
					long time3 = System.currentTimeMillis();
					lTimeSort += time2 - time1;
					lTimeAverage += time3 - time2;
				}
				if (infinite) {
					output[3 * (x + y * xSize)] = Float.POSITIVE_INFINITY;
					output[3 * (x + y * xSize) + 1] = Float.NEGATIVE_INFINITY;
					output[3 * (x + y * xSize) + 2] = Float.NEGATIVE_INFINITY;
				}
				sourceCount[x + y * xSize] = sourceCountInt;
			}
			int newDots = (70 * y) / ySize;
			while (newDots > dots) {
				System.err.print(".");
				System.err.flush();
				dots++;
			}
		}
		timeRemap += lTimeRemap;
		timeReadPixels += lTimeReadPixels;
		timeSort += lTimeSort;
		timeAverage += lTimeAverage;
		//System.err.println("took " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
	}

	public static void buildOutput(int xSize, int ySize, int iXsize, int iYsize, List<DoTransform> trans, Image[] images, float[] b1, float[] weights, float[] output, float[] iVariation, float maxValue, double factor) {
		WeightedValue[][] values = new WeightedValue[3][];
		for (int i = 0; i < 3; i++) {
			values[i] = new WeightedValue[images.length];
		}
		int dots = 0;
		long startTime = System.currentTimeMillis();
		for (int y = 0; y < ySize; y++) {
			for (int x = 0; x < xSize; x++) {
		//for (int y = 1200; y < 1300; y++) {
		//	for (int x = 1900; x < 2700; x++) {
				int oIndex = 3 * (x + y * xSize);
				if ((maxValue == 0.0F) || (output[oIndex] >= maxValue) || (output[oIndex + 1] >= maxValue) || (output[oIndex + 2] >= maxValue)) {
					int valueNo = 0;
					for (int i = 0; i < images.length; i++) {
						Image image = images[i];
						DoTransform.Point p = trans.get(i).outToIn(x, y);
						int xt = (int) p.getX();
						int yt = (int) p.getY();
						if ((xt >= 0) && (xt < iXsize) && (yt >= 0) && (yt < iYsize)) {
							int index = xt + yt * iXsize;
							for (int channel = 0; channel < 3; channel++) {
								values[channel][valueNo] = new WeightedValue(image.getPixel(xt, yt, channel) - b1[index * 3 + channel] * iVariation[i * 3 + channel], weights[index * 3 + channel]);
							}
							valueNo++;
						}
					}
					boolean infinite = false;
					for (int channel = 0; channel < 3; channel++) {
						if ((maxValue == 0.0) || ((output[oIndex + channel] >= maxValue) && (!Float.isInfinite(output[oIndex + channel])))) {
							Arrays.sort(values[channel], 0, valueNo);
							int clip = valueNo / 15;
							float sum = 0.0F;
							float weight = 0.0F;
							for (int i = clip; i < valueNo - clip; i++) {
								WeightedValue v = values[channel][i];
								if (v.getWeight() > 0.0) {
									sum += v.getValue() * v.getWeight();
									weight += v.getWeight();
								}
							}
							if ((weight == 0.0) && (maxValue == 0.0)) {
								output[3 * (x + y * xSize) + channel] = (channel == 0 ? Float.POSITIVE_INFINITY : Float.NEGATIVE_INFINITY);
								infinite = true;
							} else if (weight > 0.0) {
								output[3 * (x + y * xSize) + channel] = (float) ((factor * sum) / weight);
							}
						}
					}
					if (infinite) {
						output[3 * (x + y * xSize)] = Float.POSITIVE_INFINITY;
						output[3 * (x + y * xSize) + 1] = Float.NEGATIVE_INFINITY;
						output[3 * (x + y * xSize) + 2] = Float.NEGATIVE_INFINITY;
					}
				}
			}
			int newDots = (70 * y) / ySize;
			while (newDots > dots) {
				System.err.print(".");
				System.err.flush();
				dots++;
			}
		}
		System.err.println("took " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
	}

	public static class WeightedValue implements Comparable<WeightedValue>
	{
		private float value, weight;

		public WeightedValue(float value, float weight) {
			this.value = value;
			this.weight = weight;
		}

		public float getValue() {
			return value;
		}

		public float getWeight() {
			return weight;
		}

		public int compareTo(WeightedValue o) {
			if (value > o.value) {
				return 1;
			} else if (value < o.value) {
				return -1;
			} else {
				return 0;
			}
		}
	}

	public static void buildCorrelations(int xSize, int ySize, int iXsize, int iYsize, List<DoTransform> trans, Image[] images, float[] b1, float[] weights, float[] output, float[] iVariation, Mask mask, String output_filename) throws Exception {
		float[][] stats = new float[3][];
		float[][] stats2 = new float[3][];
		for (int i = 0; i < 3; i++) {
			stats[i] = new float[images.length];
			stats2[i] = new float[images.length];
		}
		int[] iVarCounts = new int[iVariation.length];
		double[] iVarSums = new double[iVariation.length];
		for (int i = 0; i < iVariation.length; i++) {
			iVarSums[i] = 0.0;
			iVarCounts[i] = 0;
		}
		int dots = 0;
		long startTime = System.currentTimeMillis();
		//PrintWriter out = new PrintWriter(new FileWriter(output_filename));
		float[] b1Sum = new float[3];
		b1Sum[0] = b1Sum[1] = b1Sum[2] = 0.0F;
		for (int y = 0; y < iYsize; y++) {
			for (int x = 0; x < iXsize; x++) {
		//for (int y = 1100; y < 1200; y++) {
		//	for (int x = 1800; x < 1900; x++) {
				if ((mask == null) || mask.isIn(x, y)) {
					int valueNo = 0;
					for (int i = 0; i < images.length; i++) {
						Image image = images[i];
						DoTransform.Point p = trans.get(i).inToOut(x + 0.5, y + 0.5);
						int xt = (int) (p.getX() + 0.5);
						int yt = (int) (p.getY() + 0.5);
						if ((xt >= 0) && (xt < xSize) && (yt >= 0) && (yt < ySize)
								&& (!(Float.isInfinite(output[3 * (xt + yt * xSize)])
										|| Float.isInfinite(output[3 * (xt + yt * xSize) + 1])
										|| Float.isInfinite(output[3 * (xt + yt * xSize) + 2]))))	{
							for (int channel = 0; channel < 3; channel++) {
								stats[channel][valueNo] = image.getPixel(x, y, channel) - output[3 * (xt + yt * xSize) + channel];
								stats2[channel][valueNo] = stats[channel][valueNo];
								iVarSums[i * 3 + channel] += stats[channel][valueNo];
							}
							valueNo++;
							iVarCounts[i]++;
						}
					}
					int clip = valueNo / 15;
					for (int channel = 0; channel < 3; channel++) {
						Arrays.sort(stats[channel], 0, valueNo);
						float sum = 0.0F;
						float ssum = 0.0F;
						float weight = 0.0F;
						for (int i = clip; i < valueNo - clip; i++) {
							sum += stats[channel][i];
							ssum += stats[channel][i] * stats[channel][i];
							weight += 1.0;
						}
						if (weight > 2.0) {
							b1[3 * (x + y * iXsize) + channel] = sum / weight;
							b1Sum[channel] += sum / weight;
							weights[3 * (x + y * iXsize) + channel] = 1.0F / (1.0F + (ssum - sum * sum / weight) / weight);
						} else {
							b1[3 * (x + y * iXsize) + channel] = 0.0F;
							weights[3 * (x + y * iXsize) + channel] = 0.0F;
						}
						//if ((x % 100 == 50) && (y % 100 == 50)) {
						//	for (int i = 0; i < valueNo; i++) {
						//		out.println(x + "\t" + y + "\t" + channel + "\t" + i + "\t" + stats[channel][i] + "\t" + stats2[channel][i] + "\t" + b1[3 * (x + y * iXsize) + channel]);
						//	}
						//}
					}
				} else {
					for (int channel = 0; channel < 3; channel++) {
						b1[3 * (x + y * iXsize) + channel] = 0.0F;
						weights[3 * (x + y * iXsize) + channel] = 0.0F;
					}
				}
			}

			int newDots = (70 * y) / iYsize;
			while (newDots > dots) {
				System.err.print(".");
				System.err.flush();
				dots++;
			}
		}
		double iVarRed, iVarGreen, iVarBlue;
		iVarRed = iVarGreen = iVarBlue = 0.0;
		for (int i = 0; i < images.length; i++) {
			iVarRed += iVarSums[i * 3];
			iVarGreen += iVarSums[i * 3 + 1];
			iVarBlue += iVarSums[i * 3 + 2];
		}
		//out.close();
		//out = new PrintWriter(new FileWriter(output_filename + "_variation"));
		for (int i = 0; i < images.length; i++) {
			iVariation[i * 3] = (float) (iVarSums[i * 3] * images.length / iVarRed);
			iVariation[i * 3 + 1] = (float) (iVarSums[i * 3 + 1] * images.length / iVarGreen);
			iVariation[i * 3 + 2] = (float) (iVarSums[i * 3 + 2] * images.length / iVarBlue);
			//out.println(i + "\t" + iVariation[i * 3] + "\t" + iVariation[i * 3 + 1] + "\t" + iVariation[i * 3 + 2]);
		}
		//out.close();

		b1Sum[0] = b1Sum[0] / iYsize / iXsize / 10.0F;
		b1Sum[1] = b1Sum[1] / iYsize / iXsize / 10.0F;
		b1Sum[2] = b1Sum[2] / iYsize / iXsize / 10.0F;
		for (int y = 0; y < iYsize; y++) {
			for (int x = 0; x < iXsize; x++) {
				for (int channel = 0; channel < 3; channel++) {
					// If the sum is implausibly low, reject the dead pixel by settings its weight low.
					if (b1[3 * (x + y * iXsize) + channel] < b1Sum[channel]) {
						weights[3 * (x + y * iXsize) + channel] = 0.0F;
					}
				}
			}
		}
		System.err.println("took " + ((System.currentTimeMillis() - startTime) / 1000) + "s");
	}

	public static class Image
	{
		private int xsize, ysize, maxVal;
		private MappedByteBuffer buffer;
		private int offset = 0;

		public Image(String filename) throws IOException, NumberFormatException
		{
			RandomAccessFile raf = new RandomAccessFile(filename, "r");
			buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
			raf.close();
			int b = buffer.get(offset++) & 255;
			if (b != 'P') {
				throw new NumberFormatException("Invalid PPM file");
			}
			b = buffer.get(offset++) & 255;
			if (b != '6') {
				throw new NumberFormatException("invalid PPM file");
			}
			b = buffer.get(offset++) & 255;
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = buffer.get(offset++) & 255;
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			xsize = 0;
			while ((b >= '0') && (b <= '9')) {
				xsize = (xsize * 10) + b - '0';
				b = buffer.get(offset++) & 255;
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = buffer.get(offset++) & 255;
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			ysize = 0;
			while ((b >= '0') && (b <= '9')) {
				ysize = (ysize * 10) + b - '0';
				b = buffer.get(offset++) & 255;
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = buffer.get(offset++) & 255;
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			maxVal = 0;
			while ((b >= '0') && (b <= '9')) {
				maxVal = (maxVal * 10) + b - '0';
				b = buffer.get(offset++) & 255;
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
		}

		public int getR(int x, int y) {
			return doPixel(3 * (x + y * xsize));
		}
		public int getG(int x, int y) {
			return doPixel(3 * (x + y * xsize) + 1);
		}
		public int getB(int x, int y) {
			return doPixel(3 * (x + y * xsize) + 2);
		}

		public int getPixel(int x, int y, int channel) {
			return doPixel(3 * (x + y * xsize) + channel);
		}

		public int doPixel(int pos) {
			if (maxVal > 255) {
				return (buffer.get(pos * 2 + offset) & 255) * 256 + (buffer.get(pos * 2 + offset + 1) & 255);
			} else {
				return buffer.get(pos + offset) & 255;
			} 
		}
		public int getXSize() {
			return xsize;
		}
		public int getYSize() {
			return ysize;
		}
		public int getMaxVal() {
			return maxVal;
		}
	}

	public static class BayerImage
	{
		private int xsize, ysize, maxVal;
		private MappedByteBuffer buffer;
		private int offset = 0;

		public BayerImage(String filename) throws IOException, NumberFormatException
		{
			RandomAccessFile raf = new RandomAccessFile(filename, "r");
			buffer = raf.getChannel().map(FileChannel.MapMode.READ_ONLY, 0, raf.length());
			raf.close();
			int b = buffer.get(offset++) & 255;
			if (b != 'P') {
				throw new NumberFormatException("Invalid PGM file");
			}
			b = buffer.get(offset++) & 255;
			if (b != '5') {
				throw new NumberFormatException("invalid PGM file");
			}
			b = buffer.get(offset++) & 255;
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PGM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = buffer.get(offset++) & 255;
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PGM file");
			}
			xsize = 0;
			while ((b >= '0') && (b <= '9')) {
				xsize = (xsize * 10) + b - '0';
				b = buffer.get(offset++) & 255;
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PGM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = buffer.get(offset++) & 255;
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PGM file");
			}
			ysize = 0;
			while ((b >= '0') && (b <= '9')) {
				ysize = (ysize * 10) + b - '0';
				b = buffer.get(offset++) & 255;
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PGM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = buffer.get(offset++) & 255;
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PGM file");
			}
			maxVal = 0;
			while ((b >= '0') && (b <= '9')) {
				maxVal = (maxVal * 10) + b - '0';
				b = buffer.get(offset++) & 255;
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PGM file");
			}
		}

		public int getPixel(int x, int y) {
			return doPixel(x + y * xsize);
		}

		public int doPixel(int pos) {
			if (maxVal > 255) {
				return (buffer.get(pos * 2 + offset) & 255) * 256 + (buffer.get(pos * 2 + offset + 1) & 255);
			} else {
				return buffer.get(pos + offset) & 255;
			} 
		}
		public int getXSize() {
			return xsize;
		}
		public int getYSize() {
			return ysize;
		}
		public int getMaxVal() {
			return maxVal;
		}
	}

	public static class Mask
	{
		private int xsize, ysize;
		private byte buffer[];

		public Mask(String filename) throws IOException, NumberFormatException
		{
			InputStream is = new BufferedInputStream(new FileInputStream(filename));
			int b = is.read();
			if (b != 'P') {
				throw new NumberFormatException("Invalid PPM file");
			}
			b = is.read();
			if (b != '4') {
				throw new NumberFormatException("invalid PPM file");
			}
			b = is.read();
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = is.read();
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			xsize = 0;
			while ((b >= '0') && (b <= '9')) {
				xsize = (xsize * 10) + b - '0';
				b = is.read();
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = is.read();
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			ysize = 0;
			while ((b >= '0') && (b <= '9')) {
				ysize = (ysize * 10) + b - '0';
				b = is.read();
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			buffer = new byte[((xsize + 7) / 8) * ysize];
			is.read(buffer);
		}

		public boolean isIn(int x, int y) {
			byte retval = buffer[(x / 8) + y * ((xsize + 7) / 8)];
			return (retval & (128 >> (x % 8))) == 0;
		}
	}

	public static class Runner implements Runnable
	{
		private int xSize, ySize, iXsize, iYsize, ymin, ymax;
		private List<DoTransform> trans;
		private BayerImage[] images;
		private BayerBlackFrame bayerBlackFrame, bayerFlatFrame, bayerFlatDarkFrame;
		private float[] output;
		private int[] sourceCount;
		private boolean finished = false;
		private double redMag, blueMag, circleRadius;
		float[][] darkOffset;

		public Runner(int xSize, int ySize, int iXsize, int iYsize, List<DoTransform> trans, BayerImage[] images, BayerBlackFrame bayerBlackFrame, BayerBlackFrame bayerFlatFrame, BayerBlackFrame bayerFlatDarkFrame, float[] output, int ymin, int ymax, double redMag, double blueMag, double circleRadius, int[] sourceCount, float[][] darkOffset) {
			this.xSize = xSize;
			this.ySize = ySize;
			this.iXsize = iXsize;
			this.iYsize = iYsize;
			this.trans = trans;
			this.images = images;
			this.bayerBlackFrame = bayerBlackFrame;
			this.bayerFlatFrame = bayerFlatFrame;
			this.bayerFlatDarkFrame = bayerFlatDarkFrame;
			this.output = output;
			this.ymin = ymin;
			this.ymax = ymax;
			this.redMag = redMag;
			this.blueMag = blueMag;
			this.circleRadius = circleRadius;
			this.sourceCount = sourceCount;
			this.darkOffset = darkOffset;
		}

		public void run() {
			buildOutputBayer(xSize, ySize, iXsize, iYsize, trans, images, bayerBlackFrame, bayerFlatFrame, bayerFlatDarkFrame, output, ymin, ymax, redMag, blueMag, circleRadius, sourceCount, darkOffset);
			synchronized (this) {
				finished = true;
				notifyAll();
			}
		}

		public synchronized void waitForFinish() {
			while (!finished) {
				try {
					wait();
				} catch (InterruptedException e) {
				}
			}
		}
	}

	public static float findMean(long total, int[] histogram) {
		long toSkip = total / 100L;
		long toInclude = total - 2 * toSkip;
		double doubleToInclude = toInclude;
		long sum = 0L;
		for (int i = 0; i < histogram.length; i++) {
			int c = histogram[i];
			if (c <= toSkip) {
				toSkip -= c;
			} else if (toInclude > 0) {
				long use = Math.min(toInclude, c - toSkip);
				sum += i * use;
				toSkip = 0;
				toInclude -= use;
			}
		}
		return (float) (sum / doubleToInclude);
	}
}
