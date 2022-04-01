import java.io.BufferedOutputStream;
import java.io.DataOutputStream;

public class MaskPfm
{
	public static void main(String[] args) throws Exception {
		WeightedSum.Image in = new WeightedSum.Image(args[0]);
		ChooseStartPoints.Image mask = new ChooseStartPoints.Image(args[1]);
		float set = 0.0F;
		if (args.length > 2) {
			set = Float.parseFloat(args[2]);
		}
		int xsize = in.xsize;
		int ysize = in.ysize;
		System.out.println("PF " + xsize + " " + ysize);
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
		for (int y = 0; y < ysize; y++) {
			for (int x = 0; x < xsize; x++) {
				if (mask.getC(x, y) > 384) {
					dout.writeFloat(set == 0.0F ? 0.0F : (float) Math.min(set, in.rbuffer[x + y * xsize]));
					dout.writeFloat(set == 0.0F ? 0.0F : (float) Math.min(set, in.gbuffer[x + y * xsize]));
					dout.writeFloat(set == 0.0F ? 0.0F : (float) Math.min(set, in.bbuffer[x + y * xsize]));
				} else {
					dout.writeFloat(in.rbuffer[x + y * xsize]);
					dout.writeFloat(in.gbuffer[x + y * xsize]);
					dout.writeFloat(in.bbuffer[x + y * xsize]);
				}
			}
		}
		dout.flush();
	}
}
