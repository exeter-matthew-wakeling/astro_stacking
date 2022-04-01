import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class PercentilesFloat
{
	public static void main(String args[]) throws Exception {
		Image in = new Image(args[0]);
		Arrays.sort(in.rbuffer, 0, in.len);
		Arrays.sort(in.gbuffer, 0, in.len);
		Arrays.sort(in.bbuffer, 0, in.len);
		for (int i = 0; i <= 100; i += 10) {
			int o = (int) ((((long) i) * (in.len - 1)) / 100);
			System.out.println(i + "th percentile: r = " + in.rbuffer[o] + ", g = " + in.gbuffer[o] + ", b = " + in.bbuffer[o]);
		}
	}

	public static class Image
	{
		public int xsize, ysize, len;
		public float rbuffer[], gbuffer[], bbuffer[];

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
			rbuffer = new float[xsize * ysize];
			gbuffer = new float[xsize * ysize];
			bbuffer = new float[xsize * ysize];
			len = 0;
			for (int i = 0; i < xsize * ysize; i++) {
				float r, g, ba;
				r = is.readFloat();
				g = is.readFloat();
				ba = is.readFloat();
				if ((r < 255) || (g > 0) || (ba > 0)) {
					rbuffer[len] = r;
					gbuffer[len] = g;
					bbuffer[len] = ba;
					len++;
				}
			}
		}
	}
}
