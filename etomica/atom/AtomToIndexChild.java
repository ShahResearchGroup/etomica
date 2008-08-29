package etomica.atom;

import java.io.Serializable;

import etomica.api.IAtom;
import etomica.api.IAtomLeaf;


/**
 * Defines the index as the Atom's node's index.
 * @author andrew
 */
public class AtomToIndexChild implements AtomToIndex, Serializable {

    /**
     * @throws NullPointerException if the atom is null.
     */
    public int getIndex(IAtom atom) {
        return ((IAtomLeaf)atom).getIndex();
    }
    
    private static final long serialVersionUID = 1L;

}
