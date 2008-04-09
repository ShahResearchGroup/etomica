package etomica.virial;

import etomica.api.IBox;
import etomica.api.IPotential;
import etomica.potential.Potential2Spherical;
import etomica.space.ISpace;
/**
 * @author kofke
 *
 * Simple e-bond, exp(-beta*u).
 */
public class MayerESpherical extends MayerFunctionSpherical {

	/**
	 * Constructor for MayerESpherical.
	 */
	public MayerESpherical(ISpace space, Potential2Spherical potential) {
        super(space);
		this.potential = potential;
	}

	/**
	 * @see etomica.virial.MayerFunctionSpherical#f(etomica.AtomPair, double)
	 */
	public double f(double r2, double beta) {
		return Math.exp(-beta*potential.u(r2));
	}
	
	public void setBox(IBox newBox) {
	    potential.setBox(newBox);
	}

	private final Potential2Spherical potential;

	/* (non-Javadoc)
	 * @see etomica.virial.MayerFunction#getPotential()
	 */
	public IPotential getPotential() {
		// TODO Auto-generated method stub
		return potential;
	}

}
