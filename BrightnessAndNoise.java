public class BrightnessAndNoise
{
	public static void main(String[] args) throws Exception {
		WeightedSum.Image i = new WeightedSum.Image(args[0]);
		int darkX1 = Integer.parseInt(args[1]);
		int darkY1 = Integer.parseInt(args[2]);
		int darkX2 = Integer.parseInt(args[3]);
		int darkY2 = Integer.parseInt(args[4]);
		int lightX1 = Integer.parseInt(args[5]);
		int lightY1 = Integer.parseInt(args[6]);
		int lightX2 = Integer.parseInt(args[7]);
		int lightY2 = Integer.parseInt(args[8]);
		double rsum, rssum, gsum, gssum, bsum, bssum;
		rsum = rssum = gsum = gssum = bsum = bssum = 0.0;
		int count = 0;
		for (int y = darkY1; y <= darkY2; y++) {
			for (int x = darkX1; x < darkX2; x++) {
				int index = x + y * i.xsize;
				rsum += i.rbuffer[index];
				rssum += i.rbuffer[index] * i.rbuffer[index];
				gsum += i.gbuffer[index];
				gssum += i.gbuffer[index] * i.gbuffer[index];
				bsum += i.bbuffer[index];
				bssum += i.bbuffer[index] * i.bbuffer[index];
				count++;
			}
		}
		rssum = Math.sqrt((rssum - rsum * rsum / count) / count);
		gssum = Math.sqrt((gssum - gsum * gsum / count) / count);
		bssum = Math.sqrt((bssum - bsum * bsum / count) / count);
		rsum = rsum / count;
		gsum = gsum / count;
		bsum = bsum / count;
		count = 0;
		double lightRsum, lightGsum, lightBsum;
		lightRsum = lightGsum = lightBsum = 0.0;
		for (int y = lightY1; y <= lightY2; y++) {
			for (int x = lightX1; x <= lightX2; x++) {
				int index = x + y * i.xsize;
				lightRsum += i.rbuffer[index];
				lightGsum += i.gbuffer[index];
				lightBsum += i.bbuffer[index];
				count++;
			}
		}
		lightRsum = lightRsum / count - rsum;
		lightGsum = lightGsum / count - gsum;
		lightBsum = lightBsum / count - bsum;

		// We can now calculate the multiplier that should be used when combining multiple images, for the lowest noise result.
		// We know:
		// 1. The value of the light pollution brightness
		// 2. The noise level at the light pollution brightness
		// 3. The value of the brightness of a bright consistent area.
		// Modelling noise on the Poisson distribution, the noise level is square root of the expected number of events, so the expected number of events is sqrt(pollution/noise).
		// We want the expected number of events for the bright area, which is bright * sqrt(pollution/noise) / pollution = bright / sqrt(noise*pollution).
		//System.out.println(args[0] + "\t" + rsum + "\t" + rssum + "\t" + lightRsum + "\t" + (lightRsum / Math.sqrt(rsum * rssum)) + "\t" + gsum + "\t" + gssum + "\t" + lightGsum + "\t" + (lightGsum / Math.sqrt(gsum * gssum)) + "\t" + bsum + "\t" + bssum + "\t" + lightBsum + "\t" + (lightBsum / Math.sqrt(bsum * bssum)));
		//System.out.printf("%s\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\n", args[0], rsum, rssum, lightRsum, lightRsum / Math.sqrt(rsum * rssum), gsum, gssum, lightGsum, lightGsum / Math.sqrt(gsum * gssum), bsum, bssum, lightBsum, lightBsum / Math.sqrt(bsum * bssum));
		// The recommended multiplier is light / noise^2
		System.out.printf("%s\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\t%1.10f\n", args[0], rsum, rssum, lightRsum, lightRsum / rssum / rssum, gsum, gssum, lightGsum, lightGsum / gssum / gssum, bsum, bssum, lightBsum, lightBsum / bssum / bssum);
	}
}
