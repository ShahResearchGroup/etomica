package etomica.nbr.cell;

import etomica.api.IAtomLeaf;
import etomica.atom.AtomArrayList;
import etomica.lattice.AbstractLattice;
import etomica.lattice.RectangularLattice;
import etomica.lattice.SiteFactory;

/**
 * Site used to form array of cells for cell-based neighbor listing.  Each
 * cell is capable of holding lists of atoms that are in them.
 */
public class Cell implements java.io.Serializable {

    public Cell(int latticeArrayIndex) {
        this.latticeArrayIndex = latticeArrayIndex;
    }
    
    public AtomArrayList occupants() {
        return occupants;
    }
    
    public void addAtom(IAtomLeaf atom) {
        occupants.add(atom);
    }
    
    public void removeAtom(IAtomLeaf atom) {
        occupants.removeAndReplace(occupants.indexOf(atom));
    }
    
    public int getLatticeArrayIndex() {
        return latticeArrayIndex;
    }
    
    private final AtomArrayList occupants = new AtomArrayList(1);
    final int latticeArrayIndex;//identifies site in lattice

    private static final long serialVersionUID = 1L;
    public static final SiteFactory FACTORY = new CellFactory();
    
    public static class CellFactory implements SiteFactory, java.io.Serializable {
        private static final long serialVersionUID = 1L;

        public Object makeSite(AbstractLattice lattice, int[] coord) {
            return new Cell(((RectangularLattice)lattice).arrayIndex(coord));
        }
    };
}
