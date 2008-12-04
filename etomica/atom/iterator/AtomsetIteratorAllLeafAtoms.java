package etomica.atom.iterator;

import etomica.action.AtomsetAction;
import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IMolecule;
import etomica.atom.AtomArrayList;
import etomica.atom.AtomsetArrayList;

/**
 * Iterator for all the molecules of a set of species in a box.  Each iterate
 * is all the molecules in a box, with each Atom as the first atom in the 
 * set. This class is used by PotentialMaster to iterate over molecules for 
 * N-body potentials.
 * 
 * This class is designed to work and conform to the API... not to be efficient 
 * or pleasant to look at!  Use neighbor lists. 
 */
public class AtomsetIteratorAllLeafAtoms implements AtomsetIteratorBasisDependent, java.io.Serializable {

    /**
     * @param species species for which molecules are returned as iterates. Only
     * species[0] is relevant, and must not be null.
     */
    public AtomsetIteratorAllLeafAtoms() {
        next = new AtomsetArrayList();
    }

    /**
     * Sets the target of iteration... has no actual effect since all iterates
     * contain all Atoms.
     */
    public void setTarget(IAtom newTargetAtom) {
    }

    public void reset() {
    	AtomArrayList atomList = next.getArrayList();
    	atomList.clear();
    }
    
    public void unset() {
        next.getArrayList().clear();
    }
    
    public IAtomList next() {
    	if(next.getArrayList().getAtomCount()>0){
    		return null;
    	}
        for (int i=0; i<basis.getAtomCount(); i++){
        	next.getArrayList().add(((IMolecule)basis.getAtom(i)).getChildList().getAtom(0));
        }
        return next;
    }
    
    public int nBody() {
        return Integer.MAX_VALUE;
    }
    
    public void allAtoms(AtomsetAction action) {
        reset();
        for (int i=0; i<basis.getAtomCount(); i++){
        	next.getArrayList().add(((IMolecule)basis.getAtom(i)).getChildList().getAtom(0));
        }
        action.actionPerformed(next);
    }

    /**
     * Returns the number of iterates given by this iterator, if iterated after
     * a call to reset().
     */
    public int size() {
        return next.getAtomCount();
    }

    private static final long serialVersionUID = 1L;
    private final AtomsetArrayList next;
	private IAtomList basis;
	public int basisSize() {
		return Integer.MAX_VALUE;
	}

	public boolean haveTarget(IAtom target) {
		for (int i=0; i<basis.getAtomCount(); i++){
			if(((IMolecule)basis.getAtom(i)).getChildList().getAtom(0) == target){
				return true;
			}
        }
		return false;
	}

	public void setBasis(IAtomList atoms) {
		basis = atoms;		
	}
}
