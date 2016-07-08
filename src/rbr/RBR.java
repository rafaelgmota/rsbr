package rbr;

import util.*;

import java.util.*;

public class RBR {
	private Graph graph;

	private HashMap<Vertice, ArrayList<RoutingPath>> routingPathForVertice;
	private HashMap<Vertice, ArrayList<Region>> regionsForVertice;

	public RBR(Graph g) {
		graph = g;
		routingPathForVertice = new HashMap<>();
		regionsForVertice = new HashMap<>();
	}

	public HashMap<Vertice, ArrayList<Region>> regions() {
		return this.regionsForVertice;
	}
	// Link weight stats [0] - mean / [1] - standard deviation
	public double[] linkWeightStats() {
		double linksWeight = 0.0;
		double[] stats = new double[2];
		double mean = 0.0;
		double std = 0.0;

		for (Edge link : graph.getEdges())
			linksWeight += (double) link.weight();

		mean = linksWeight / (double) graph.getEdges().size();
		stats[0] = mean;

		double temp = 0.0;
		for (Edge link : graph.getEdges())
			temp += ((double) link.weight() - mean)
					* ((double) link.weight() - mean);

		double variance = (temp / (double) (graph.getEdges().size()));
		// size-1 for sample. We have population

		std = Math.sqrt(variance);
		stats[1] = std;

		return stats;
	}

	// Calculates the regions stats - [0] - Max / [1] - Min / [2] - Average
	public double[] getRegionsStats() {

		double[] stats = new double[3];
		double average;
		List<Integer> regSizes = new ArrayList<>();

		for (Vertice r : graph.getVertices()) {
			regSizes.add(regionsForVertice.get(r).size());
		}
		Collections.sort(regSizes);

		int sum = 0;
		for (int size : regSizes) {
			sum += size;
		}
		average = sum / regSizes.size();

		stats[0] = (double) regSizes.get(regSizes.size() - 1);
		stats[1] = (double) regSizes.get(0);
		stats[2] = (double) average;
		return stats;
	}
	
	// Pack routing options if they have the same input port and the same
	// destination
	private void packOutputPort(Vertice atual) {
		ArrayList<RoutingPath> actRP = routingPathForVertice.get(atual);
		routingPathForVertice.put(atual, new ArrayList<>());
		for (RoutingPath a : actRP) {
			String op = a.getOp();
			String dst = a.getDst();
			String ip = a.getIp();

			for (RoutingPath b : actRP) {
				if (ip.equals(b.getIp()) && dst.equals(b.getDst())) {
					if (!op.contains(b.getOp()))
						op = op.concat(b.getOp());
				}
			}
			addRoutingPath(atual, ip, dst, op);
		}
	}

	// Pack routing options if they have the same output port and the same
	// destination
	public void packInputPort(Vertice atual) {
		ArrayList<RoutingPath> actRP = routingPathForVertice.get(atual);
		routingPathForVertice.put(atual, new ArrayList<>());
		for (RoutingPath a : actRP) {
			String op = a.getOp();
			String dst = a.getDst();
			String ip = a.getIp();

			for (RoutingPath b : actRP) {
				if (op.equals(b.getOp()) && dst.equals(b.getDst())) {
					if (!ip.contains(b.getIp()))
						ip = ip.concat(b.getIp());
				}
			}
			addRoutingPath(atual, ip, dst, op);
		}
	}

	public void addRoutingOptions(ArrayList<ArrayList<Path>> paths) {

		for(Vertice v : graph.getVertices())
			routingPathForVertice.put(v, new ArrayList<>());

		for(ArrayList<Path> alp : paths) {			
			for (Path path : alp) {
				String dest = path.dst().getNome();
				for (Vertice sw : path) {
					if (path.indexOf(sw) != path.size() - 1) {
						String op = sw.edge(path.get(path.indexOf(sw) + 1))
								.color();
						String ip = (path.indexOf(sw) == 0) ? "I" : sw.edge(
								path.get(path.indexOf(sw) - 1)).color();
						addRoutingPath(sw, ip, dest, op);
					}
				}
			}
		}
		for (Vertice atual : graph.getVertices()) {
			packOutputPort(atual);
			// packInputPort(atual);
		}
	}

	// Do output combinations
	private static ArrayList<String> getOutputCombinations() {
		ArrayList<String> oPComb = new ArrayList<String>();
		char[] op = "ENSW".toCharArray();

		for (int m = 1; m != 1 << op.length; m++) {
			String a = "";
			for (int i = 0; i != op.length; i++) {
				if ((m & (1 << i)) != 0) {
					a = a.concat(Character.toString(op[i]));
				}
			}
			oPComb.add(a);
		}
		return oPComb;
	}
	
	// Compute the regions
	public void regionsComputation() {
		ArrayList<String> opComb = getOutputCombinations();
		for (Vertice sw : graph.getVertices()) {
			regionsForVertice.put(sw, new ArrayList<>());
			for (String op : opComb) {
				String ip = new String();
				ArrayList<String> destinations = new ArrayList<String>();
				for (RoutingPath rp : routingPathForVertice.get(sw)) {
					if (rp.getOp().equals(op)) {
						if (!destinations.contains(rp.getDst()))
							destinations.add(rp.getDst());
						ip = mergeString(ip, rp.getIp());
					}
				}
				if (destinations.size() != 0) {
					regionsForVertice.get(sw).add(new Region(ip, destinations, op));
				}
			}

			for (Region reg : regionsForVertice.get(sw)) {
				reg.setextrems();
			}
		}
		adjustsRegions();

	}

	// Adjust the regions to avoid overlap
	private void adjustsRegions() {
		for (Vertice sw : graph.getVertices()) {
			ArrayList<Region> regionsTemp = new ArrayList<>();
			ArrayList<Region> regionsRemov = new ArrayList<>();
			for (Region reg : regionsForVertice.get(sw)) {
				ArrayList<String> strgs = getStranges(reg);

				if (strgs != null) {
					String[] extrems = getExtrems(strgs);

					String[] Min = extrems[0].split("\\.");
					int xmin = Integer.valueOf(Min[0]);
					int ymin = Integer.valueOf(Min[1]);
					String[] Max = extrems[1].split("\\.");
					int xmax = Integer.valueOf(Max[0]);
					int ymax = Integer.valueOf(Max[1]);

					ArrayList<String> dests = reg
							.getDst(xmin, ymin, xmax, ymax);

					if (nSides(reg, strgs) == 3) {
						deleteFromRegion(extrems, reg);
						reg.setextrems();
					} else {
						regionsRemov.add(reg);
						ArrayList<ArrayList<String>> dsts = getDestinations(
								xmin, xmax, ymin, ymax, reg);
						if (dsts != null) {
							for (ArrayList<String> dst : dsts) {
								Region r = new Region(reg.getIp(), dst,
										reg.getOp());
								// r.setextrems();
								regionsTemp.add(r);
							}
						}
					}
					// use others routers to make others regions
					if (dests != null)
						regionsTemp.addAll(makeRegions(dests, reg.getIp(),
								reg.getOp()));
				}
			}
			regionsForVertice.get(sw).removeAll(regionsRemov);
			regionsForVertice.get(sw).addAll(regionsTemp);
		}
	}

	// Get destinations depending on the min and max from region and from
	// excluded box
	private static ArrayList<ArrayList<String>> getDestinations(int xmin, int xmax,
			int ymin, int ymax, Region reg) {
		ArrayList<ArrayList<String>> dsts = new ArrayList<>();
		ArrayList<String> dstTemp1 = new ArrayList<>();
		ArrayList<String> dstTemp2 = new ArrayList<>();
		ArrayList<String> dstTemp3 = new ArrayList<>();
		ArrayList<String> dstTemp4 = new ArrayList<>();
		boolean left = touchLeft(xmin, reg);
		boolean right = touchRight(xmax, reg);
		boolean up = touchUp(ymax, reg);
		boolean down = touchDown(ymin, reg);

		if (left && down && !up && !right) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x > xmax)
					dstTemp1.add(dst);
				else if (y > ymax)
					dstTemp2.add(dst);
			}
		} else if (left && up && !right && !down) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x > xmax)
					dstTemp1.add(dst);
				else if (y < ymin)
					dstTemp2.add(dst);
			}
		} else if (right && up && !left && !down) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x < xmin)
					dstTemp1.add(dst);
				else if (y < ymin)
					dstTemp2.add(dst);
			}
		} else if (right && down && !left && !up) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x < xmin)
					dstTemp1.add(dst);
				else if (y > ymax)
					dstTemp2.add(dst);
			}
		} else if (up && down && !right && !left) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				if (x < xmin)
					dstTemp1.add(dst);
				else if (x > xmax)
					dstTemp2.add(dst);
			}
		} else if (left && right && !up && !down) {
			for (String dst : reg.getDst()) {
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (y < ymin)
					dstTemp1.add(dst);
				else if (y > ymax)
					dstTemp2.add(dst);
			}
		} else if (left && !up && !down && !right) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x > xmax)
					dstTemp1.add(dst);
				else if (y > ymax)
					dstTemp2.add(dst);
				else if (y < ymin)
					dstTemp3.add(dst);
			}
		} else if (right && !left && !down && !up) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x < xmin)
					dstTemp1.add(dst);
				else if (y > ymax)
					dstTemp2.add(dst);
				else if (y < ymin)
					dstTemp3.add(dst);
			}
		} else if (down && !up && !left && !right) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (y > ymax)
					dstTemp1.add(dst);
				else if (x < xmin)
					dstTemp2.add(dst);
				else if (x > xmax)
					dstTemp3.add(dst);
			}
		} else if (up && !down && !left && !right) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (y < ymin)
					dstTemp1.add(dst);
				else if (x < xmin)
					dstTemp2.add(dst);
				else if (x > xmax)
					dstTemp3.add(dst);
			}
		} else if (!up && !down && !left && !right) {
			for (String dst : reg.getDst()) {
				int x = Integer.parseInt(dst.split("\\.")[0]);
				int y = Integer.parseInt(dst.split("\\.")[1]);
				if (x < xmin)
					dstTemp1.add(dst);
				else if (x > xmax)
					dstTemp2.add(dst);
				else if (y > ymax)
					dstTemp3.add(dst);
				else
					dstTemp4.add(dst);
			}
		} else {
			;// System.err.println("Severe Error: total overlap!!");
		}
		if (dstTemp1.size() != 0)
			dsts.add(dstTemp1);
		if (dstTemp2.size() != 0)
			dsts.add(dstTemp2);
		if (dstTemp3.size() != 0)
			dsts.add(dstTemp3);
		if (dstTemp4.size() != 0)
			dsts.add(dstTemp4);
		if (dsts.size() == 0)
			dsts = null;
		return dsts;
	}

	private static boolean touchLeft(int xmin, Region reg) {
		return (xmin == reg.getXmin());
	}

	private static boolean touchRight(int xmax, Region reg) {
		return (xmax == reg.getXmax());
	}

	private static boolean touchUp(int ymax, Region reg) {
		return (ymax == reg.getYmax());
	}

	private static boolean touchDown(int ymin, Region reg) {
		return (ymin == reg.getYmin());
	}

	// [0] - DownLeft [1]- UpRight
	private static String[] getExtrems(ArrayList<String> dsts) {
		String[] xtrems = new String[2];

		int xMin = Integer.MAX_VALUE, yMin = Integer.MAX_VALUE;
		int xMax = 0, yMax = 0;

		for (String s : dsts) {
			String[] xy = s.split("\\.");
			int x = Integer.valueOf(xy[0]);
			int y = Integer.valueOf(xy[1]);

			xMin = (xMin < x) ? xMin : x;
			yMin = (yMin < y) ? yMin : y;
			xMax = (xMax > x) ? xMax : x;
			yMax = (yMax > y) ? yMax : y;
		}

		xtrems[1] = xMax + "." + yMax;
		xtrems[0] = xMin + "." + yMin;

		return xtrems;

	}

	// Return number of common sides of the box formed by strangers and the
	// region
	private static int nSides(Region reg, ArrayList<String> strgs) {
		String[] strgsXtrems = getExtrems(strgs);
		int sides = 0;

		if (Integer.parseInt(strgsXtrems[0].split("\\.")[0]) == reg.getXmin())
			sides++;
		if (Integer.parseInt(strgsXtrems[0].split("\\.")[1]) == reg.getYmin())
			sides++;
		if (Integer.parseInt(strgsXtrems[1].split("\\.")[0]) == reg.getXmax())
			sides++;
		if (Integer.parseInt(strgsXtrems[1].split("\\.")[1]) == reg.getYmax())
			sides++;

		return sides;

	}

	// Delete routers inside of box defined by extremes
	private static void deleteFromRegion(String[] extrems, Region reg) {

		String[] Min = extrems[0].split("\\.");
		int xmin = Integer.valueOf(Min[0]);
		int ymin = Integer.valueOf(Min[1]);
		String[] Max = extrems[1].split("\\.");
		int xmax = Integer.valueOf(Max[0]);
		int ymax = Integer.valueOf(Max[1]);
		for (int i = xmin; i <= xmax; i++) {
			for (int j = ymin; j <= ymax; j++) {
				String dst = i + "." + j;
				reg.getDst().remove(dst);
			}
		}
	}

	// Return wrong destinations
	private static ArrayList<String> getStranges(Region reg) {
		ArrayList<String> strg = new ArrayList<String>();
		int xmin = reg.getXmin(), xmax = reg.getXmax();
		int ymin = reg.getYmin(), ymax = reg.getYmax();
		for (int x = xmin; x <= xmax; x++) {
			for (int y = ymin; y <= ymax; y++) {
				String dest = x + "." + y;
				if (!reg.getDst().contains(dest)) {
					strg.add(dest);
				}
			}
		}
		if (strg.size() == 0)
			strg = null;
		return strg;

	}

	// Make regions only with correct destinations
	private static ArrayList<Region> makeRegions(ArrayList<String> dsts, String ip,
			String op) {
		ArrayList<Region> result = new ArrayList<Region>();
		String[] extrems = getExtrems(dsts);
		String[] Min = extrems[0].split("\\.");
		int Xmin = Integer.valueOf(Min[0]);
		int Ymin = Integer.valueOf(Min[1]);
		String[] Max = extrems[1].split("\\.");
		int Xmax = Integer.valueOf(Max[0]);
		int Ymax = Integer.valueOf(Max[1]);

		while (!dsts.isEmpty()) {
			int Lmin = Ymin, Cmax = Xmax;
			int Cmin = Xmin, Lmax = Ymax;

			boolean first = true;
			for (int line = Lmax; line >= Lmin; line--) {
				for (int col = Cmin; col <= Cmax; col++) {
					if (first) {
						if (dsts.contains(col + "." + line)) {
							Cmin = col;
							Lmax = line;
							first = false;
						}
					} else {
						if (!dsts.contains(col + "." + line)) { // if stranger
							if (line == Lmax) { // first line
								Cmax = col - 1;
							} else if (col > (Cmax - Cmin) / 2 && col > Cmin) {
								Cmax = col - 1;
							} else {
								Lmin = ++line;
							}

							if (line == Lmin) { // last line
								Region rg = montaRegiao(Cmin, Lmin, Cmax, Lmax,
										ip, op);
								dsts.removeAll(rg.getDst());
								result.add(rg);
							}
							break;
						}
					}
					if (line == Lmin && col == Cmax) { // last line
						Region rg = montaRegiao(Cmin, Lmin, Cmax, Lmax, ip, op);
						dsts.removeAll(rg.getDst());
						result.add(rg);
					}
				}
			}
		}
		return result;
	}

	private static Region montaRegiao(int xmin, int ymin, int xmax, int ymax,
			String ip, String op) {
		ArrayList<String> dst = new ArrayList<String>();
		for (int x = xmin; x <= xmax; x++)
			for (int y = ymin; y <= ymax; y++)
				dst.add(x + "." + y);

		return (new Region(ip, dst, op));
	}

	// Check if regions r1 and r2 can be merged
	private static boolean CanBeMerged(Region r1, Region r2) {
		boolean canBeMerged = false;

		if (AreNeighbours(r1, r2) && FormBox(r1, r2) && OpIsSub(r1, r2)) {
			canBeMerged = true;
		}

		return canBeMerged;
	}

	// Calculates reachability
	private double reachability(Vertice orig) {
		double reaches = 0, total = graph.getVertices().size() - 1;
		for (Vertice dest : graph.getVertices()) {
			if (orig != dest) {
				if (reaches(orig, dest)) {
					reaches++;
				}
			}
		}
		return (reaches / total);
	}

	private boolean reaches(Vertice src, Vertice dest) {
		return reaches(src, dest, "I");
	}

	private boolean reaches(Vertice src, Vertice dest, String ipColor) {
		if (dest == src)
			return true;
		String opColor = getOpColor(src, dest, ipColor);
		if (opColor == null)
			return false;
		return reaches(src.getAdj(opColor).destination(), dest, EdgeColor.getInvColor(src.getAdj(opColor).color()));
	}

	private String getOpColor(Vertice src, Vertice dest, String ipColor) {
		String router = dest.getNome();
		for (rbr.Region reg : regionsForVertice.get(src))
			if (reg.contains(router) && reg.getIp().contains(ipColor))
				return (reg.getOp().substring(0, 1));

		System.err.println("ERROR : There isn't Op on " + src.getNome()
				+ " for " + dest.getNome() + " " + ipColor);
		return null;
	}

	public void merge(double reachability) {
		for (Vertice vertice : graph.getVertices())
			merge(vertice, reachability);
	}

	// Merge the regions of a router
	private void merge(Vertice router, double reachability) {
		ArrayList<Region> bkpListRegion = null;
		boolean wasPossible = true;

		while (reachability(router) >= reachability && wasPossible) {
			bkpListRegion = new ArrayList<>(regionsForVertice.get(router));
			wasPossible = mergeUnitary(router);
		}
		if (bkpListRegion != null) {
			regionsForVertice.put(router, bkpListRegion);
		}

	}

	/*
	 * Tries to make one (and only one) merge and returns true in case of
	 * success
	 */
	private boolean mergeUnitary(Vertice router) {
		for (int a = 0; a < regionsForVertice.get(router).size(); a++) {
			Region ra = regionsForVertice.get(router).get(a);
			for (int b = a + 1; b < regionsForVertice.get(router).size(); b++) {
				Region rb = regionsForVertice.get(router).get(b);

				if (CanBeMerged(ra, rb)) {
					String upRight = getUpRightMerged(ra, rb);
					String downLeft = getDownLeftMerged(ra, rb);
					String op = getOpMerged(ra, rb);
					String ip = getIpMerged(ra, rb);

					Region reg = new Region(ip, ra.getDst(), op);
					reg.setUpRight(upRight);
					reg.setDownLeft(downLeft);
					reg.getDst().addAll(rb.getDst());
					reg.setSize();

					regionsForVertice.get(router).add(reg);
					regionsForVertice.get(router).remove(ra);
					regionsForVertice.get(router).remove(rb);

					Collections.sort(regionsForVertice.get(router));

					return true;
				}
			}
		}
		return false;
	}

	// Return UpRight identifier after merge
	private static String getUpRightMerged(Region r1, Region r2) {
		String upRight;

		upRight = Integer.toString(Math.max(
				Integer.parseInt(r1.getUpRight().split("\\.")[0]),
				Integer.parseInt(r2.getUpRight().split("\\.")[0])))
				+ "."
				+ Integer.toString(Math.max(
						Integer.parseInt(r1.getUpRight().split("\\.")[1]),
						Integer.parseInt(r2.getUpRight().split("\\.")[1])));

		return upRight;
	}

	// Return DownLeft identifier after merge
	private static String getDownLeftMerged(Region r1, Region r2) {
		String downLeft;

		downLeft = Integer.toString(Math.min(
				Integer.parseInt(r1.getDownLeft().split("\\.")[0]),
				Integer.parseInt(r2.getDownLeft().split("\\.")[0])))
				+ "."
				+ Integer.toString(Math.min(
						Integer.parseInt(r1.getDownLeft().split("\\.")[1]),
						Integer.parseInt(r2.getDownLeft().split("\\.")[1])));

		return downLeft;
	}

	// return the Output ports after merge
	private static String getOpMerged(Region r1, Region r2) {
		String op;

		if (r1.getOp().contains(r2.getOp())) {
			op = r2.getOp();
		} else {
			op = r1.getOp();
		}

		return op;
	}

	// return the Input ports after merge
	private static String getIpMerged(Region r1, Region r2) {
		String ip = new String(r2.getIp());

		for (int i = 0; i < r1.getIp().length(); i++) {
			if (!ip.contains(r1.getIp().substring(i, i + 1)))
				ip += r1.getIp().substring(i, i + 1);
		}
		return ip;
	}

	private static String mergeString(String s1, String s2) {
		String ip = new String(s2);

		for (int i = 0; i < s1.length(); i++) {
			if (!ip.contains(s1.substring(i, i + 1)))
				ip += s1.substring(i, i + 1);
		}
		return ip;
	}

	// Check if regions r1 and r2 are neighbours
	private static boolean AreNeighbours(Region r1, Region r2) {
		boolean areNeighbours = false;

		int Xmax1 = Integer.parseInt(r1.getUpRight().split("\\.")[0]);
		int Xmax2 = Integer.parseInt(r2.getUpRight().split("\\.")[0]);
		int Ymax1 = Integer.parseInt(r1.getUpRight().split("\\.")[1]);
		int Ymax2 = Integer.parseInt(r2.getUpRight().split("\\.")[1]);

		int Xmin1 = Integer.parseInt(r1.getDownLeft().split("\\.")[0]);
		int Xmin2 = Integer.parseInt(r2.getDownLeft().split("\\.")[0]);
		int Ymin1 = Integer.parseInt(r1.getDownLeft().split("\\.")[1]);
		int Ymin2 = Integer.parseInt(r2.getDownLeft().split("\\.")[1]);

		if (Xmax1 > Xmax2) {
			if (Xmin1 == Xmax2 + 1)
				areNeighbours = true;
		}

		if (Xmax1 < Xmax2) {
			if (Xmin2 == Xmax1 + 1)
				areNeighbours = true;
		}

		if (Ymax1 > Ymax2) {
			if (Ymax2 == Ymin1 - 1)
				areNeighbours = true;
		}

		if (Ymax1 < Ymax2) {
			if (Ymax1 == Ymin2 - 1)
				areNeighbours = true;
		}
		return areNeighbours;
	}

	// Check if regions form a box
	private static boolean FormBox(Region r1, Region r2) {

		if ((Integer.parseInt(r1.getUpRight().split("\\.")[0]) == Integer
				.parseInt(r2.getUpRight().split("\\.")[0]) && Integer
				.parseInt(r1.getDownLeft().split("\\.")[0]) == Integer
				.parseInt(r2.getDownLeft().split("\\.")[0]))
				|| (Integer.parseInt(r1.getUpRight().split("\\.")[1]) == Integer
						.parseInt(r2.getUpRight().split("\\.")[1]) && Integer
						.parseInt(r1.getDownLeft().split("\\.")[1]) == Integer
						.parseInt(r2.getDownLeft().split("\\.")[1]))) {
			return true;
		}

		return false;
	}

	// Check if output port are subsets
	private static boolean OpIsSub(Region r1, Region r2) {

		String r1Op = sortStrAlf(r1.getOp());
		String r2Op = sortStrAlf(r2.getOp());
		if (r1Op.contains(r2Op) || r2Op.contains(r1Op)) {
			return true;
		}

		return false;
	}
	
	public double linkWeightMean(ArrayList<ArrayList<Path>> paths) {
		double acc = 0;
		for(ArrayList<Path> alp : paths) {
			Path path = alp.get(0);
			acc += ((double)path.size()-1.0)*path.volume();
		}
		return acc/(double)graph.getEdges().size();
	}

	public double pathWeightMean(ArrayList<ArrayList<Path>> paths) {
		double acc = 0;
		for(ArrayList<Path> alp : paths)
			acc += (double) (alp.get(0).size()-1);
		return acc*linkWeightMean(paths)/(double)paths.size();
	}

	public double[] pathWeightStats(ArrayList<ArrayList<Path>> paths) {
		double[] stats = new double[2];
		double acc = 0;
		int nPaths = 0;
		for(ArrayList<Path> alp : paths) {
			nPaths += alp.size();
			for(Path path: alp)
				acc += path.getWeight();
		}
		stats[0] = acc/(double)nPaths; // media
		
		acc = 0; double dev;
		for(ArrayList<Path> alp : paths) {
			for(Path path: alp) {
				dev = path.getWeight()-stats[0];
				acc += dev*dev;
			}
		}
		stats[1] = Math.sqrt(acc/(double)nPaths); // desvio padrao
		return stats;
	}

	public double[] pathNormWeightStats(ArrayList<ArrayList<Path>> paths) {
		double[] stats = new double[2];
		double acc = 0;
		int nPaths = 0;
		for(ArrayList<Path> alp : paths) {
			nPaths += alp.size();
			for(Path path: alp)
				acc += path.getWeight()/(double)(path.size()-1);
		}
		stats[0] = acc/(double)nPaths; // media
		
		acc = 0; double dev;
		for(ArrayList<Path> alp : paths) {
			for(Path path: alp) {
				dev = stats[0]-path.getWeight()/(double)(path.size()-1);
				acc += dev*dev;
			}
		}
		stats[1] = Math.sqrt(acc/(double)nPaths); // desvio padrao
		return stats;
	}

	private void addRoutingPath(Vertice v, String ip, String dst, String op) {
		RoutingPath rp = new RoutingPath(sortStrAlf(ip), dst, sortStrAlf(op));
		// @Todo replace this by a Set to not worry with duplication
		if(!routingPathForVertice.get(v).contains(rp))
			routingPathForVertice.get(v).add(rp);
	}

	private static String sortStrAlf(String input) {
		char[] ip1 = input.toCharArray();
		Arrays.sort(ip1);
		return String.valueOf(ip1);
	}
}
