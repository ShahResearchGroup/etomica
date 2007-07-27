package etomica.virial;

import etomica.atom.AtomSet;
import etomica.potential.IPotential;
import etomica.potential.Potential;

/**
 * @author kofke
 *
 * General Mayer function, which wraps the Mayer potential around an instance of
 * a Potential2 object.
 */
public class MayerGeneral implements MayerFunction, java.io.Serializable {

	/**
	 * Constructor Mayer function using given potential.
	 */
	public MayerGeneral(IPotential potential) {
		this.potential = potential;
	}

	public double f(AtomSet pair, double beta) {
		return Math.exp(-beta*potential.energy(pair)) - 1.0;
	}

	private final IPotential potential;

	/* (non-Javadoc)
	 * @see etomica.virial.MayerFunction#getPotential()
	 */
	public IPotential getPotential() {
		// TODO Auto-generated method stub
		return potential;
	}
}
