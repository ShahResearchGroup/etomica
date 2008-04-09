package etomica.modules.osmosis;

import etomica.api.IAtomSet;
import etomica.api.IVector;

import etomica.EtomicaInfo;
import etomica.atom.IAtomKinetic;
import etomica.potential.Potential1;
import etomica.potential.PotentialHard;
import etomica.space.ISpace;
import etomica.space.Tensor;
import etomica.units.Length;

/**
 */
 
public class P1HardWall extends Potential1 implements PotentialHard {
    
    private static final long serialVersionUID = 1L;
    private double collisionRadius;
    
    public P1HardWall(ISpace space) {
        this(space, 1.0);
    }
    
    public P1HardWall(ISpace space, double sigma) {
        super(space);
        collisionRadius = sigma;
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Harmonic potential at the box boundaries");
        return info;
    }

    public double energy(IAtomSet a) {
        double e = 0.0;
        //XXX ignore atoms in the wall.  this can happen due to bogus initial configurations
//        if (Math.abs(((AtomLeaf)a).coord.position().x(0)) < collisionRadius) {
//            e = Double.MAX_VALUE;
//        }
        return e;
    }

     
    public double collisionTime(IAtomSet a, double falseTime) {
        IAtomKinetic atom = (IAtomKinetic)a.getAtom(0);
        IVector r = atom.getPosition();
        IVector v = atom.getVelocity();
        double vx = v.x(0);
        double rx = r.x(0) + vx * falseTime;
        double t = (vx > 0.0) ? - collisionRadius : collisionRadius;
        t = (t - rx) / vx;
        if (t < 0) {
            // moving away from the wall
            t = Double.POSITIVE_INFINITY;
        }
        return t+falseTime;
    }

    public void bump(IAtomSet a, double falseTime) {
        IAtomKinetic atom = (IAtomKinetic)a.getAtom(0);
        IVector v = atom.getVelocity();

        v.setX(0,-v.x(0));

        double newP = atom.getPosition().x(0) - falseTime*v.x(0)*2.0;
        atom.getPosition().setX(0,newP);
    }

    public double energyChange() {
        return 0;
    }
    
    /**
     * not yet implemented
     */
    public double lastCollisionVirial() {return Double.NaN;}
    
    /**
     * not yet implemented.
     */
    public Tensor lastCollisionVirialTensor() {return null;}
    
        
    /**
     * Distance from the center of the sphere to the boundary at collision.
     */
    public void setCollisionRadius(double d) {collisionRadius = d;}
    /**
     * Distance from the center of the sphere to the boundary at collision.
     */
    public double getCollisionRadius() {return collisionRadius;}
    /**
     * Indicates collision radius has dimensions of Length.
     */
    public etomica.units.Dimension getCollisionRadiusDimension() {return Length.DIMENSION;}

}
   
