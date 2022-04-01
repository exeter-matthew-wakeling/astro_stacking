import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class ConvertToLinearPpm
{
	public static void main(String[] args) throws Exception {
		String filename = args[0];
		Image image = new Image(filename);
		float whiteR, whiteG, whiteB;
		whiteR = whiteG = whiteB = 0.0f;
		for (int y = 0; y < image.ysize; y += 1) {
			for (int x = 0; x < image.xsize; x += 1) {
				int index = 3 * (x + y * image.xsize);
				if ((!Float.isInfinite(image.buffer[index])) && (!Float.isInfinite(image.buffer[index + 1])) && (!Float.isInfinite(image.buffer[index + 2]))) {
					whiteR = Math.max(whiteR, image.buffer[index]);
					whiteG = Math.max(whiteG, image.buffer[index + 1]);
					whiteB = Math.max(whiteB, image.buffer[index + 2]);
				}
			}
		}
		System.err.println("Calculated white level: " + whiteR + ", " + whiteG + ", " + whiteB);
		float white = Math.max(whiteR, Math.max(whiteG, whiteB));
		if ("-mult".equals(args[1])) {
			white = white / Float.parseFloat(args[2]);
		} else if ("-white".equals(args[1])) {
			white = Float.parseFloat(args[2]);
		}
		System.out.println("P6 " + image.xsize + " " + image.ysize + " 65535");
		for (int i = 0; i < image.buffer.length; i += 3) {
			if ((!Float.isInfinite(image.buffer[i])) && (!Float.isInfinite(image.buffer[i + 1])) && (!Float.isInfinite(image.buffer[i + 2]))) {
				for (int channel = 0; channel < 3; channel++) {
					float v = image.buffer[i + channel] / white;
					if (v > 1.0) {
						v = 1.0F;
					}
					if (!(v >= 0.0)) {
						v = 0.0F;
					}
					int o = (int) (65535.9999 * v);
					System.out.write((o / 256) & 0xFF);
					System.out.write(o & 0xFF);
				}
			} else {
				System.out.write(0);
				System.out.write(0);
				System.out.write(0);
				System.out.write(0);
				System.out.write(0);
				System.out.write(0);
			}
		}
		System.out.flush();
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
