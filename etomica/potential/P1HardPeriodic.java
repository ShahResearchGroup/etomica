package etomica.potential;

import etomica.api.IAtomSet;
import etomica.api.IAtomTypeSphere;
import etomica.api.IVector;
import etomica.atom.IAtomKinetic;
import etomica.space.ISpace;
import etomica.space.Tensor;

/**
 * pseudo-potential for a "collision" time to update colliders for periodic boundaries
 */
 
public class P1HardPeriodic extends Potential1 implements PotentialHard {

    /**
     * Returns an instance of P1HardPeriodic with sigma = NaN.  call setSigma
     * to set the value you want.
     */
    public P1HardPeriodic(ISpace space) {
        this(space, Double.NaN);
        // use NaN so they'll have to call setSigma later
    }

    /**
     * Returns an instance of P1HardPeriodic with the given value of sigma (the
     * maximum distance between two atoms where they interact)
     */
    public P1HardPeriodic(ISpace space, double sigma) {
        super(space);
        this.sigma = sigma;
    }
    
    /**
     * Returns zero.
     */
    public double energy(IAtomSet a) {
        return 0.0;
    }
     
    /**
     * Returns zero.
     */
    public double energyChange() {
        return 0.0;
    }
    
    public double collisionTime(IAtomSet a, double falseTime) {
        IAtomKinetic atom = (IAtomKinetic)a.getAtom(0);
        if(!(atom.getType() instanceof IAtomTypeSphere)) {return Double.POSITIVE_INFINITY;}
        IVector v = atom.getVelocity();
        IVector dim = boundary.getDimensions();
        double tmin = Double.POSITIVE_INFINITY;
        double d2 = 2.0*sigma;
        int D = dim.getD();
        for(int i=0; i<D; i++) {
            double t = (dim.x(i)-d2)/v.x(i);
            t = (t < 0) ? -t : t;//abs
            tmin = (t < tmin) ? t : tmin;
        }
        return 0.25*tmin + falseTime;
    }
    
    public void setSigma(double newSigma) {
        sigma = newSigma;
    }
    
    public double getSgima() {
        return sigma;
    }
    
    /**
     * Performs no action.
     */
    public void bump(IAtomSet a, double falseTime) { }
    
    /**
     * Returns zero.
     */
    public double lastCollisionVirial() {return 0;}
    
    /**
     * Returns null.
     */
    public Tensor lastCollisionVirialTensor() {return null;}
    
    private static final long serialVersionUID = 1L;
    protected double sigma;
}
   
