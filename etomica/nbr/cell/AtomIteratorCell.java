package etomica.nbr.cell;

import etomica.action.AtomAction;
import etomica.action.AtomsetAction;
import etomica.action.AtomsetCount;
import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IBox;
import etomica.atom.AtomArrayList;
import etomica.atom.AtomSetSinglet;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.box.BoxAgentManager;
import etomica.lattice.CellLattice;
import etomica.lattice.RectangularLattice;

/**
 * Returns occupants of all cells as iterates.
 */

public class AtomIteratorCell implements AtomIterator, java.io.Serializable {

    /**
     * Constructor makes iterator that must have box specified and then be
     * reset() before iteration.
     * 
     * @param D
     *            the dimension of the space of the simulation (used to
     *            construct cell iterators)
     */
	public AtomIteratorCell(int D, BoxAgentManager agentManager) {
        cellIterator = new RectangularLattice.Iterator(D);
        atomIterator = new AtomIteratorArrayListSimple();
        boxAgentManager = agentManager;
        atomSetSinglet = new AtomSetSinglet();
	}

	public void setBox(IBox box) {
        CellLattice lattice = ((NeighborCellManager)boxAgentManager.getAgent(box)).getLattice();
        cellIterator.setLattice(lattice);
        unset();
	}
    
    /**
     * Performs action on all iterates.
     */
    public void allAtoms(AtomsetAction action) {
        cellIterator.reset();
        while(cellIterator.hasNext()) {//outer loop over all cells
            Cell cell = (Cell)cellIterator.next();
            AtomArrayList list = cell.occupants();
            
            //consider pairs formed from molecules in cell
            if(!list.isEmpty()) {
                atomIterator.setList(list);
                atomIterator.allAtoms(action);
            }
        }//end of outer loop over cells
    }//end of allAtoms
    
    /**
     * Performs action on all iterates.
     */
    public void allAtoms(AtomAction action) {
        cellIterator.reset();
        while(cellIterator.hasNext()) {//outer loop over all cells
            Cell cell = (Cell)cellIterator.next();
            AtomArrayList list = cell.occupants();
            
            //consider pairs formed from molecules in cell
            if(!list.isEmpty()) {
                atomIterator.setList(list);
                atomIterator.allAtoms(action);
            }
        }//end of outer loop over cells
    }//end of allAtoms
    
	/**
     * Returns the number of atoms the iterator will return if reset and
     * iterated in its present state.
     */
	public int size() {
        AtomsetCount counter = new AtomsetCount();
        allAtoms(counter);
        return counter.callCount();
	}
	
    public boolean hasNext() {
        throw new RuntimeException("jfdka");
    }
    
    public final IAtomList next() {
        atomSetSinglet.atom = nextAtom();
        return atomSetSinglet;
    }
    
    public IAtom nextAtom() {
        IAtom nextAtom = atomIterator.nextAtom();
        while (nextAtom == null) {
            if(cellIterator.hasNext()) {
                AtomArrayList list = ((Cell)cellIterator.next()).occupants();
                atomIterator.setList(list);
                atomIterator.reset();
            } else {//no more cells at all
                break;
            }
            nextAtom = atomIterator.nextAtom();
        }
        return nextAtom;
    }
    
    public void unset() {
        atomIterator.unset();
    }

    /**
     * Returns 1, indicating that this is an atom iterator.
     */
    public int nBody() {
        return 1;
    }
    
    public void reset() {
        cellIterator.reset();
        atomIterator.unset();
    }
    
    private static final long serialVersionUID = 1L;
    private final AtomIteratorArrayListSimple atomIterator;
    private final RectangularLattice.Iterator cellIterator;
    private final BoxAgentManager boxAgentManager;
    protected final AtomSetSinglet atomSetSinglet;
}
