package etomica.nbr.site;

import etomica.api.IAtomLeaf;
import etomica.lattice.AbstractLattice;
import etomica.lattice.RectangularLattice;
import etomica.lattice.SiteFactory;

/**
 * Site used to form array of cells for cell-based neighbor listing.  Each
 * cell is capable of holding lists of atoms that are in them.
 */


public class AtomSite {

    public AtomSite(int latticeArrayIndex) {
        this.latticeArrayIndex = latticeArrayIndex;
    }
    
    public IAtomLeaf getAtom() {return atom;}
    
    public void setAtom(IAtomLeaf atom) {
        this.atom = atom;
    }
    
    public int getLatticeArrayIndex() {
        return latticeArrayIndex;
    }
    
    private IAtomLeaf atom;
    final int latticeArrayIndex;//identifies site in lattice

    public static final SiteFactory FACTORY = new SiteFactory() {
        public Object makeSite(AbstractLattice lattice, int[] coord) {
            return new AtomSite(((RectangularLattice)lattice).arrayIndex(coord));
        }
    };
}
