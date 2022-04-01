public class DoTransform
{
	public static final int GRID = 20;

	public static void main(String args[]) {
		DoTransform trans = new DoTransform(3000, 2000, 40, 300, 200, 50, 10, 10, 10, 0.01, 0.01, 0.01, 10, 10);
		Point p = trans.outToIn(100, 100);
		System.err.println(p.getX() + "\t" + p.getY());
		Point p2 = trans.inToOut(p.getX(), p.getY());
		System.err.println(p2.getX() + "\t" + p2.getY());

		trans = new DoTransform(301, 201, 53.10667, 3000, 3000, 60, 0, 10, 0, 0, 0, 0, 0, 0);
		System.out.println("P5 3000 3000 255");
		for (int y = 0; y < 3000; y++) {
			for (int x = 0; x < 3000; x++) {
				p = trans.outToIn(x, y);
				if ((p.getX() < 0) || (p.getX() > 301) || (p.getY() < 0) || (p.getY() > 201)) {
					System.out.write(128);
				} else if (((int) p.getX() % 10 == 0) || ((int) p.getY() % 10 == 0)) {
					System.out.write(0);
				} else {
					System.out.write(255);
				}
			}
		}
		System.out.flush();
	}

	private double iXsize, iYsize, iWidth, oXsize, oYsize, oWidth;
	private double yaw, pitch, roll, a, b, c, d, e;
	private double[][] outToInGridX, outToInGridY;

	public DoTransform(double iXsize, double iYsize, double iWidth, double oXsize, double oYsize, double oWidth, double yaw, double pitch, double roll, double a, double b, double c, double d, double e) {
		this.iXsize = iXsize;
		this.iYsize = iYsize;
		this.iWidth = iWidth;
		this.oXsize = oXsize;
		this.oYsize = oYsize;
		this.oWidth = oWidth;
		this.yaw = yaw;
		this.pitch = pitch;
		this.roll = roll;
		this.a = a;
		this.b = b;
		this.c = c;
		this.d = d;
		this.e = e;

		int gridYSize = (int) ((oYsize + 30) / GRID);
		int gridXSize = (int) ((oXsize + 30) / GRID);
		outToInGridX = new double[gridYSize][];
		outToInGridY = new double[gridYSize][];
		for (int y = 0; y < gridYSize; y++) {
			outToInGridX[y] = new double[gridXSize];
			outToInGridY[y] = new double[gridXSize];
			int oY = y * GRID - GRID;
			for (int x = 0; x < gridXSize; x++) {
				int oX = x * GRID - GRID;
				Point p = origOutToIn(oX, oY);
				outToInGridX[y][x] = p.getX();
				outToInGridY[y][x] = p.getY();
			}
		}
	}

	public Point inToOut(double x, double y) {
		x = x - d;
		y = y - e;
		double centrex = iXsize / 2.0;
		double centrey = iYsize / 2.0;
		double r = Math.sqrt((x - centrex) * (x - centrex) + (y -centrey) * (y - centrey)) / Math.min(centrex, centrey);
		// Perform reverse transformation to r = (1.0 - a - b - c) * newR + c * newR * newR + b * newR * newR * newR + a * newR * newR * newR * newR - use Newton-Raphson.
		double newR = r;
		for (int i = 0; i < 5; i++) {
			newR -= ((1.0 - a - b - c) * newR + c * newR * newR + b * newR * newR * newR + a * newR * newR * newR * newR - r) / (1.0 - a - b - c + 2.0 * c * newR + 3.0 * b * newR * newR + 4.0 * a * newR * newR * newR);
		}
		x = (x - centrex) * newR / r;
		y = (y - centrey) * newR / r;
		// Do roll
		double angle = Math.PI * roll / 180.0;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double x2 = cos * x - sin * y;
		double y2 = sin * x + cos * y;
		// Do pitch. First convert to 3d vector
		double z2 = iXsize / Math.tan(Math.PI * iWidth / 360.0) / 2.0;
		// Rotate around x axis
		angle = Math.PI * pitch / 180.0;
		cos = Math.cos(angle);
		sin = Math.sin(angle);
		x = x2;
		y = cos * y2 - sin * z2;
		double z = cos * z2 + sin * y2;
		// Now do yaw - rotate around y axis
		angle = Math.PI * yaw / 180.0;
		cos = Math.cos(angle);
		sin = Math.sin(angle);
		x2 = cos * x + sin * z;
		y2 = y;
		z2 = cos * z - sin * x;
		// Now convert to xy coords.
		x = (x2 * oXsize / Math.tan(Math.PI * oWidth / 360.0) / z2 + oXsize) / 2.0;
		y = (y2 * oXsize / Math.tan(Math.PI * oWidth / 360.0) / z2 + oYsize) / 2.0;
		return new Point(x, y);
	}

	public Point outToIn(double x, double y) {
		int gx = (int) ((x + GRID) / GRID);
		int gy = (int) ((y + GRID) / GRID);
		if ((gx >= 0) && (gx < outToInGridX[0].length - 1) && (gy >= 0) && (gy < outToInGridX.length - 1)) {
			double fracx = ((x + GRID) / GRID) - gx;
			double fracy = ((y + GRID) / GRID) - gy;
			double x1 = outToInGridX[gy][gx] + fracx * (outToInGridX[gy][gx + 1] - outToInGridX[gy][gx]);
			double x2 = outToInGridX[gy + 1][gx] + fracx * (outToInGridX[gy + 1][gx + 1] - outToInGridX[gy + 1][gx]);
			double rx = x1 + fracy * (x2 - x1);
			double y1 = outToInGridY[gy][gx] + fracx * (outToInGridY[gy][gx + 1] - outToInGridY[gy][gx]);
			double y2 = outToInGridY[gy + 1][gx] + fracx * (outToInGridY[gy + 1][gx + 1] - outToInGridY[gy + 1][gx]);
			double ry = y1 + fracy * (y2 - y1);
			return new Point(rx, ry);
		}
		return origOutToIn(x, y);
	}

	public Point origOutToIn(double x, double y) {
		double z = oXsize / Math.tan(Math.PI * oWidth / 360.0) / 2.0;
		x -= oXsize / 2.0;
		y -= oYsize / 2.0;
		// Perform yaw in reverse
		double angle = Math.PI * yaw / 180.0;
		double cos = Math.cos(angle);
		double sin = Math.sin(angle);
		double x2 = cos * x - sin * z;
		double y2 = y;
		double z2 = cos * z + sin * x;
		// Now perform pitch - rotate around x axis
		angle = Math.PI * pitch / 180.0;
		cos = Math.cos(angle);
		sin = Math.sin(angle);
		x = x2;
		y = cos * y2 + sin * z2;
		z = cos * z2 - sin * y2;
		// Convert to xy coordinates
		x = x * iXsize / Math.tan(Math.PI * iWidth / 360.0) / z / 2.0;
		y = y * iXsize / Math.tan(Math.PI * iWidth / 360.0) / z / 2.0;
		// Do roll
		angle = Math.PI * roll / 180.0;
		cos = Math.cos(angle);
		sin = Math.sin(angle);
		x2 = cos * x + sin * y;
		y2 = cos * y - sin * x;
		// Now do distortion
		double centrex = iXsize / 2.0;
		double centrey = iYsize / 2.0;
		double r = Math.sqrt(x*x + y*y) / Math.min(centrex, centrey);
		double newR = (1.0 - a - b - c) * r + c * r * r + b * r * r * r + a * r * r * r * r;
		if (1.0 - a - b - c + 2.0 * c * r + 3.0 * b * r * r + 4.0 * a * r * r * r < 0.0) {
			// Radius wraparound.
			newR = 100.0;
		}
		return new Point(x2 * newR / r + centrex + d, y2 * newR / r + centrey + e);
	}

	public double getA() {
		return a;
	}

	public double getB() {
		return b;
	}

	public double getC() {
		return c;
	}

	public double getD() {
		return d;
	}

	public double getE() {
		return e;
	}

	public double getPitch() {
		return pitch;
	}

	public double getRoll() {
		return roll;
	}

	public double getYaw() {
		return yaw;
	}

	public double getIWidth() {
		return iWidth;
	}

	public double getIXsize() {
		return iXsize;
	}

	public double getIYsize() {
		return iYsize;
	}

	public String toString() {
		return "(iXsize: " + iXsize + ", iYsize: " + iYsize + ", iWidth: " + iWidth + ", oXsize: " + oXsize + ", oYsize: " + oYsize + ", oWidth: " + oWidth + ", yaw: " + yaw + ", pitch: " + pitch + ", roll: " + roll + ", a: " + a + ", b: " + b + ", c: " + c + ", d: " + d + ", e: " + e + ")";
	}

	public static class Point
	{
		private double x, y;

		public Point(double x, double y) {
			this.x = x;
			this.y = y;
		}

		public double getX() {
			return x;
		}

		public double getY() {
			return y;
		}
	}
}
