
package etomica.models.water;

import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IVector;
import etomica.potential.Potential2;
import etomica.space.Space;
import etomica.space3d.IVector3D;

/** 
 * 3-point potential for water.  Potential parameters are typically defined
 * by a convenience subclass.
 * 
 * @author David Kofke, Andrew Schultz
 */
public class P2Water3P extends Potential2 {

	public P2Water3P(Space space, double sigma, double epsilon, double chargeO, double chargeH) {
		super(space);
        this.sigma = sigma;
        sigma2 = sigma*sigma;
		work = (IVector3D)space.makeVector();
		shift = (IVector3D)space.makeVector();
        this.epsilon = epsilon;
        epsilon4 = 4*epsilon;
        chargeOO = chargeO * chargeO;
        chargeOH = chargeO * chargeH;
        chargeHH = chargeH * chargeH;
        this.chargeO = chargeO;
        this.chargeH = chargeH;
	}

    public void setBox(IBox box) {
        boundary = box.getBoundary();
    }

    public double energy(IAtomSet pair){
		double sum = 0.0;
		double r2 = 0.0;

		IMolecule water1 = (IMolecule)pair.getAtom(0);
		IMolecule water2 = (IMolecule)pair.getAtom(1);
		
		//compute O-O distance to consider truncation	
        IVector O1r = ((IAtomPositioned)water1.getChildList().getAtom(2)).getPosition();
        IVector O2r = ((IAtomPositioned)water2.getChildList().getAtom(2)).getPosition();

		work.Ev1Mv2(O1r, O2r);
        shift.Ea1Tv1(-1,work);
		boundary.nearestImage(work);
        shift.PE(work);
        final boolean zeroShift = shift.squared() < 0.1;
		r2 = work.squared();

		if(r2<1.6) return Double.POSITIVE_INFINITY;
	
		sum += chargeOO/Math.sqrt(r2);
		double s2 = sigma2/(r2);
		double s6 = s2*s2*s2;
		sum += epsilon4*s6*(s6 - 1.0);
		
        IVector H11r = ((IAtomPositioned)water1.getChildList().getAtom(0)).getPosition();
        IVector H12r = ((IAtomPositioned)water1.getChildList().getAtom(1)).getPosition();
        IVector H21r = ((IAtomPositioned)water2.getChildList().getAtom(0)).getPosition();
        IVector H22r = ((IAtomPositioned)water2.getChildList().getAtom(1)).getPosition();
        		
        if (zeroShift) {
            r2 = O1r.Mv1Squared(H21r);
            sum += chargeOH/Math.sqrt(r2);
            r2 = O1r.Mv1Squared(H22r);
            sum += chargeOH/Math.sqrt(r2);
            r2 = H11r.Mv1Squared(O2r);
            sum += chargeOH/Math.sqrt(r2);
            r2 = H11r.Mv1Squared(H21r);
            sum += chargeHH/Math.sqrt(r2);
            r2 = H11r.Mv1Squared(H22r);
            sum += chargeHH/Math.sqrt(r2);
            r2 = H12r.Mv1Squared(O2r);
            sum += chargeOH/Math.sqrt(r2);
            r2 = H12r.Mv1Squared(H21r);
            sum += chargeHH/Math.sqrt(r2);
            r2 = H12r.Mv1Squared(H22r);
            sum += chargeHH/Math.sqrt(r2);
        }
        else {
            shift.PE(O1r);
            r2 = H21r.Mv1Squared(shift);
            shift.ME(O1r);
            sum += chargeOH/Math.sqrt(r2);

            shift.PE(O1r);
            r2 = H22r.Mv1Squared(shift);
            shift.ME(O1r);
            sum += chargeOH/Math.sqrt(r2);

            shift.PE(H11r);
            r2 = O2r.Mv1Squared(shift);
            shift.ME(H11r);
            sum += chargeOH/Math.sqrt(r2);

            shift.PE(H11r);
            r2 = H21r.Mv1Squared(shift);
            shift.ME(H11r);
            sum += chargeHH/Math.sqrt(r2);

            shift.PE(H11r);
            r2 = H22r.Mv1Squared(shift);
            shift.ME(H11r);
            sum += chargeHH/Math.sqrt(r2);

            shift.PE(H12r);
            r2 = O2r.Mv1Squared(shift);
            shift.ME(H12r);
            sum += chargeOH/Math.sqrt(r2);

            shift.PE(H12r);
            r2 = H21r.Mv1Squared(shift);
            shift.ME(H12r);
            sum += chargeHH/Math.sqrt(r2);

            shift.PE(H12r);
            r2 = H22r.Mv1Squared(shift);
            shift.ME(H12r);
            sum += chargeHH/Math.sqrt(r2);
        }

		return sum;																					        
	}

    public double getRange() {
        return Double.POSITIVE_INFINITY;
    }
    
	public double getSigma() {return sigma;}
    
	public double getEpsilon() {return epsilon;}
    
    private static final long serialVersionUID = 1L;
	public double sigma , sigma2;
	protected double epsilon, epsilon4;
	protected IBoundary boundary;
	protected final double chargeH;
	protected final double chargeO;
	protected final double chargeOO, chargeOH, chargeHH;
	protected final IVector3D work, shift;
}
