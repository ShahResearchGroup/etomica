package etomica.atom.iterator;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IAtomType;

public class AtomIteratorBasisFilteredType extends AtomIteratorBasis {

    public AtomIteratorBasisFilteredType(IAtomType type) {
        super();
        filteredType = type;
    }
    
    public IAtom nextAtom() {
        IAtom atom = super.nextAtom();
        while (atom != null) {
            if (atom.getType() == filteredType) {
                return atom;
            }
            atom = super.nextAtom();
        }
        return null;
    }

    public IAtomList next() {
        IAtomList atom = super.next();
        while (atom != null) {
            if (atom.getAtom(0).getType() == filteredType) {
                return atom;
            }
            atom = super.next();
        }
        return null;
    }

    public int size() {
        reset();
        int count = 0;
        for (IAtom atom = nextAtom(); atom != null; atom = nextAtom()) {
            count++;
        }
        return count;
    }

    private static final long serialVersionUID = 1L;
    protected final IAtomType filteredType;
}
