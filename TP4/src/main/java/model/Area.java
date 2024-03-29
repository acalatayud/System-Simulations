package model;

import java.util.ArrayList;
import java.util.List;

public class Area {

	private double length;
	private double height;
	private double holeLength;
	private List<Particle> particles;
	
	public Area(double length, double height, double holeLength, List<Particle> particles) {
		this.length = length;
		this.height = height;
		this.holeLength = holeLength;
		this.particles = particles;
	}
	
	public double getLength() {
		return length;
	}
	
	public void setLength(double length) {
		this.length = length;
	}

	public List<Particle> getParticles() {
		return particles;
	}

	public void setParticles(List<Particle> particles) {
		this.particles = particles;
	}

	public double getHeight() {
		return height;
	}

	public void setHeight(double height) {
		this.height = height;
	}

	public double getHoleLength() {
		return holeLength;
	}

	public void setHoleLength(double holeLength) {
		this.holeLength = holeLength;
	}

	public List<Pair> getWallPositions(Particle particle) {
		final List<Pair> wallPositions = new ArrayList<>();
		final double holeStart = height / 2 - holeLength / 2;
		final double holeEnd = height / 2 + holeLength / 2;
		final double x = particle.getX();
		final double y = particle.getY();

		if(y >= holeStart && y <= holeEnd) {
			wallPositions.add(new Pair(x, 0));
			wallPositions.add(new Pair(x, height));
			wallPositions.add(new Pair(0, y));
			wallPositions.add(new Pair(2 * length, y));
			wallPositions.add(new Pair(length, holeStart));
			wallPositions.add(new Pair(length, holeEnd));
		} else {
			wallPositions.add(new Pair(x, height));
			wallPositions.add(new Pair(x, 0));
			if(x < length) {
				wallPositions.add(new Pair(0, y));
				wallPositions.add(new Pair(length, y));
			} else {
				wallPositions.add(new Pair(2 * length, y));
				wallPositions.add(new Pair(length, y));
			}
		}

		return wallPositions;
	}

	public boolean leftBox(Particle particle) {
		return particle.getX() < length;
	}

	private boolean holeInteraction(Particle p, Particle o) {
		final double holeStart = height / 2 - holeLength / 2;
		final double holeEnd = height / 2 + holeLength / 2;
		final double slope = (o.getY() - p.getY()) / (o.getX() - p.getX());
		final double wallDistance = length - p.getX();

		final double wallPoint = slope * wallDistance + p.getY();

		return wallPoint >= holeStart && wallPoint <= holeEnd;
	}

	public boolean forceInteraction(Particle p, Particle o) {
		final double holeStart = height / 2 - holeLength / 2;
		final double holeEnd = height / 2 + holeLength / 2;
		if(p.getX() < length && o.getX() < length)
			return true;
		else if(p.getX() > length && o.getX() > length)
			return true;
		else return holeInteraction(p, o);
	}
}
