import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

import model.Area;
import model.Collision;
import model.CollisionType;
import model.Particle;

public class Simulation {
	
	static boolean append = false;
	static boolean timesAppend = false;
	
	public static void simulate(Options options, int particleId) {
		Area area = initSimulation(options);
		List<Particle> particles = area.getParticles();
		double length = area.getLength();
		PriorityQueue<Collision> collisions = new PriorityQueue<>();
		double totalTime = 0;
		int totalCollisions = 0;

		double initialX = particles.get(particleId).getX();
		double initialY = particles.get(particleId).getY();
		logMeanSquaredDisplacement(particles.get(particleId), initialX, initialY, totalTime);
		
		calculateCollisions(particles, length, collisions);
		logParticles(particles, length, totalTime);

		System.out.println("Energia Cinetica: " + calculateKineticEnergy(particles));
		logVelocityModules(particles, totalTime);
		boolean daBigTouchDaWall = false;
		append = true;
		while(!daBigTouchDaWall) {
			Collision collision = collisions.remove();
			double time = collision.getTime();
			totalTime += time;
			particles.parallelStream().forEach(p -> p.evolvePosition(time));
			collisions.parallelStream().forEach(c -> c.updateTime(time));
			logParticles(particles, length, totalTime);
			collision.collide();
			recalculateCollisions(particles, length, collision.getParticle1(), collisions);
			recalculateCollisions(particles, length, collision.getParticle2(), collisions);
			daBigTouchDaWall = collision.getParticle1().getId() == particleId && collision.getParticle2() == null;
			totalCollisions++;
			logCollisionTime(totalTime);
			logVelocityModules(particles, totalTime);
			logMeanSquaredDisplacement(particles.get(particleId), initialX, initialY, totalTime);
		}

		System.out.println("Colisiones: " + totalCollisions);
		System.out.println("Tiempo: " + totalTime);
	}
	
	public static void simulate(Options options, double dt) {
		Area area = initSimulation(options);
		List<Particle> particles = area.getParticles();
		double length = area.getLength();
		PriorityQueue<Collision> collisions = new PriorityQueue<>();
		
		calculateCollisions(particles, length, collisions);
		logParticles(particles, length);
		
		boolean daBigTouchDaWall = false;
		double timeSinceLastCollision = 0;
		while(!daBigTouchDaWall) {
			Collision collision = collisions.peek();
			double time = collision.getTime();
			if (timeSinceLastCollision + dt >= time) {
				collisions.remove();
				double advance = time - timeSinceLastCollision;
				timeSinceLastCollision = 0;
				particles.parallelStream().forEach(p -> p.evolvePosition(advance));
				collisions.parallelStream().forEach(c -> c.updateTime(time));
				collision.collide();
				recalculateCollisions(particles, length, collision.getParticle1(), collisions);
				recalculateCollisions(particles, length, collision.getParticle2(), collisions);
				daBigTouchDaWall = collision.getParticle1().isBig() && collision.getParticle2() == null;
			} else {
				particles.parallelStream().forEach(p -> p.evolvePosition(dt));
				timeSinceLastCollision += dt;
				logParticles(particles, length);
			}
		}
		logParticles(particles, length);
	}
	
	private static Area initSimulation(Options options) {
		List<Particle> particles = new ArrayList<>();
		particles.add(new Particle(0, options.getLength()/2, options.getLength()/2, options.getBigRadius(), 0, 0, options.getBigMass(), true));
		int i = 0;
		boolean overlapped;
		while(i < options.getN()) {
			double ang = rand(0, 2*Math.PI);
			double mod = rand(0, options.getVelocityRange());
			double x = mod * Math.cos(ang);
			double y = mod * Math.sin(ang);
			Particle particle = new Particle(i+1, rand(options.getLittleRadius(), options.getLength()-options.getLittleRadius()),
					rand(options.getLittleRadius(), options.getLength()-options.getLittleRadius()), options.getLittleRadius(),
			x, y, options.getLittleMass(), false);
			overlapped = false;
			for (Particle p : particles) {
				if (particle.isOverlapped(p)) {
					overlapped = true;
					break;
				}
			}
			if (!overlapped) {
				particles.add(particle);
				i++;
			}
		}
		return new Area(options.getLength(), particles);
	}
	
	private static void calculateCollisions(List<Particle> particles, double length, PriorityQueue<Collision> collisions) {
		Particle particle1, particle2;
		Double collisionTime;
		
		for (int i = 0; i < particles.size(); i++) {
			particle1 = particles.get(i);
			for (int j = i + 1; j < particles.size(); j++) {
				particle2 = particles.get(j);
				collisionTime = particle1.calculateCollisionTime(particle2);
				if (collisionTime != null) {
					collisions.add(new Collision(particle1, particle2, CollisionType.PARTICLE_VS_PARTICLE, collisionTime));
				}
			}
			collisionTime = particle1.calculateCollisionTime(CollisionType.PARTICLE_VS_HWALL, length);
			if (collisionTime != null) {
				collisions.add(new Collision(particle1, null, CollisionType.PARTICLE_VS_HWALL, collisionTime));
			}
			collisionTime = particle1.calculateCollisionTime(CollisionType.PARTICLE_VS_VWALL, length);
			if (collisionTime != null) {
				collisions.add(new Collision(particle1, null, CollisionType.PARTICLE_VS_VWALL, collisionTime));
			}
		}
	}
	
	private static void recalculateCollisions(List<Particle> particles, double length, Particle particle, PriorityQueue<Collision> collisions) {
		if (particle == null)
			return;

		collisions.removeIf(c -> c.hasParticle(particle));
		
		Double collisionTime;
		for (Particle p : particles) {
			if (!p.equals(particle)) {
				collisionTime = particle.calculateCollisionTime(p);
				if (collisionTime != null) {
					collisions.add(new Collision(particle, p, CollisionType.PARTICLE_VS_PARTICLE, collisionTime));
				}
			}
		}

		collisionTime = particle.calculateCollisionTime(CollisionType.PARTICLE_VS_HWALL, length);
		if (collisionTime != null) {
			collisions.add(new Collision(particle, null, CollisionType.PARTICLE_VS_HWALL, collisionTime));
		}
		collisionTime = particle.calculateCollisionTime(CollisionType.PARTICLE_VS_VWALL, length);
		if (collisionTime != null) {
			collisions.add(new Collision(particle, null, CollisionType.PARTICLE_VS_VWALL, collisionTime));
		}
	}
	
    private static void logParticles(List<Particle> particles, double length) {
    	File file = new File("output.xyz");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, append);
			append = true;
		} catch (FileNotFoundException e) {
			return;
		}
		PrintStream ps = new PrintStream(fos);

		ps.println(particles.size());
		ps.println("Lattice=\"1.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0\" Properties=pos:R:2:vel:R:2:radius:R:1:mass:R:1");
		for (Particle p : particles) {
			ps.println(p.getX() + " " + p.getY() + " " + p.getVx() + " " + p.getVy() + " " + p.getRadius() + " " + p.getMass());
		}
		
		ps.close();
    }

	private static void logCollisionTime(double time) {
		File file = new File("collision_times.data");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, timesAppend);
			timesAppend = true;
		} catch (FileNotFoundException e) {
			return;
		}
		PrintStream ps = new PrintStream(fos);
		ps.println(time);
		ps.close();
	}

	private static void logVelocityModules(List<Particle> particles, double time) {
		File file = new File("velocity_modules.data");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, append);
		} catch (FileNotFoundException e) {
			return;
		}

		PrintStream ps = new PrintStream(fos);

		if(!append) {
			ps.println(particles.size());
		}

		ps.println(time);
		for (Particle particle: particles) {
			ps.println(particle.getVelocityModule());
		}

		ps.println();

		ps.close();
	}

	private static void logMeanSquaredDisplacement(Particle particle, double initialX, double initialY, double time) {
		File file = new File("MSD.data");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, append);
		} catch (FileNotFoundException e) {
			return;
		}

		PrintStream ps = new PrintStream(fos);
		ps.println(time + " " + calculateMeanSquaredDisplacement(particle, initialX, initialY) + " " + particle.getX() + " " + particle.getY());
		ps.close();
	}

    private static double calculateKineticEnergy(final List<Particle> particles) {

		return particles.stream()
				.mapToDouble(particle -> 0.5 * (particle.getMass() / 1000) * (Math.pow(particle.getVx(), 2) + Math.pow(particle.getVy(), 2)))
				.average().orElse(0);
	}

	private static double calculateMeanSquaredDisplacement(Particle particle, double initialX, double initialY) {

		return Math.pow(particle.getX() - initialX, 2) + Math.pow(particle.getY() - initialY, 2);

	}

	private static void logParticles(List<Particle> particles, double length, double time) {
		File file = new File("output.xyz");
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(file, append);
		} catch (FileNotFoundException e) {
			return;
		}
		PrintStream ps = new PrintStream(fos);

		ps.println(particles.size());
		ps.println("Lattice=\"1.0 0.0 0.0 0.0 1.0 0.0 0.0 0.0 1.0\"");
		for (Particle p : particles) {
			ps.println(p.getX() + " " + p.getY() + " " + p.getVx() + " " + p.getVy() + " " + p.getRadius() + " " + p.getMass()+ " " + p.getVelocityModule());
		}

		ps.close();
	}
	
	private static double rand(double min, double max) {
		return ThreadLocalRandom.current().nextDouble(min, max);
	}

}
