package forces;

import java.util.List;

import interfaces.Force;
import model.Area;
import model.Pair;
import model.Particle;

public class DampedOscillator implements Force {

    private static final double K = 10000;
    private static final double GAMMA = 100.0;
    private static final double A = 1.0;
    private static final boolean velocityDependant = true;
    
	@Override
	public void calculate(List<Particle> particles, Area area) { }

    @Override
    public Pair recalculateForce(Particle particle, List<Particle> particles, Area area) {
	    return getForce(particle);
    }

    @Override
    public Pair getForce(final Particle particle) {
        final double x = -K * particle.getX() - GAMMA * particle.getVx();
        return new Pair(x, 0);
    }

    @Override
    public Pair getD1(final Particle particle) {
        //final double x = K * GAMMA * particle.getX() / particle.getMass() + (GAMMA * GAMMA / particle.getMass() - K) * particle.getVx();
        return getForce(particle).multiplyByScalar(-GAMMA / particle.getMass()).substract(new Pair(particle.getVelocity()).multiplyByScalar(K)) ;  
    	//return new Pair(x, 0);
    }

    @Override
    public Pair getD2(final Particle particle) {
        //final double x = (K * K - K * GAMMA * GAMMA) * particle.getX() + (2 * K * GAMMA - Math.pow(GAMMA, 3)) * particle.getVx();
        //return new Pair(x, 0);
    	return getD1(particle).multiplyByScalar(-GAMMA / particle.getMass()).substract(getForce(particle).multiplyByScalar(K / particle.getMass())) ;
    }

    @Override
    public Pair getD3(final Particle particle) {
        //final double x = (K * Math.pow(GAMMA, 3) - 2 * K * K * GAMMA) * particle.getX()
        //        + (K * K - 3 * K * GAMMA * GAMMA + Math.pow(GAMMA, 4)) * particle.getVx();
        //return new Pair(x, 0);
    	return getD2(particle).multiplyByScalar(-GAMMA / particle.getMass()).substract(getD1(particle).multiplyByScalar(K / particle.getMass())) ;
    }

    @Override
    public Pair getAnalyticalSolution(final Particle particle, final double time) {
        final double x = A * Math.exp(-GAMMA * time / (2 * particle.getMass())) *
                Math.cos(Math.sqrt(K / particle.getMass() - GAMMA * GAMMA / (4 * particle.getMass() * particle.getMass())) * time);
        return new Pair(x, 0);
    }

    @Override
    public boolean isVelocityDependant() {
        return velocityDependant;
    }

    @Override
    public double getPotentialEnergy(Particle particle) {
        return 0;
    }

}
