import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ToneMap
{
	public static void main(String[] args) throws Exception {
		String filename = args[0];
		Image image = new Image(filename);
		float blackR, blackG, blackB, blackXR, blackXG, blackXB, blackYR, blackYG, blackYB, whiteR, whiteG, whiteB;
		blackXR = blackXG = blackXB = blackYR = blackYG = blackYB = 0.0f;
		int countR = 0;
		int countG = 0;
		int countB = 0;
		float[] histogramR = new float[(image.xsize / 4 + 1) * (image.ysize / 4 + 1)];
		float[] histogramG = new float[(image.xsize / 4 + 1) * (image.ysize / 4 + 1)];
		float[] histogramB = new float[(image.xsize / 4 + 1) * (image.ysize / 4 + 1)];
		for (int y = 0; y < image.ysize; y += 4) {
			for (int x = 0; x < image.xsize; x += 4) {
				int index = 3 * (x + y * image.xsize);
				if (!Float.isInfinite(image.buffer[index])) {
					histogramR[countR++] = image.buffer[index];
				}
				if (!Float.isInfinite(image.buffer[index + 1])) {
					histogramG[countG++] = image.buffer[index + 1];
				}
				if (!Float.isInfinite(image.buffer[index + 2])) {
					histogramB[countB++] = image.buffer[index + 2];
				}
			}
		}
		Arrays.sort(histogramR, 0, countR);
		blackR = histogramR[countR / 2];
		whiteR = histogramR[countR - 1];
		Arrays.sort(histogramG, 0, countG);
		blackG = histogramG[countG / 2];
		whiteG = histogramG[countG - 1];
		Arrays.sort(histogramB, 0, countB);
		blackB = histogramB[countB / 2];
		whiteB = histogramB[countB - 1];
		System.err.println("Calculated black level: " + blackR + ", " + blackG + ", " + blackB);
		System.err.println("Calculated white level: " + whiteR + ", " + whiteG + ", " + whiteB);
		System.err.println("Sampled " + countR + " pixels out of " + ((image.xsize / 4) * (image.ysize / 4)));
		float multR = 2.038880f;
		float multG = 0.936148f;
		float multB = 1.143356f;
		if (whiteR < 40.0f) {
			multR = multG = multB = 1.0f;
		}
		float white = -1.0f;
		float ramp = 10.0f;
		for (int i = 1; i < args.length; i++) {
			if ("-black".equals(args[i])) {
				i++;
				blackR = Float.parseFloat(args[i]);
				i++;
				blackG = Float.parseFloat(args[i]);
				i++;
				blackB = Float.parseFloat(args[i]);
			} else if ("-mult".equals(args[i])) {
				i++;
				multR = Float.parseFloat(args[i]);
				i++;
				multG = Float.parseFloat(args[i]);
				i++;
				multB = Float.parseFloat(args[i]);
			} else if ("-white".equals(args[i])) {
				i++;
				white = Float.parseFloat(args[i]);
			} else if ("-ramp".equals(args[i])) {
				i++;
				ramp = Float.parseFloat(args[i]);
			}
		}
		System.err.println("Using white balance of " + multR + ", " + multG + ", " + multB);
		if (white == -1.0f) {
			white = (whiteR - blackR) * multR;
			white = Math.min(white, (whiteG - blackG) * multG);
			white = Math.min(white, (whiteB - blackB) * multB);
			System.err.println("Calculated white level of " + white);
		}
		// Logarithmic ramp, to emphasise the dark regions. We want the gradient at zero to be "ramp", and the value at 1 to be 1.
		// The equation is of the form y = a log(bx + 1), where the gradient at zero is ab, and the value at 1 is a log(b + 1), which is ramp log(b + 1) / b.
		// We can solve this with a couple of rounds of Newton-Raphson.
		float rampb = 0.0001f;
		for (int i = 0; i < 50; i++) {
			float rampval = (float) (ramp * Math.log(rampb + 1.0) / rampb - 1.0);
			float rampgrad = (float) (ramp / rampb / (rampb + 1.0) - ramp * Math.log(rampb + 1.0) / rampb / rampb);
			//System.err.println(rampval + "\t" + rampgrad);
			rampb = rampb - rampval / rampgrad;
			//System.err.println("Ramp b coefficient: " + rampb);
		}
		System.out.println("P6 " + image.xsize + " " + image.ysize + " 255");
		BufferedOutputStream out = new BufferedOutputStream(System.out);
		for (int i = 0; i < image.buffer.length; i += 3) {
			if (Float.isInfinite(image.buffer[i])) {
				out.write(255);
				out.write(0);
				out.write(0);
			} else {
				float r = (image.buffer[i] - blackR) * multR / white;
				float g = (image.buffer[i + 1] - blackG) * multG / white;
				float b = (image.buffer[i + 2] - blackB) * multB / white;
				if (r > 1.0) {
					r = 1.0f;
				}
				if (g > 1.0) {
					g = 1.0f;
				}
				if (b > 1.0) {
					b = 1.0f;
				}
				float max = (float) Math.max(r, Math.max(g, b));
				float remap = (float) (ramp * Math.log(rampb * max + 1.0) / rampb / max);
				r = srgbGamma(r * remap);
				g = srgbGamma(g * remap);
				b = srgbGamma(b * remap);
				if (r > 1.0) {
					r = 1.0f;
				}
				if (g > 1.0) {
					g = 1.0f;
				}
				if (b > 1.0) {
					b = 1.0f;
				}
				if (r < 0.0) {
					r = 0.0f;
				}
				if (g < 0.0) {
					g = 0.0f;
				}
				if (b < 0.0) {
					b = 0.0f;
				}
				out.write((int) (r * 255.99));
				out.write((int) (g * 255.99));
				out.write((int) (b * 255.99));
			}
		}
		out.flush();
	}

	public static float srgbGamma(float v) {
		if (v < 0.0031308) {
			return v * 323.0f / 25.0f;
		}
		return (float) ((211.0 * Math.pow(v, 5.0 / 12.0) - 11) / 200.0);
	}

	public static class Image
	{
		private int xsize, ysize, len;
		private float buffer[];

		public Image(String filename) throws IOException, NumberFormatException
		{
			DataInputStream is = new DataInputStream(new BufferedInputStream(new FileInputStream(filename)));
			int b = is.read();
			if (b != 'P') {
				throw new NumberFormatException("Invalid PPM file");
			}
			b = is.read();
			if (b != 'F') {
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
			buffer = new float[3 * xsize * ysize];
			for (int i = 0; i < 3 * xsize * ysize; i++) {
				buffer[i] = is.readFloat();
			}
		}
	}
}
