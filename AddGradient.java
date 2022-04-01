import java.io.BufferedOutputStream;
import java.io.DataOutputStream;

public class AddGradient
{
	public static void main(String[] args) throws Exception {
		WeightedSum.Image in = new WeightedSum.Image(args[0]);
		float rbase = Float.parseFloat(args[1]);
		float rx = Float.parseFloat(args[2]);
		float ry = Float.parseFloat(args[3]);
		float gbase = Float.parseFloat(args[4]);
		float gx = Float.parseFloat(args[5]);
		float gy = Float.parseFloat(args[6]);
		float bbase = Float.parseFloat(args[7]);
		float bx = Float.parseFloat(args[8]);
		float by = Float.parseFloat(args[9]);
		int xsize = in.xsize;
		int ysize = in.ysize;
		System.out.println("PF " + xsize + " " + ysize);
		DataOutputStream dout = new DataOutputStream(new BufferedOutputStream(System.out));
		for (int y = 0; y < ysize; y++) {
			for (int x = 0; x < xsize; x++) {
				int index = x + y * xsize;
				if ((!Float.isInfinite(in.rbuffer[index])) && (!Float.isNaN(in.rbuffer[index])) && (in.rbuffer[index] != 0.0F) && (!Float.isInfinite(in.gbuffer[index])) && (!Float.isNaN(in.gbuffer[index])) && (in.gbuffer[index] != 0.0F) && (!Float.isInfinite(in.bbuffer[index])) && (!Float.isNaN(in.bbuffer[index])) && (in.bbuffer[index] != 0.0F)) {
					dout.writeFloat(in.rbuffer[index] + rbase + rx * x + ry * y);
					dout.writeFloat(in.gbuffer[index] + gbase + gx * x + gy * y);
					dout.writeFloat(in.bbuffer[index] + bbase + bx * x + by * y);
				} else {
					dout.writeFloat(in.rbuffer[index]);
					dout.writeFloat(in.gbuffer[index]);
					dout.writeFloat(in.bbuffer[index]);
				}
			}
		}
		dout.flush();
	}
}	
