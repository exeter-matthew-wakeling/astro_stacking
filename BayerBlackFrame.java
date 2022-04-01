import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BayerBlackFrame
{
	public int xsize, ysize, maxVal;
	public float[] mean, stddev;

	public static void main(String[] args) throws Exception {
		List<String> filenames = new ArrayList<String>();
		for (String arg : args) {
			filenames.add(arg);
		}
		BayerBlackFrame frame = new BayerBlackFrame(filenames);
		float minMean = Float.MAX_VALUE;
		float maxMean = -Float.MAX_VALUE;
		float minStddev = Float.MAX_VALUE;
		float maxStddev = -Float.MAX_VALUE;
		for (int i = 0; i < frame.mean.length; i++) {
			minMean = Math.min(minMean, frame.mean[i]);
			maxMean = Math.max(maxMean, frame.mean[i]);
			minStddev = Math.min(minStddev, frame.stddev[i]);
			maxStddev = Math.max(maxStddev, frame.stddev[i]);
		}
		System.err.println("Mean ranges from " + minMean + " to " + maxMean);
		System.err.println("Stddev ranges from " + minStddev + " to " + maxStddev);
		System.out.println("P6 " + frame.xsize + " " + frame.ysize + " 255");
		for (int i = 0; i < frame.mean.length; i++) {
			int mean = (int) (255.99 * (frame.mean[i] - minMean) / (maxMean - minMean));
			int stddev = (int) (255.99 * (frame.stddev[i] - minStddev) / (maxStddev - minStddev));
			System.out.write(stddev);
			System.out.write(mean);
			System.out.write(stddev);
		}
		System.out.flush();
		Arrays.sort(frame.mean);
		Arrays.sort(frame.stddev);
		for (int i = 0; i <= 100; i += 5) {
			int o = (int) ((((long) i) * (frame.mean.length - 1)) / 100);
			System.err.println(i + "th percentile: mean = " + frame.mean[o] + ", stddev = " + frame.stddev[o]);
		}
	}

	public BayerBlackFrame(List<String> filenames) throws IOException, NumberFormatException {
		for (String filename : filenames) {
			Image im = new Image(filename);
			if (mean == null) {
				xsize = im.xsize;
				ysize = im.ysize;
				maxVal = im.maxVal;
				mean = new float[im.buffer.length];
				stddev = new float[im.buffer.length];
			}
			for (int i = 0; i < im.buffer.length; i++) {
				mean[i] += im.buffer[i];
				stddev[i] += im.buffer[i] * im.buffer[i];
			}
		}
		int imageCount = filenames.size();
		for (int i = 0; i < mean.length; i++) {
			stddev[i] = (float) Math.sqrt((stddev[i] - (mean[i] * mean[i] / imageCount)) / imageCount);
			mean[i] = mean[i] / imageCount;
		}
	}

	public float getMean(int x, int y) {
		return mean[x + y * xsize];
	}

	public float getStddev(int x, int y) {
		return mean[x + y * xsize];
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
