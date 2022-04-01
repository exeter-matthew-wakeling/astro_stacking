import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeSet;

/**
 * Creates control points by matching the positions of stars in multiple images, so that the images can be aligned.
 * 
 * Algorithm:
 * For the first image, create a list of 40 prominent stars. Create an index from that list.
 * For each star, pick the nearest 4 stars, and list each combination of 2 of them (there are six combinations).
 * For each combination, store the angle (less than 180deg anticlockwise) from the first to the second,
 * and the distance to the second as a fraction of the distance to the first.
 * For subsequent images, create a list of prominent stars. Perform a lookup against the index.
 * For each star, pick the nearest 4 stars, and list each combination of 2 of them.
 * For each combination, calculate the angle and distance ratio, then find the best match in the index.
 * Score each best match as 1 / ((angle discrepancy radians)^2 + (ratio discrepancy)^2 + 0.00001).
 * Sum the scores for each match. Pick the match with the highest sum score.
 * If there is no good match, then add it to the index, as it may be a star that was not present in the original image.
 */
public class ProduceMatches2
{
	public static final int[][] COMBINATIONS = new int[][] {new int[] {0, 1}, new int[] {0, 2}, new int[] {0, 3}, new int[] {1, 2}, new int[] {1, 3}, new int[] {2, 3}};

	public static void main(String[] args) throws Exception {
		int imageNo = 0;
		int pNum = 20;
		int indexedPointCount = 0;
		int brightness = 100;
		ArrayList<PointIndex> pointIndexes = new ArrayList<PointIndex>();
		for (int argNo = 0; argNo < args.length; argNo++) {
			if ("-n".equals(args[argNo])) {
				argNo++;
				pNum = Integer.parseInt(args[argNo]);
			} else if ("-bright".equals(args[argNo])) {
				argNo++;
				brightness = Integer.parseInt(args[argNo]);
			} else {
				ChooseStartPoints.Image image = new ChooseStartPoints.Image(args[argNo]);
				int xsize = image.getXSize();
				int ysize = image.getYSize();
				int maxVal = image.getMaxVal();
				TreeSet<ChooseStartPoints.Point> points = new TreeSet<ChooseStartPoints.Point>();
				for (int y = 50; y < ysize - 50; y++) {
					for (int x = 50; x < xsize - 50; x++) {
						int c = image.getC(x, y);
						if (c > brightness) {
							ChooseStartPoints.Point p = new ChooseStartPoints.Point(x, y, c);
							points.add(p);
						}
					}
				}
				int margin = xsize / 10;
				int smargin = margin * margin;
				ArrayList<ChooseStartPoints.Point> chosenPoints = new ArrayList<ChooseStartPoints.Point>();
				while ((!points.isEmpty()) && (chosenPoints.size() < pNum)) {
					ChooseStartPoints.Point p = points.first();
					chosenPoints.add(p);
					Iterator<ChooseStartPoints.Point> iter = points.iterator();
					while (iter.hasNext()) {
						ChooseStartPoints.Point n = iter.next();
						int xdist = n.getX() - p.getX();
						int ydist = n.getY() - p.getY();
						if (xdist * xdist + ydist * ydist < smargin) {
							iter.remove();
						}
					}
				}

				System.err.print("Image " + args[argNo] + " found " + chosenPoints.size() + " points, matching against " + indexedPointCount + " points... ");
				System.err.flush();
				int pointCount = 0;

				for (ChooseStartPoints.Point point : chosenPoints) {
					ChooseStartPoints.Point[] nearPoints = new ChooseStartPoints.Point[4];
					int[] nearPointDists = new int[4];
					// Find nearest 4 points to the current point.
					for (ChooseStartPoints.Point nearPoint : chosenPoints) {
						int dist = (point.getX() - nearPoint.getX()) * (point.getX() - nearPoint.getX()) + (point.getY() - nearPoint.getY()) * (point.getY() - nearPoint.getY());
						if (dist == 0) {
						} else if ((nearPoints[0] == null) || (dist < nearPointDists[0])) {
							nearPoints[3] = nearPoints[2];
							nearPoints[2] = nearPoints[1];
							nearPoints[1] = nearPoints[0];
							nearPoints[0] = nearPoint;
							nearPointDists[3] = nearPointDists[2];
							nearPointDists[2] = nearPointDists[1];
							nearPointDists[1] = nearPointDists[0];
							nearPointDists[0] = dist;
						} else if ((nearPoints[1] == null) || (dist < nearPointDists[1])) {
							nearPoints[3] = nearPoints[2];
							nearPoints[2] = nearPoints[1];
							nearPoints[1] = nearPoint;
							nearPointDists[3] = nearPointDists[2];
							nearPointDists[2] = nearPointDists[1];
							nearPointDists[1] = dist;
						} else if ((nearPoints[2] == null) || (dist < nearPointDists[2])) {
							nearPoints[3] = nearPoints[2];
							nearPoints[2] = nearPoint;
							nearPointDists[3] = nearPointDists[2];
							nearPointDists[2] = dist;
						} else if ((nearPoints[3] == null) || (dist < nearPointDists[3])) {
							nearPoints[3] = nearPoint;
							nearPointDists[3] = dist;
						}
					}
					//System.err.println("Nearest points to " + point.getX() + " " + point.getY() + " are:");
					//for (int i = 0; i < 4; i++) {
					//	System.err.println(nearPoints[i].getX() + "\t" + nearPoints[i].getY() + "\t" + nearPointDists[i]);
					//}

					PointIndex bestIndex = null;
					double bestScore = -Double.MAX_VALUE;

					for (int comb = 0; comb < COMBINATIONS.length; comb++) {
						int point1No = COMBINATIONS[comb][0];
						int point2No = COMBINATIONS[comb][1];
						double angle1 = Math.atan2(nearPoints[point1No].getY() - point.getY(), nearPoints[point1No].getX() - point.getX());
						double angle2 = Math.atan2(nearPoints[point2No].getY() - point.getY(), nearPoints[point2No].getX() - point.getX());
						double angle = angle2 - angle1;
						double distRatio = (1.0 * nearPointDists[point2No]) / nearPointDists[point1No];
						if ((angle < 0.0) || (angle > Math.PI)) {
							angle += Math.PI * 2.0;
						}
						if ((angle < 0.0) || (angle > Math.PI)) {
							angle = angle1 - angle2;
							distRatio = (1.0 * nearPointDists[point1No]) / nearPointDists[point2No];
						}
						if ((angle < 0.0) || (angle > Math.PI)) {
							angle += Math.PI * 2.0;
						}
						//System.err.println("Combination " + comb + " angle " + angle + " distRatio " + distRatio);
						for (PointIndex index : pointIndexes) {
							if (index.imageNo < imageNo) {
								double angleDiff = index.angle - angle;
								double distDiff = index.distRatio - distRatio;
								double score = -(angleDiff * angleDiff + distDiff * distDiff);
								//System.err.println("PointIndex angleDiff " + angleDiff + " distDiff " + distDiff + " score " + score);
								if (score > bestScore) {
									bestScore = score;
									bestIndex = index;
								}
							}
						}
					}
					if (bestScore < -0.00001) {
						for (int comb = 0; comb < COMBINATIONS.length; comb++) {
							int point1No = COMBINATIONS[comb][0];
							int point2No = COMBINATIONS[comb][1];
							double angle1 = Math.atan2(nearPoints[point1No].getY() - point.getY(), nearPoints[point1No].getX() - point.getX());
							double angle2 = Math.atan2(nearPoints[point2No].getY() - point.getY(), nearPoints[point2No].getX() - point.getX());
							double angle = angle2 - angle1;
							double distRatio = (1.0 * nearPointDists[point2No]) / nearPointDists[point1No];
							if ((angle < 0.0) || (angle > Math.PI)) {
								angle += Math.PI * 2.0;
							}
							if ((angle < 0.0) || (angle > Math.PI)) {
								angle = angle1 - angle2;
								distRatio = (1.0 * nearPointDists[point1No]) / nearPointDists[point2No];
							}
							if ((angle < 0.0) || (angle > Math.PI)) {
								angle += Math.PI * 2.0;
							}
							pointIndexes.add(new PointIndex(imageNo, point, angle, distRatio));
						}
						indexedPointCount++;
						//if (bestIndex == null) {
							//System.err.println(point.getX() + " " + point.getY() + " no match, score " + bestScore);
						//} else {
							//System.err.println(point.getX() + " " + point.getY() + " no match image " + bestIndex.imageNo + " " + bestIndex.point.getX() + " " + bestIndex.point.getY() + " score " + bestScore);
						//}
					} else {
						//System.err.println(point.getX() + " " + point.getY() + " best match image " + bestIndex.imageNo + " " + bestIndex.point.getX() + " " + bestIndex.point.getY() + " score " + bestScore);
						System.out.println("c n" + bestIndex.imageNo + " N" + imageNo + " x" + bestIndex.point.getX() + " y" + bestIndex.point.getY() + " X" + point.getX() + " Y" + point.getY() + " t0");
						pointCount++;
					}
				}
				System.err.println("Matched " + pointCount);

				imageNo++;
			}
		}
	}

	public static class PointIndex
	{
		private int imageNo;
		private ChooseStartPoints.Point point;
		private double angle, distRatio;

		public PointIndex(int imageNo, ChooseStartPoints.Point point, double angle, double distRatio) {
			this.imageNo = imageNo;
			this.point = point;
			this.angle = angle;
			this.distRatio = distRatio;
		}
	}
}
