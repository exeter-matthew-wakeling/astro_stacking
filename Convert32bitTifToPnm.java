import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.util.List;

import mil.nga.tiff.TiffReader;
import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.Rasters;

/**
 * Converts a 32-bit float RGB TIFF image into a 32-bit float RGB PNM image.
 *
 * @author Matthew Wakeling
 */
public class Convert32bitTifToPnm
{
	public static void main(String[] args) throws Exception {
		// For some reason, ImageIO doesn't like 32-bit float TIFF images.
		TIFFImage tiffImage = TiffReader.readTiff(new File(args[0]));
		List<FileDirectory> directories = tiffImage.getFileDirectories();
		System.err.println("Found " + directories.size() + " directories");
		FileDirectory directory = directories.get(0);
		Rasters rasters = directory.readRasters();
		int xsize = rasters.getWidth();
		int ysize = rasters.getHeight();
		System.out.println("PF " + xsize + " " + ysize);
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
		int sampleCount = rasters.getSamplesPerPixel();
		for (int y = 0; y < ysize; y++) {
			for (int x = 0; x < xsize; x++) {
				Number[] v = rasters.getPixel(x, y);
				float r = (Float) v[0];
				float g = (Float) v[sampleCount == 1 ? 0 : 1];
				float b = (Float) v[sampleCount == 1 ? 0 : 2];
				dout.writeFloat(r);
				dout.writeFloat(g);
				dout.writeFloat(b);
			}
		}
		dout.flush();
	}
}
