/*
 * History
 * Created on Nov 21, 2004 by kofke
 */
package etomica.nbr.cell;

import etomica.action.AtomActionTranslateBy;
import etomica.action.AtomGroupAction;
import etomica.atom.Atom;
import etomica.atom.AtomAgentManager;
import etomica.atom.AtomLeaf;
import etomica.atom.AtomPositionCOM;
import etomica.atom.AtomPositionDefinition;
import etomica.atom.AtomAgentManager.AgentSource;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorTree;
import etomica.integrator.mcmove.MCMoveEvent;
import etomica.integrator.mcmove.MCMoveListener;
import etomica.integrator.mcmove.MCMovePhase;
import etomica.integrator.mcmove.MCMoveTrialCompletedEvent;
import etomica.lattice.CellLattice;
import etomica.phase.Phase;
import etomica.phase.PhaseCellManager;
import etomica.phase.PhaseEvent;
import etomica.phase.PhaseInflateEvent;
import etomica.phase.PhaseListener;
import etomica.space.Boundary;
import etomica.space.IVector;
import etomica.space.Space;
import etomica.util.Debug;

/**
 * Class that defines and manages construction and use of lattice of cells 
 * for cell-based neighbor listing.
 */

//TODO modify assignCellAll to loop through cells to get all atoms to be assigned
//no need for index when assigning cell
//different iterator needed

public class NeighborCellManager implements PhaseCellManager, AgentSource, PhaseListener, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    private final CellLattice lattice;
    private final Space space;
    private final AtomIteratorTree atomIterator;
    private final AtomPositionDefinition positionDefinition;
    private final Phase phase;
    private int cellRange = 2;
    private double range;
    private final AtomAgentManagerCell agentManager;
    
    /**
     * Constructs manager for neighbor cells in the given phase.  The number of
     * cells in each dimension is given by nCells. Position definition for each
     * atom is that given by its type (it is set to null in this class).
     */
    public NeighborCellManager(Phase phase, double potentialRange) {
        this(phase, potentialRange, null);
    }
    
    /**
     * Construct manager for neighbor cells in the given phase.  The number
     * of cells in each dimension is given by nCells.  Position definition is
     * used to determine the cell a given atom is in; if null, the position
     * definition given by the atom's type is used.  Position definition is
     * declared final.
     */
    public NeighborCellManager(final Phase phase, double potentialRange, AtomPositionDefinition positionDefinition) {
        this.positionDefinition = positionDefinition;
        this.phase = phase;
        space = phase.getSpace();
        atomIterator = new AtomIteratorTree();
        atomIterator.setDoAllNodes(true);
        atomIterator.setRoot(phase.getSpeciesMaster());

        lattice = new CellLattice(phase.getBoundary().getDimensions(), Cell.FACTORY);
        setPotentialRange(potentialRange);

        //force lattice to be sized so the Cells will exist
        checkDimensions();
        agentManager = new AtomAgentManagerCell(this,phase);
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
        checkDimensions();
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
     * Checks the phase's dimensions to make sure the number of cells is 
     * appropriate.
     */
    protected void checkDimensions() {
        if (range == 0) {
            // simulation is still being constructed, don't try to do anything useful
            return;
        }
    	int D = space.D();
        int[] nCells = new int[D];
        IVector dimensions = phase.getBoundary().getDimensions();
        lattice.setDimensions(dimensions);
        for (int i=0; i<D; i++) {
            nCells[i] = (int)Math.floor(cellRange*dimensions.x(i)/range);
        }
        //only update the lattice (expensive) if the number of cells changed
        int[] oldSize = lattice.getSize();
        for (int i=0; i<D; i++) {
            if (oldSize[i] != nCells[i]) {
                lattice.setSize(nCells);
                break;
            }
        }
    }
    
    /**
     * Assigns cells to all interacting atoms in the phase.  Interacting atoms
     * are those that have one or more potentials that act on them.  
     */
    public void assignCellAll() {
        // ensure that any changes to cellRange, potentialRange and boundary
        // dimension take effect
        checkDimensions();

        Object[] allCells = lattice.sites();
        for (int i=0; i<allCells.length; i++) {
            ((Cell)allCells[i]).occupants().clear();
        }
        
        Cell[] cells = agentManager.getAgents();
        for (int i=0; i<cells.length; i++) {
            cells[i] = null;
        }
        
        atomIterator.reset();
        while(atomIterator.hasNext()) {
            Atom atom = atomIterator.nextAtom();
            if (atom.getType().isInteracting()) {
                assignCell(atom);
            }
        }
    }
    
    public Cell getCell(Atom atom) {
        return (Cell)agentManager.getAgent(atom);
    }
    
    protected void removeFromCell(Atom atom) {
        Cell[] cells = agentManager.getAgents();
        if (cells[atom.getGlobalIndex()] != null) {
            cells[atom.getGlobalIndex()].removeAtom(atom);
            cells[atom.getGlobalIndex()] = null;
        }
    }
    
    /**
     * Assigns the cell for the given atom.
     * @param atom
     */
    public void assignCell(Atom atom) {
        IVector position = (positionDefinition != null) ?
                positionDefinition.position(atom) :
                    atom.getType().getPositionDefinition().position(atom);
        Cell atomCell = (Cell)lattice.site(position);
        atomCell.addAtom(atom);
        Cell[] cells = agentManager.getAgents();
        cells[atom.getGlobalIndex()] = atomCell;
    }
    
    public MCMoveListener makeMCMoveListener() {
        return new MyMCMoveListener(space,phase,this);
    }
    
    public Class getAgentClass() {
        return Cell.class;
    }
    
    public Object makeAgent(Atom atom) {
        if (atom.getType().isInteracting()) {
            IVector position = (positionDefinition != null) ?
                    positionDefinition.position(atom) :
                        atom.getType().getPositionDefinition().position(atom);
            Cell atomCell = (Cell)lattice.site(position);
            atomCell.addAtom(atom);
            if (Debug.ON && Debug.DEBUG_NOW && Debug.anyAtom(atom)) {
                System.out.println("assigning new "+atom+" "+atom.getGlobalIndex()+" at "+position+" to "+atomCell);
            }
            return atomCell;
        }
        return null;
    }

    public void releaseAgent(Object agent, Atom atom) {
        removeFromCell(atom);
    }
    
    public void actionPerformed(PhaseEvent event) {
        if (event instanceof PhaseInflateEvent) {
            lattice.setDimensions(phase.getBoundary().getDimensions());
        }
    }
    
    private static class MyMCMoveListener implements MCMoveListener, java.io.Serializable {
        public MyMCMoveListener(Space space, Phase phase, NeighborCellManager manager) {
            treeIterator = new AtomIteratorTree();
            treeIterator.setDoAllNodes(true);
            moleculePosition = new AtomPositionCOM(space);
            translator = new AtomActionTranslateBy(space);
            moleculeTranslator = new AtomGroupAction(translator);
            this.phase = phase;
            neighborCellManager = manager;
        }
        
        public void actionPerformed(MCMoveEvent evt) {
            if (evt instanceof MCMoveTrialCompletedEvent && ((MCMoveTrialCompletedEvent)evt).isAccepted()) {
                return;
            }
            MCMovePhase move = (MCMovePhase)evt.getMCMove();
            AtomIterator iterator = move.affectedAtoms();
            iterator.reset();
            while (iterator.hasNext()) {
                Atom atom = iterator.nextAtom();
                if (!atom.getNode().isLeaf()) {
                    treeIterator.setRoot(atom);
                    treeIterator.reset();
                    while (treeIterator.hasNext()) {
                        Atom childAtom = treeIterator.nextAtom();
                        updateCell(childAtom);
                    }
                }
                else {
                    updateCell(atom);
                }
            }
        }

        private void updateCell(Atom atom) {
            if (atom.getType().isInteracting()) {
                Boundary boundary = phase.getBoundary();
                neighborCellManager.removeFromCell(atom);
                if (!atom.getNode().isLeaf()) {
                    IVector shift = boundary.centralImage(moleculePosition.position(atom));
                    if (!shift.isZero()) {
                        translator.setTranslationVector(shift);
                        moleculeTranslator.actionPerformed(atom);
                    }
                }
                else {
                    boundary.nearestImage(((AtomLeaf)atom).getCoord().getPosition());
                }
                neighborCellManager.assignCell(atom);
            }
        }
        
        private static final long serialVersionUID = 1L;
        private final AtomIteratorTree treeIterator;
        private final AtomPositionDefinition moleculePosition;
        private final AtomActionTranslateBy translator;
        private final AtomGroupAction moleculeTranslator;
        private final Phase phase;
        private final NeighborCellManager neighborCellManager;
    }
    
    /**
     * Inner class to let us cheat and access and modify elements of the agents array.
     */
    protected static class AtomAgentManagerCell extends AtomAgentManager {

        public AtomAgentManagerCell(NeighborCellManager neighborCellManager, Phase phase) {
            super(neighborCellManager, phase, true);
        }

        /**
         * Returns the array of Cells for the Phase, indexed by the Atom's
         * global index.
         */
        protected Cell[] getAgents() {
            return (Cell[])agents;
        }

        private static final long serialVersionUID = 1L;
    }
}
