import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import mil.nga.tiff.TIFFImage;
import mil.nga.tiff.TiffWriter;
import mil.nga.tiff.FieldType;
import mil.nga.tiff.FileDirectory;
import mil.nga.tiff.Rasters;
import mil.nga.tiff.util.TiffConstants;

public class Convert32bitPnmToTif
{
	public static void main(String[] args) throws Exception {
		Image in = new Image(args[0]);
		Rasters rasters = new Rasters(in.xsize, in.ysize, 3, FieldType.FLOAT);
		int rowsPerStrip = rasters.calculateRowsPerStrip(TiffConstants.PLANAR_CONFIGURATION_CHUNKY);
		FileDirectory directory = new FileDirectory();
		directory.setImageWidth(in.xsize);
		directory.setImageHeight(in.ysize);
		directory.setBitsPerSample(FieldType.FLOAT.getBits());
		directory.setCompression(TiffConstants.COMPRESSION_NO);
		directory.setPhotometricInterpretation(TiffConstants.PHOTOMETRIC_INTERPRETATION_BLACK_IS_ZERO);
		directory.setSamplesPerPixel(3);
		directory.setRowsPerStrip(rowsPerStrip);
		directory.setPlanarConfiguration(TiffConstants.PLANAR_CONFIGURATION_CHUNKY);
		directory.setSampleFormat(TiffConstants.SAMPLE_FORMAT_FLOAT);
		directory.setWriteRasters(rasters);

		for (int y = 0; y < in.ysize; y++) {
			for (int x = 0; x < in.xsize; x++) {
				rasters.setPixelSample(0, x, y, in.rbuffer[x + y * in.xsize]);
				rasters.setPixelSample(1, x, y, in.gbuffer[x + y * in.xsize]);
				rasters.setPixelSample(2, x, y, in.bbuffer[x + y * in.xsize]);
			}
		}

		TIFFImage tiffImage = new TIFFImage();
		tiffImage.add(directory);
		TiffWriter.writeTiff(new File(args[1]), tiffImage);
	}

	public static class Image
	{
		private int xsize, ysize, len;
		private float rbuffer[], gbuffer[], bbuffer[];

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
