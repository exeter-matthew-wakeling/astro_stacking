import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import nom.tam.fits.*;

/**
 * Convert a FITS 32-bit float RGB colour image into a 32-bit float PNM file.
 *
 * @author Matthew Wakeling
 */
public class ConvertFitsToFloatPnm
{
	public static void main(String[] args) throws Exception {
		Fits f = new Fits(args[0]);
		ImageHDU hdu = (ImageHDU) f.getHDU(0);
//		System.out.println(hdu);
		ImageData imageData = (ImageData) hdu.getData();
		Object imageObj = imageData.getData();
//		System.out.println(imageObj.getClass());
		if (imageObj instanceof float[][][]) {
			float[][][] image = (float[][][]) imageObj;
			System.err.println(image.length + "\t" + image[0].length + "\t" + image[0][0].length);
			int xsize = image[0][0].length;
			int ysize = image[0].length;
			System.out.println("PF " + xsize + " " + ysize);
			DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
			for (int y = 0; y < ysize; y++) {
				for (int x = 0; x < xsize; x++) {
					dout.writeFloat(image[0][y][x]);
					dout.writeFloat(image[1][y][x]);
					dout.writeFloat(image[2][y][x]);
				}
			}
			dout.flush();
		} else if (imageObj instanceof float[][]) {
			float[][] image = (float[][]) imageObj;
			System.err.println(image.length + "\t" + image[0].length);
			int xsize = image[0].length;
			int ysize = image.length;
			System.out.println("PF " + xsize + " " + ysize);
			DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
			for (int y = 0; y < ysize; y++) {
				for (int x = 0; x < xsize; x++) {
					dout.writeFloat(image[y][x]);
					dout.writeFloat(image[y][x]);
					dout.writeFloat(image[y][x]);
				}
			}
			dout.flush();
		}
	}
}
