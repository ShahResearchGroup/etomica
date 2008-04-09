package etomica.potential;

import etomica.space.ISpace;

/**
 * @author kofke
 *
 * General potential that depends on positions of all N molecules, or is
 * otherwise not naturally expressed as a single-, pair-, etc-body potential.
 */

public abstract class PotentialN extends Potential {

	/**
	 * Constructor for PotentialN.
	 * @param sim
	 */
	public PotentialN(ISpace space){
		super(Integer.MAX_VALUE, space);
	}
}
