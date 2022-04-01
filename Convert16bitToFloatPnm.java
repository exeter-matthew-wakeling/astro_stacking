import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;

/**
 * Convert from a normal RGB colour PNM file to a 32-bit float PNM file.
 *
 * @author Matthew Wakeling
 */
public class Convert16bitToFloatPnm
{
	public static void main(String[] args) throws Exception {
		int xsize, ysize, maxVal, len;
		BufferedInputStream is = args.length > 0 ? new BufferedInputStream(new FileInputStream(args[0])) : new BufferedInputStream(System.in);
		int b = is.read();
		if (b != 'P') {
			throw new NumberFormatException("Invalid PPM file");
		}
		b = is.read();
		if (b != '6') {
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
			maxVal = (maxVal * 10) + b - '0';
			b = is.read();
		}
		if ((b != ' ') && (b != '\t') && (b != '\n')) {
			throw new NumberFormatException("Invalid PPM file");
		}
		System.out.println("PF " + xsize + " " + ysize);
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
		for (int i = 0; i < xsize * ysize * 3; i++) {
			float c = 0.0F;
			if (maxVal > 65535) {
				c = is.read() * 16777216.0F + is.read() * 65536.0F + is.read() * 256.0F + is.read() * 1.0F;
			} else if (maxVal > 255) {
				c = is.read() * 256.0F + is.read() * 1.0F;
			} else {
				c = is.read() * 1.0F;
			}
			dout.writeFloat(c);
		}
		dout.flush();
	}
}
