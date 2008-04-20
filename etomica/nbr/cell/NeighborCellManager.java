package etomica.nbr.cell;

import etomica.action.AtomActionTranslateBy;
import etomica.action.AtomGroupAction;
import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomPositionCOM;
import etomica.atom.AtomSetSinglet;
import etomica.atom.MoleculeAgentManager;
import etomica.atom.MoleculeAgentManager.MoleculeAgentSource;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorTreeBox;
import etomica.box.BoxCellManager;
import etomica.box.BoxEvent;
import etomica.box.BoxInflateEvent;
import etomica.box.BoxListener;
import etomica.integrator.mcmove.MCMoveBox;
import etomica.integrator.mcmove.MCMoveEvent;
import etomica.integrator.mcmove.MCMoveListener;
import etomica.integrator.mcmove.MCMoveTrialCompletedEvent;
import etomica.lattice.CellLattice;
import etomica.space.ISpace;
import etomica.util.Debug;

/**
 * Class that defines and manages construction and use of lattice of cells 
 * for cell-based neighbor listing.
 */

//TODO modify assignCellAll to loop through cells to get all atoms to be assigned
//no need for index when assigning cell
//different iterator needed

public class NeighborCellManager implements BoxCellManager, AtomLeafAgentManager.AgentSource, MoleculeAgentSource, BoxListener, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    protected final ISimulation sim;
    protected final CellLattice lattice;
    protected final ISpace space;
    protected final AtomIteratorTreeBox atomIterator;
    protected final IAtomPositionDefinition positionDefinition;
    protected final IBox box;
    protected int cellRange = 2;
    protected double range;
    protected final AtomLeafAgentManager agentManager;
    protected final MoleculeAgentManager moleculeAgentManager;
    
    /**
     * Constructs manager for neighbor cells in the given box.  The number of
     * cells in each dimension is given by nCells. Position definition for each
     * atom is that given by its type (it is set to null in this class).
     */
    public NeighborCellManager(ISimulation sim, IBox box, double potentialRange, ISpace _space) {
        this(sim, box, potentialRange, null, _space);
    }
    
    /**
     * Construct manager for neighbor cells in the given box.  The number
     * of cells in each dimension is given by nCells.  Position definition is
     * used to determine the cell a given atom is in; if null, the position
     * definition given by the atom's type is used.  Position definition is
     * declared final.
     */
    public NeighborCellManager(ISimulation sim, IBox box, double potentialRange, IAtomPositionDefinition positionDefinition, ISpace _space) {
        this.positionDefinition = positionDefinition;
        this.box = box;
        this.sim = sim;
        space = _space;
        atomIterator = new AtomIteratorTreeBox();
        atomIterator.setDoAllNodes(true);
        atomIterator.setBox(box);

        lattice = new CellLattice(box.getBoundary().getDimensions(), Cell.FACTORY);
        setPotentialRange(potentialRange);
        agentManager = new AtomLeafAgentManager(this,box);
        moleculeAgentManager = new MoleculeAgentManager(sim, box, this);
    }

    public CellLattice getLattice() {
        return lattice;
    }

    /**
     * Sets the potential range to the given value.  Cells are made large 
     * enough so that cellRange*cellSize > potentialRange.
     */
    public void setPotentialRange(double newRange) {
        range = newRange;
        if (checkDimensions() && agentManager != null) {
            assignCellAll();
        }
    }
    
    /**
     * Returns the potential range.
     */
    public double getPotentialRange() {
        return range;
    }
    
    /**
     * Returns the cellRange.
     */
    public int getCellRange() {
        return cellRange;
    }

    /**
     * Sets the cell range to the given value.  Cells are made large 
     * enough so that cellRange*cellSize > potentialRange
     */
    public void setCellRange(int newCellRange) {
        cellRange = newCellRange;
        checkDimensions();
    }
    
    /**
     * Checks the box's dimensions to make sure the number of cells is 
     * appropriate.
     */
    protected boolean checkDimensions() {
        if (range == 0) {
            // simulation is still being constructed, don't try to do anything useful
            return false;
        }
    	int D = space.D();
        int[] nCells = new int[D];
        IVector dimensions = box.getBoundary().getDimensions();
        lattice.setDimensions(dimensions);
        for (int i=0; i<D; i++) {
            nCells[i] = (int)Math.floor(cellRange*dimensions.x(i)/range);
            if (nCells[i] < cellRange*2+1) {
                // the box is too small for the lattice, but things might still be OK.
                // it's possible the box is big enough for the potential range, but
                // the extra padding that's needed for happy cells is missing.
                // capping the number of cells means we can proceed and just don't
                // get any advantage from using cells in this direction.  ideally
                // we would make the neighbor iterator non-wrapping in this direction
                // and use 1 cell.
                nCells[i] = cellRange*2+1;
                if (range > dimensions.x(i)/2) {
                    // box was too small for the potentials too.  doh.
                    // Perhaps the direction is not periodic or we're in the middle
                    // of multiple changes which will (in the end) be happy.
                    System.err.println("range is greater than half the box length in direction "+i);
                }
                if (Debug.ON) System.err.println("capping number of cells in direction "+i+" at "+nCells[i]);
            }
        }
        //only update the lattice (expensive) if the number of cells changed
        int[] oldSize = lattice.getSize();
        for (int i=0; i<D; i++) {
            if (oldSize[i] != nCells[i]) {
                lattice.setSize(nCells);
                return true;
            }
        }
        return false;
    }
    
    /**
     * Assigns cells to all interacting atoms in the box.  Interacting atoms
     * are those that have one or more potentials that act on them.  
     */
    public void assignCellAll() {
        // ensure that any changes to cellRange, potentialRange and boundary
        // dimension take effect.  checkDimensions can call us, but if that
        // happens, our call into checkDimensions should 
        checkDimensions();

        Object[] allCells = lattice.sites();
        for (int i=0; i<allCells.length; i++) {
            ((Cell)allCells[i]).occupants().clear();
        }
        
        IAtomSet leafList = box.getLeafList();
        int count = leafList.getAtomCount();
        for (int i=0; i<count; i++) {
            IAtomLeaf atom = (IAtomLeaf)leafList.getAtom(i);
            if (atom.getType().isInteracting()) {
                assignCell(atom);
            }
            else {
                // the atom is probably not in a cell, but if it is, this is
                // the only way it will get purged.  This is probably as cheap
                // or cheaper than checking first.
                agentManager.setAgent(atom, null);
            }
        }

        ISpecies[] species = sim.getSpeciesManager().getSpecies();
        for (int i=0; i<species.length; i++) {
            if (!species[i].isInteracting()) {
                // should we try removing the molecules from cells
                continue;
            }
            IAtomSet moleculeList = box.getMoleculeList(species[i]);
            count = moleculeList.getAtomCount();
            for (int j=0; j<count; j++) {
                IMolecule atom = (IMolecule)moleculeList.getAtom(i);
                assignCell(atom);
            }
        }
    }
    
    public Cell getCell(IAtom atom) {
        return (Cell)agentManager.getAgent(atom);
    }

    /**
     * Assigns the cell for the given atom.  The atom will be listed in the
     * cell's atom list and the cell with be associated with the atom via
     * agentManager.
     */
    public void assignCell(IAtomLeaf atom) {
        Cell atomCell = (Cell)lattice.site(((IAtomPositioned)atom).getPosition());
        atomCell.addAtom(atom);
        agentManager.setAgent(atom, atomCell);
    }
    
    /**
     * Assigns the cell for the given atom.  The atom will be listed in the
     * cell's atom list and the cell with be associated with the atom via
     * agentManager.
     */
    public void assignCell(IMolecule atom) {
        IVector position = (positionDefinition != null) ?
                positionDefinition.position(atom) :
                    atom.getType().getPositionDefinition().position(atom);
        Cell atomCell = (Cell)lattice.site(position);
        atomCell.addAtom(atom);
        moleculeAgentManager.setAgent(atom, atomCell);
    }
    
    public MCMoveListener makeMCMoveListener() {
        return new MyMCMoveListener(space,box,this);
    }
    
    public Class getAgentClass() {
        return Cell.class;
    }

    /**
     * Returns the cell containing the given atom.  The atom is added to the
     * cell's atom list.
     */
    public Object makeAgent(IAtom atom) {
        if (atom.getType().isInteracting()) {
            IVector position = ((IAtomPositioned)atom).getPosition();
            Cell atomCell = (Cell)lattice.site(position);
            atomCell.addAtom(atom);
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(atom))) {
                System.out.println("assigning new "+atom+" "+atom.getGlobalIndex()+" at "+position+" to "+atomCell);
            }
            return atomCell;
        }
        return null;
    }

    /**
     * Removes the given atom from the cell.
     */
    public void releaseAgent(Object cell, IAtom atom) {
        ((Cell)cell).removeAtom(atom);
    }
    
    public Class getMoleculeAgentClass() {
        return Cell.class;
    }

    /**
     * Returns the cell containing the given atom.  The atom is added to the
     * cell's atom list.
     */
    public Object makeAgent(IMolecule atom) {
        if (atom.getType().isInteracting()) {
            IVector position = (positionDefinition != null) ?
                    positionDefinition.position(atom) :
                        atom.getType().getPositionDefinition().position(atom);
            Cell atomCell = (Cell)lattice.site(position);
            atomCell.addAtom(atom);
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(new AtomSetSinglet(atom))) {
                System.out.println("assigning new "+atom+" "+atom.getGlobalIndex()+" at "+position+" to "+atomCell);
            }
            return atomCell;
        }
        return null;
    }

    /**
     * Removes the given atom from the cell.
     */
    public void releaseAgent(Object cell, IMolecule atom) {
        ((Cell)cell).removeAtom(atom);
    }
    
    public void actionPerformed(BoxEvent event) {
        if (event instanceof BoxInflateEvent) {
            checkDimensions();
            // we need to reassign cells even if checkDimensions didn't resize
            // the lattice.  If the box size changed, the cell size changed,
            // and the atom assignments need to change too.
            //FIXME but only if we have multi-atomic molecules.  For monatomic
            // molecules, we would only need to call this if the lattice size
            // changes
            assignCellAll();
        }
    }
    
    private static class MyMCMoveListener implements MCMoveListener, java.io.Serializable {
        public MyMCMoveListener(ISpace space, IBox box, NeighborCellManager manager) {
            moleculePosition = new AtomPositionCOM(space);
            translator = new AtomActionTranslateBy(space);
            moleculeTranslator = new AtomGroupAction(translator);
            this.box = box;
            neighborCellManager = manager;
        }
        
        public void actionPerformed(MCMoveEvent evt) {
            if (evt instanceof MCMoveTrialCompletedEvent && ((MCMoveTrialCompletedEvent)evt).isAccepted()) {
                return;
            }
            MCMoveBox move = (MCMoveBox)evt.getMCMove();
            AtomIterator iterator = move.affectedAtoms();
            iterator.reset();
            for (IAtom atom = iterator.nextAtom(); atom != null; atom = iterator.nextAtom()) {
                updateCell(atom);
                if (atom instanceof IMolecule) {
                    IAtomSet childList = ((IMolecule)atom).getChildList();
                    for (int iChild = 0; iChild < childList.getAtomCount(); iChild++) {
                        updateCell(childList.getAtom(iChild));
                    }
                }
            }
        }

        private void updateCell(IAtom atom) {
            if (atom.getType().isInteracting()) {
                IBoundary boundary = box.getBoundary();
                Cell cell = neighborCellManager.getCell(atom);
                // we only need to remove the atom from the cell's list and
                // not de-associate the atom from the cell.  assignCell below
                // will do that.
                cell.removeAtom(atom);
                if (atom instanceof IMolecule) {
                    IVector shift = boundary.centralImage(moleculePosition.position(atom));
                    if (!shift.isZero()) {
                        translator.setTranslationVector(shift);
                        moleculeTranslator.actionPerformed(atom);
                    }
                    neighborCellManager.assignCell((IMolecule)atom);
                }
                else {
                    boundary.nearestImage(((IAtomPositioned)atom).getPosition());
                    neighborCellManager.assignCell((IAtomLeaf)atom);
                }
            }
        }
        
        private static final long serialVersionUID = 1L;
        private final IAtomPositionDefinition moleculePosition;
        private final AtomActionTranslateBy translator;
        private final AtomGroupAction moleculeTranslator;
        private final IBox box;
        private final NeighborCellManager neighborCellManager;
    }
}
