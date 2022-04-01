import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;

public class WeightedSum
{
	public static void main(String[] args) throws Exception {
		System.err.println("Loading " + args[0] + " with weight " + args[1]);
		Image in = new Image(args[0]);
		int xsize = in.xsize;
		int ysize = in.ysize;
		float[] rbuffer = new float[in.rbuffer.length];
		float[] gbuffer = new float[in.rbuffer.length];
		float[] bbuffer = new float[in.rbuffer.length];
		float[] cbuffer = new float[in.rbuffer.length];
		float weight = Float.parseFloat(args[1]);
		addData(rbuffer, gbuffer, bbuffer, cbuffer, xsize, ysize, in, weight);
		for (int i = 2; i < args.length; i += 2) {
			System.err.println("Loading " + args[i] + " with weight " + args[i + 1]);
			in = new Image(args[i]);
			weight = Float.parseFloat(args[i + 1]);
			addData(rbuffer, gbuffer, bbuffer, cbuffer, xsize, ysize, in, weight);
		}
		System.out.println("PF " + xsize + " " + ysize);
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
		for (int y = 0; y < ysize; y++) {
			for (int x = 0; x < xsize; x++) {
				int index = x + y * xsize;
				dout.writeFloat(rbuffer[index] / cbuffer[index]);
				dout.writeFloat(gbuffer[index] / cbuffer[index]);
				dout.writeFloat(bbuffer[index] / cbuffer[index]);
			}
		}
		dout.flush();
	}

	public static void addData(float[] rbuffer, float[] gbuffer, float[] bbuffer, float[] cbuffer, int xsize, int ysize, Image in, float weight) {
		for (int y = 0; y < in.ysize; y++) {
			for (int x = 0; x < in.xsize; x++) {
				int inIndex = x + y * in.xsize;
				int outIndex = x + y * xsize;
				if ((!Float.isInfinite(in.rbuffer[inIndex])) && (!Float.isNaN(in.rbuffer[inIndex])) && (in.rbuffer[inIndex] != 0.0F) && (!Float.isInfinite(in.gbuffer[inIndex])) && (!Float.isNaN(in.gbuffer[inIndex])) && (in.gbuffer[inIndex] != 0.0F) && (!Float.isInfinite(in.bbuffer[inIndex])) && (!Float.isNaN(in.bbuffer[inIndex])) && (in.bbuffer[inIndex] != 0.0F)) {
					rbuffer[outIndex] += in.rbuffer[inIndex] * weight;
					gbuffer[outIndex] += in.gbuffer[inIndex] * weight;
					bbuffer[outIndex] += in.bbuffer[inIndex] * weight;
					cbuffer[outIndex] += weight;
				}
			}
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
				rbuffer[len] = r;
				gbuffer[len] = g;
				bbuffer[len] = ba;
				len++;
			}
		}
	}
}
