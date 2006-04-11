//includes main method
package etomica.potential;

import etomica.EtomicaInfo;
import etomica.atom.Atom;
import etomica.atom.AtomLeaf;
import etomica.atom.AtomSet;
import etomica.graphics.Drawable;
import etomica.simulation.Simulation;
import etomica.space.ICoordinateKinetic;
import etomica.space.Space;
import etomica.space.Tensor;
import etomica.space.Vector;
import etomica.units.Length;
import etomica.units.Temperature;
import etomica.util.Debug;

/**
 * Potential that places hard repulsive walls coinciding with the
 * boundary of the phase, which is assumed to be rectangular in shape.
 *
 * @author David Kofke
 */
 
 //extends PotentialAbstract instead of Potential1HardAbstract because potential depends
 //on property (boundary.dimensions) of phase, and this is more readily available to
 //the Agent than to the parent potential.  
 //perhaps Potential1HardAbstract should be redesigned to permit easier access to features
 //of the phase by the parent potential, since this is probably a common situation for
 //one-body potentials
 
public class P1HardBoundary extends Potential1 implements PotentialHard, Drawable {
    
    private double collisionRadius = 0.0;
    private boolean isothermal = false;
    private double temperature;
    private final Vector work;
    private int[] pixPosition;
    private int[] thickness;
    private boolean ignoreOverlap;
    
    public P1HardBoundary(Simulation sim) {
        this(sim.space, sim.getDefaults().temperature, sim.getDefaults().ignoreOverlap);
    }
    
    public P1HardBoundary(Space space, double temperature, boolean ignoreOverlap) {
        super(space);
        this.temperature = temperature;
        this.ignoreOverlap = ignoreOverlap;
        work = space.makeVector();
        isActiveDim = new boolean[space.D()][2];
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Hard repulsive potential at the phase boundaries");
        return info;
    }
    
    public double energy(AtomSet a) {
        //XXX this doesn't account for inactive walls!
        double e = 0.0;
        Vector dimensions = boundary.getDimensions();
        double rx = ((AtomLeaf)a).coord.position().x(0);
        double ry = ((AtomLeaf)a).coord.position().x(1);
        double dxHalf = 0.5*dimensions.x(0);
        double dyHalf = 0.5*dimensions.x(1);
        if((rx < -dxHalf+collisionRadius) || (rx > dxHalf-collisionRadius) ||
           (ry < -dyHalf+collisionRadius) || (ry > dyHalf-collisionRadius)) {
            e = Double.POSITIVE_INFINITY;
        }
        else{ 
            e = 0.0;
        }
        return e;
    }
     
    public double energyChange() {return 0.0;}
    
    public double collisionTime(AtomSet a, double falseTime) {
        work.E(((AtomLeaf)a).coord.position());
        Vector v = ((ICoordinateKinetic)((AtomLeaf)a).coord).velocity();
        work.PEa1Tv1(falseTime,v);
        Vector dimensions = boundary.getDimensions();
        double tmin = Double.POSITIVE_INFINITY;
        for(int i=work.D()-1; i>=0; i--) {
            double vx = v.x(i);
            if(vx == 0.0) continue;
            double rx = work.x(i);
            double dxHalf = 0.5*dimensions.x(i);
            double t=0;
            if (vx > 0.0) {
                if (isActiveDim[i][1]) {
                    t = (dxHalf - rx - collisionRadius)/vx;
                }
                else {
                    continue;
                }
            }
            else if (isActiveDim[i][0]) {
                t = (-dxHalf -rx + collisionRadius)/vx;
            }
            else {
                continue;
            }
            if(t < tmin) tmin = t;
        }
        if (ignoreOverlap && tmin<0.0) tmin = 0.0;
        if (Debug.ON && tmin < 0.0) {
            System.out.println("t "+tmin+" "+a+" "+work+" "+v+" "+boundary.getDimensions());
            throw new RuntimeException("you screwed up");
        }
        return tmin + falseTime;
    }
                
//    public void bump(IntegratorHard.Agent agent) {
//        Atom a = agent.atom();
    public void bump(AtomSet a, double falseTime) {
        work.E(((AtomLeaf)a).coord.position());
        Vector v = ((ICoordinateKinetic)((AtomLeaf)a).coord).velocity();
        work.PEa1Tv1(falseTime,v);
        Vector dimensions = boundary.getDimensions();
        double delmin = Double.MAX_VALUE;
        int imin = 0;
        //figure out which component is colliding
        for(int i=work.D()-1; i>=0; i--) {
            double rx = work.x(i);
            double vx = v.x(i);
            double dxHalf = 0.5*dimensions.x(i);
            double del = (vx > 0.0) ? Math.abs(dxHalf - rx - collisionRadius) : Math.abs(-dxHalf - rx + collisionRadius);
            if(del < delmin) {
                delmin = del;
                imin = i;
            }
        }
        if (Debug.ON && Math.abs(work.x(imin)-collisionRadius)/collisionRadius > 1.e-9 
                && Math.abs(dimensions.x(imin)-work.x(imin)-collisionRadius)/collisionRadius > 1.e-9) {
            System.out.println(a+" "+work+" "+dimensions);
            System.out.println("stop that");
        }
        v.setX(imin,-v.x(imin));
        // dv = 2*NewVelocity
        double newP = ((AtomLeaf)a).coord.position().x(imin) - falseTime*v.x(imin)*2.0;
        ((AtomLeaf)a).coord.position().setX(imin,newP);
    }//end of bump
    
    public void setIsothermal(boolean b) {isothermal = b;}
    public boolean isIsothremal() {return isothermal;}
    
    public void setTemperature(double t) {temperature = t;}
    public double getTemperature() {return temperature;}
    public etomica.units.Dimension getTemperatureDimension() {return Temperature.DIMENSION;}
        
    /**
     * not yet implemented
     */
    public double lastCollisionVirial() {return 0;}
    
    /**
     * not yet implemented.
     */
    public Tensor lastCollisionVirialTensor() {return null;}
    
    /**
     * Distance from the center of the sphere to the boundary at collision.
     */
    public void setCollisionRadius(double d) {
        if (d < 0) {
            throw new IllegalArgumentException("collision radius must not be negative");
        }
        collisionRadius = d;
    }
    /**
     * Distance from the center of the sphere to the boundary at collision.
     */
    public double getCollisionRadius() {return collisionRadius;}
    /**
     * Indicates collision radius has dimensions of Length.
     */
    public etomica.units.Dimension getCollisionRadiusDimension() {return Length.DIMENSION;}

    public void setActive(int dim, boolean first, boolean isActive) {
        isActiveDim[dim][first?0:1] = isActive;
    }
    
    public void setLongWall(int dim, boolean first, boolean longWall) {
        if (longWallDim == null) {
            longWallDim = new boolean[2][2];
            pixPosition = new int[2];
            thickness = new int[2];
        }
        longWallDim[dim][first?0:1] = longWall;
    }
    
    public void draw(java.awt.Graphics g, int[] origin, double toPixel) {
        if (boundary == null) return;
        g.setColor(java.awt.Color.gray);
        // if not 2D serious problems!
        for (int i=0; i<2; i++) {
            for (int j=0; j<2; j++) {
                if (!isActiveDim[i][j]) continue;
                pixPosition[i] = origin[i];
                thickness[i] = 1;
                if (longWallDim == null || !longWallDim[i][j]) {
                    pixPosition[1-i] = origin[1-i];
                    thickness[1-i] = (int)(boundary.getDimensions().x(1-i)*toPixel);
                }
                else {
                    pixPosition[1-i] = 0;
                    thickness[1-i] = Integer.MAX_VALUE;
                }
                if (j==1) {
                    pixPosition[i] += (int)(boundary.getDimensions().x(i)*toPixel);
                }
                g.fillRect(pixPosition[0],pixPosition[1],thickness[0],thickness[1]);
            }
        }
    }
    
    private boolean[][] isActiveDim;
    private boolean[][] longWallDim;
        
}
