import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

public class BayerHistogram
{
	public static void main(String[] args) throws Exception {
		int argNo = 0;
		boolean sqrt = false;
		boolean mean = false;
		if ("-sqrt".equals(args[argNo])) {
			argNo++;
			sqrt = true;
		} else if ("-mean".equals(args[argNo])) {
			argNo++;
			mean = true;
		}
		for (; argNo < args.length; argNo++) {
			int[] red = new int[sqrt ? 256 : 65536];
			int[] green = new int[sqrt ? 256 : 65536];
			int[] blue = new int[sqrt ? 256 : 65536];
			long total = 0L;
			Image image = new Image(args[argNo]);
			total += image.buffer.length;
			for (int y = 0; y < image.ysize; y++) {
				for (int x = 0; x < image.xsize; x++) {
					int i = y * image.xsize + x;
					int v = sqrt ? ((int) Math.sqrt(image.buffer[i])) : image.buffer[i];
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
			if (mean) {
				double darkGreen = findMean(total / 2, green, false, 100L);
				double dark = (findMean(total / 4, red, false, 100L) + 2.0 * darkGreen + findMean(total / 4, blue, false, 100L)) / 4.0;
				double middle = (findMean(total / 4, red, true, 100L) + 2.0 * findMean(total / 2, green, true, 100L) + findMean(total / 4, blue, true, 100L)) / 4.0;
				double midbrightGreen = findMean(total / 2, green, true, 3000L);
				double midbright = (findMean(total / 4, red, true, 3000L) + 2.0 * midbrightGreen + findMean(total / 4, blue, true, 3000L)) / 4.0;
				double bright = (findMean(total / 4, red, true, 100000L) + 2.0 * findMean(total / 2, green, true, 100000L) + findMean(total / 4, blue, true, 100000L)) / 4.0;
				System.out.println(args[argNo] + "\t" + dark + "\t" + middle + "\t" + midbright + "\t" + bright + "\t" + findFwhm(image, false, darkGreen, midbrightGreen));
			} else {
				for (int i = 0; i < red.length; i++) {
					System.out.println(i + "\t" + red[i] + "\t" + green[i] + "\t" + blue[i]);
				}
			}
		}
	}

	public static String findFwhm(Image image, boolean verbose, double dark, double midbright) {
		int xsize = image.xsize;
		int ysize = image.ysize;
/*		int[] sortBuffer = new int[image.buffer.length / 2];
		int sortPos = 0;
		for (int y = ysize / 3; y < (ysize * 2) / 3; y++) {
			for (int x = xsize / 3; x < (xsize * 2) / 3; x++) {
				int pixelChannel = ((x % 2 == 0) && (y % 2 == 0)) ? 0 : (((x % 2 == 1) && (y % 2 == 1)) ? 2 : 1);
				if (pixelChannel == 1) {
					sortBuffer[sortPos++] = image.buffer[x + y * xsize];
				}
			}
		}
		Arrays.sort(sortBuffer);
		int limit = sortBuffer[sortBuffer.length - sortBuffer.length / 1000];
		sortBuffer = null;*/
		int limit = (int) midbright;
		//System.err.println("Limit = " + limit);
		TreeSet<ProduceMatches3.Point> points = new TreeSet<ProduceMatches3.Point>();
		for (int y = ysize / 6; y < (ysize * 5) / 6; y++) {
			for (int x = xsize / 6; x < (xsize * 5) / 6; x++) {
				int pixelChannel = ((x % 2 == 0) && (y % 2 == 0)) ? 0 : (((x % 2 == 1) && (y % 2 == 1)) ? 2 : 1);
				if (pixelChannel == 1) {
					if (image.buffer[x + y * xsize] > limit) {
						points.add(new ProduceMatches3.Point(x, y, image.buffer[x + y * xsize]));
					}
				}
			}
		}
		int margin = xsize / 500;
		int smargin = margin * margin;
		ArrayList<ProduceMatches3.Point> chosenPoints = new ArrayList<ProduceMatches3.Point>();
		while (!points.isEmpty()) {
			ProduceMatches3.Point p = points.first();
			chosenPoints.add(p);
			Iterator<ProduceMatches3.Point> iter = points.iterator();
			while (iter.hasNext()) {
				ProduceMatches3.Point n = iter.next();
				double xdist = n.getX() - p.getX();
				double ydist = n.getY() - p.getY();
				if (xdist * xdist + ydist * ydist < smargin) {
					iter.remove();
				}
			}
		}
		ArrayList<ProduceMatches3.Point> chosenPoints2 = new ArrayList<ProduceMatches3.Point>();
		for (ProduceMatches3.Point point : chosenPoints) {
			// Refine the point by finding the centre of mass
			double sumx = 0.0;
			double sumy = 0.0;
			double sum = 0.0;
			int max = 0;
			int px = (int) point.getX();
			int py = (int) point.getY();
			for (int y = py - 10; y <= py + 10; y++) {
				for (int x = px - 10; x <= px + 10; x++) {
					int pixelChannel = ((x % 2 == 0) && (y % 2 == 0)) ? 0 : (((x % 2 == 1) && (y % 2 == 1)) ? 2 : 1);
					if (pixelChannel == 1) {
						int c = image.buffer[x + y * xsize] - ((int) dark);
						sumx += x * c;
						sumy += y * c;
						sum += c;
						max = Math.max(max, c);
					}
				}
			}
			sumx = sumx / sum;
			sumy = sumy / sum;
			//System.err.println("Refined point at (" + point.getX() + ", " + point.getY() + ") to (" + sumx + ", " + sumy + ")");
			chosenPoints2.add(new ProduceMatches3.Point(sumx, sumy, max));
		}
		chosenPoints = chosenPoints2;
		double sumCountHalf = 0.0;
		double sumCount20 = 0.0;
		int starCount = 0;
		for (ProduceMatches3.Point point : chosenPoints) {
			// Find the number of pixels that have a value at least half the maximum, and also 1/20th the maximum.
			int countHalf = 0;
			int count20 = 0;
			int px = (int) point.getX();
			int py = (int) point.getY();
			for (int y = py - 10; y <= py + 10; y++) {
				for (int x = px - 10; x <= px + 10; x++) {
					int pixelChannel = ((x % 2 == 0) && (y % 2 == 0)) ? 0 : (((x % 2 == 1) && (y % 2 == 1)) ? 2 : 1);
					if (pixelChannel == 1) {
						int c = image.buffer[x + y * xsize] - ((int) dark);
						if (c >= point.getC() / 2) {
							countHalf++;
						}
						if (c >= point.getC() / 10) {
							count20++;
						}
					}
				}
			}
			if (verbose) {
				System.out.println(point.getX() + "\t" + point.getY() + "\t" + point.getC() + "\t" + Math.sqrt(2.0 * countHalf) + "\t" + Math.sqrt(2.0 * count20));
			}
			if (point.getC() < 13000) {
				sumCountHalf += Math.sqrt(2.0 * countHalf);
				sumCount20 += Math.sqrt(2.0 * count20);
				starCount++;
			}
		}
		return starCount + "\t" + (sumCountHalf / starCount) + "\t" + (sumCount20 / starCount);
	}

	public static double findMean(long total, int[] histogram, boolean top, long divider) {
		long toSkip = total / divider;
		long toInclude = total - 2 * toSkip;
		if (top) {
			toSkip = total - (total / divider);
			toInclude = total / divider;
		}
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
		return sum / doubleToInclude;
	}

	public static class Image
	{
		private int xsize, ysize, len, maxVal;
		private int buffer[];

		public Image(String filename) throws IOException, NumberFormatException
		{
			DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
			int b = is.read();
			if (b != 'P') {
				throw new NumberFormatException("Invalid PPM file");
			}
			b = is.read();
			if (b != '5') {
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
			while ((b == ' ') || (b == '\t') || (b == '\n')) {
				b = is.read();
			}
			if ((b < '0') || (b > '9')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			maxVal = 0;
			while ((b >= '0') && (b <= '9')) {
				maxVal = (ysize * 10) + b - '0';
				b = is.read();
			}
			if ((b != ' ') && (b != '\t') && (b != '\n')) {
				throw new NumberFormatException("Invalid PPM file");
			}
			buffer = new int[xsize * ysize];
			len = 0;
			for (int i = 0; i < xsize * ysize; i++) {
				buffer[len++] = is.read() * 256 + is.read();
			}
			is.close();
		}
	}
}
