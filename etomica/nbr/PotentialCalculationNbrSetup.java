/*
 * History
 * Created on Sep 10, 2004 by kofke
 */
package etomica.nbr;

import etomica.Atom;
import etomica.AtomsetIterator;
import etomica.IteratorDirective;
import etomica.NearestImageVectorSource;
import etomica.Potential;
import etomica.Space;
import etomica.atom.iterator.AtomsetIteratorDirectable;
import etomica.potential.PotentialCalculation;
import etomica.space.Vector;

/**
 * PotentialCalculation that performs setup of neighbor lists.  Takes all pair iterates
 * it receives and adds each one of the pair to the other's list of neighbors (which
 * are held in the atoms' sequencers).  This action should be invoked by passing an
 * instance of this class to the PotentialMaster's calculate method (the PotentialMaster
 * should be an instance of PotentialMasterNbr), with an iterator directive that specifies
 * no target atom.
 */
public class PotentialCalculationNbrSetup extends PotentialCalculation {

	public PotentialCalculationNbrSetup() {
		super();
	}
    
    

    /* (non-Javadoc)
     * @see etomica.PotentialCalculation#doCalculation(etomica.AtomsetIterator, etomica.IteratorDirective, etomica.Potential)
     */
    public void doCalculation(AtomsetIterator iterator, IteratorDirective id,
            Potential potential) {
        if(iterator instanceof ApiFiltered) {
            nearestImageVectorSource = (NearestImageVectorSource)iterator;
        }
        super.doCalculation(iterator, id, potential);
    }
	/**
     * Takes all pair iterates given by the iterator and puts each one of the
     * pair in the other's neighbor list. Performs no action if
     * the order of the iterator is not exactly 2. The given potential should be
     * a concrete potential, not a potential group (this method is called by the
     * 3-argument PotentialCalculation.doCalculation method after screening out
     * PotentialGroup instances, so this requirement should be met
     * automatically).
     */
	protected void doCalculation(AtomsetIterator iterator, Potential potential) {
		if(iterator.nBody() != 2) return;
		((AtomsetIteratorDirectable)iterator).setDirection(IteratorDirective.UP);
		iterator.reset();
		while(iterator.hasNext()) {
			Atom[] atoms = iterator.next();
            Vector nearestImageVector = nearestImageVectorSource.getNearestImageVector();
            //up and down will be defined here, and might not be consistent
            //with definition used elsewhere
			((AtomSequencerNbr)atoms[0].seq).addUpNbr(atoms[1], potential, nearestImageVector);
			((AtomSequencerNbr)atoms[1].seq).addDownNbr(atoms[0], potential, nearestImageVector);
		}
	}
    
    NearestImageVectorSource nearestImageVectorSource;
}
