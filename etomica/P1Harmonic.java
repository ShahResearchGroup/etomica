package etomica;

/**
 * Potential in which attaches a harmonic spring between each affected atom and
 * the nearest boundary in each direction.
 *
 * This class has not been used or checked for correctness.
 *
 * @author David Kofke
 */
 
public class P1Harmonic extends Potential1 implements Potential1.Soft {
    
    public String getVersion() {return "P1Harmonic:01.07.07/"+Potential1.VERSION;}

    private final int D;
    private double w = 100.0;
    private final Space.Vector force;
    
    public P1Harmonic() {
        this(Simulation.instance.hamiltonian.potential);
    }
    
    public P1Harmonic(PotentialGroup parent) {
        super(parent);
        D = parentSimulation().space().D();
        force = parentSimulation().space().makeVector();
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Harmonic potential at the phase boundaries");
        return info;
    }

    public void setSpringConstant(double springConstant) {w = springConstant;}
    public double getSpringConstant() {return w;}
    /**
     * Not implemented correctly.  
     * Returns dimensionless for spring constant.  Should be energy/length^2.
     */
    public etomica.units.Dimension getSpringConstantDimension() {
        return etomica.units.Dimension.NULL;
    }

    public double energy(Atom a) {
        Space.Vector r = a.coord.position();
        Space.Vector d = a.node.parentPhase().boundary().dimensions();
        double aSum = 0.0;
        for(int i=0; i<D; i++) {
            double x = r.x(i);
            double dx = d.x(i);
            if(x > 0.5*dx) x = dx - x;
            aSum += x*x;
        }
        return 0.5*w*aSum;
    }

    public Space.Vector gradient(Atom a){
        Space.Vector r = a.coord.position();
        Space.Vector d = a.node.parentPhase().boundary().dimensions();
        force.E(0.0);
        for(int i=0; i<D; i++) {
            double x = r.x(i);
            double dx = d.x(i);
            if(x > 0.5*dx) {
                x = dx - x;
                force.setX(i, -w*x);
            }
            else force.setX(i, w*x);
        }
        return force;
    }//end of gradient
    
}//end of P1Harmonic
   
