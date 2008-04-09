package etomica.nbr.site;

import etomica.api.IAtom;
import etomica.api.IAtomLeaf;
import etomica.api.IAtomSet;
import etomica.api.IAtomType;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotential;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.atom.AtomSetSinglet;
import etomica.atom.iterator.AtomsetIteratorPDT;
import etomica.atom.iterator.IteratorDirective;
import etomica.box.BoxAgentManager;
import etomica.box.BoxAgentManager.BoxAgentSource;
import etomica.nbr.CriterionAll;
import etomica.nbr.CriterionInterMolecular;
import etomica.nbr.CriterionType;
import etomica.nbr.CriterionTypePair;
import etomica.nbr.CriterionTypesMulti;
import etomica.nbr.NeighborCriterion;
import etomica.nbr.PotentialGroupNbr;
import etomica.nbr.PotentialMasterNbr;
import etomica.potential.Potential2;
import etomica.potential.PotentialArray;
import etomica.potential.PotentialCalculation;
import etomica.space.ISpace;
import etomica.species.Species;
import etomica.util.Arrays;
import etomica.util.Debug;

public class PotentialMasterSite extends PotentialMasterNbr {

	/**
	 * Invokes superclass constructor, specifying IteratorFactoryCell
     * for generating molecule iterators.  Sets default nCells of 10 and
     * position definition to null, so that atom type's definition is used
     * to assign cells. 
	 */
	public PotentialMasterSite(ISimulation sim, int nCells, ISpace _space) {
        this(sim, new BoxAgentSiteManager(nCells, _space), _space);
    }
    
    public PotentialMasterSite(ISimulation sim,
    		                   BoxAgentSource boxAgentSource, ISpace _space) {
        this(sim, boxAgentSource, new BoxAgentManager(boxAgentSource), _space);
    }
    
    public PotentialMasterSite(ISimulation sim, BoxAgentSource boxAgentSource,
    		BoxAgentManager agentManager, ISpace _space) {
        this(sim, boxAgentSource, agentManager, new Api1ASite(_space.D(),agentManager), _space);
    }
    
    protected PotentialMasterSite(ISimulation sim, BoxAgentSource boxAgentSource, 
            BoxAgentManager agentManager, AtomsetIteratorPDT neighborIterator,
            ISpace _space) {
        super(sim, boxAgentSource, agentManager, _space);
        atomSetSinglet = new AtomSetSinglet();
        this.neighborIterator = neighborIterator;
	}
    
    /**
     * @return Returns the cellRange.
     */
    public int getCellRange() {
        return cellRange;
    }

    /**
     * @param cellRange The cellRange to set.
     */
    public void setCellRange(int newCellRange) {
        cellRange = newCellRange;
    }
    
    protected void addRangedPotentialForTypes(IPotential potential, IAtomType[] atomType) {
        NeighborCriterion criterion;
        if (atomType.length == 2) {
            criterion = new CriterionTypePair(new CriterionAll(), atomType[0], atomType[1]);
            if ((atomType[0] instanceof IAtomTypeLeaf) &&
                    (atomType[1] instanceof IAtomTypeLeaf)) {
                ISpecies moleculeType0 = ((IAtomTypeLeaf)atomType[0]).getSpecies();
                ISpecies moleculeType1 = ((IAtomTypeLeaf)atomType[1]).getSpecies();
                if (moleculeType0 == moleculeType1) {
                    criterion = new CriterionInterMolecular(criterion);
                }
            }
        }
        else if (atomType.length == 1) {
            criterion = new CriterionType(new CriterionAll(), atomType[0]);
        }
        else {
            criterion = new CriterionTypesMulti(new CriterionAll(), atomType);
        }
        for (int i=0; i<atomType.length; i++) {
            ((PotentialArray)rangedAgentManager.getAgent(atomType[i])).setCriterion(potential, criterion);
        }
        criteriaArray = (NeighborCriterion[]) Arrays.addObject(criteriaArray, criterion);
    }
    
    /**
     * Returns the criterion used by to determine what atoms interact with the
     * given potential.
     */
    public NeighborCriterion getCriterion(IPotential potential) {
        rangedPotentialIterator.reset();
        while (rangedPotentialIterator.hasNext()) {
            PotentialArray potentialArray = (PotentialArray)rangedPotentialIterator.next();
            IPotential[] potentials = potentialArray.getPotentials();
            for (int j=0; j<potentials.length; j++) {
                if (potentials[j] == potential) {
                    return potentialArray.getCriteria()[j];
                }
            }
        }
        return null;
    }
    
    /**
     * Sets the criterion associated with the given potential, overriding the 
     * default provided by the PotentialMasterCell.  The criterion can be 
     * configured by calling getCriterion(Potential) and changing the 
     * criterion.  The potential passed to this method must be a potential 
     * handled by this instance.
     */
    public void setCriterion(IPotential potential, NeighborCriterion criterion) {
        rangedPotentialIterator.reset();
        while (rangedPotentialIterator.hasNext()) {
            PotentialArray potentialArray = (PotentialArray)rangedPotentialIterator.next();
            IPotential[] potentials = potentialArray.getPotentials();
            for (int j=0; j<potentials.length; j++) {
                if (potentials[j] == potential) {
                    potentialArray.setCriterion(potential, criterion);
                    return;
                }
            }
        }
        throw new IllegalArgumentException("Potential "+potential+" is not associated with this PotentialMasterList");
    }
    
    /**
     * Overrides superclass method to enable direct neighbor-list iteration
     * instead of iteration via species/potential hierarchy. If no target atoms are
     * specified in directive, neighborlist iteration is begun with
     * speciesMaster of box, and repeated recursively down species hierarchy;
     * if one atom is specified, neighborlist iteration is performed on it and
     * down species hierarchy from it; if two or more atoms are specified,
     * superclass method is invoked.
     */
    public void calculate(IBox box, IteratorDirective id, PotentialCalculation pc) {
        if (!enabled)
            return;
        for (int i=0; i<criteriaArray.length; i++) {
            criteriaArray[i].setBox(box);
        }
        IAtom targetAtom = id.getTargetAtom();
        neighborIterator.setBox(box);
        if (targetAtom == null) {
            if (Debug.ON && id.direction() != IteratorDirective.Direction.UP) {
                throw new IllegalArgumentException("When there is no target, iterator directive must be up");
            }
            neighborIterator.setDirection(IteratorDirective.Direction.UP);
            // invoke setBox on all potentials
            for (int i=0; i<allPotentials.length; i++) {
                allPotentials[i].setBox(box);
            }

            //no target atoms specified
            //call calculate with each SpeciesAgent
            ISpecies[] species = simulation.getSpeciesManager().getSpecies();
            for (int j=0; j<species.length; j++) {
                IAtomSet list = box.getMoleculeList();
                int size = list.getAtomCount();
                PotentialArray potentialArray = (PotentialArray)rangedAgentManager.getAgent(species[j]);
                IPotential[] potentials = potentialArray.getPotentials();
                NeighborCriterion[] criteria = potentialArray.getCriteria();
                PotentialArray intraPotentialArray = getIntraPotentials(species[j]);
                final IPotential[] intraPotentials = intraPotentialArray.getPotentials();
                for (int i=0; i<size; i++) {
                    IMolecule molecule = (IMolecule)list.getAtom(i);
                    calculate(molecule, potentials, criteria, pc);//call calculate with the SpeciesAgent

                    for(int k=0; k<potentials.length; k++) {
                        ((PotentialGroupNbr)intraPotentials[k]).calculateRangeIndependent(molecule,id,pc);
                    }
                }
            }
        }
        else {
            // one target atom
            neighborIterator.setDirection(id.direction());
            PotentialArray potentialArray = (PotentialArray)rangedAgentManager.getAgent(targetAtom.getType());
            IPotential[] potentials = potentialArray.getPotentials();
            for(int i=0; i<potentials.length; i++) {
                potentials[i].setBox(box);
            }
            if (targetAtom instanceof IAtomLeaf) {
                //walk up the tree looking for 1-body range-independent potentials that apply to parents
                IMolecule parentMolecule = ((IAtomLeaf)targetAtom).getParentGroup();
                potentialArray = getIntraPotentials((ISpecies)parentMolecule.getType());
                potentials = potentialArray.getPotentials();
                for(int i=0; i<potentials.length; i++) {
                    potentials[i].setBox(box);
                    ((PotentialGroupNbr)potentials[i]).calculateRangeIndependent(parentMolecule,id,pc);
                }
                calculate((IAtomLeaf)targetAtom, pc);
            }
            else {
                potentialArray = (PotentialArray)rangedAgentManager.getAgent(targetAtom.getType());
                potentials = potentialArray.getPotentials();
                NeighborCriterion[] criteria = potentialArray.getCriteria();
                calculate((IMolecule)targetAtom, potentials, criteria, pc);

                PotentialArray intraPotentialArray = getIntraPotentials((ISpecies)targetAtom.getType());
                final IPotential[] intraPotentials = intraPotentialArray.getPotentials();
                for(int k=0; k<potentials.length; k++) {
                    ((PotentialGroupNbr)intraPotentials[k]).calculateRangeIndependent((IMolecule)targetAtom,id,pc);
                }
            }
        }
        if (lrcMaster != null) {
            lrcMaster.calculate(box, id, pc);
        }
    }
	
    /**
     * Performs given PotentialCalculation using potentials/neighbors associated
     * with the given atom (if any).  Then, if atom is not a leaf atom, iteration over
     * child atoms is performed and process is repeated (recursively) with each on down
     * the hierarchy until leaf atoms are reached.
     */
    //TODO make a "TerminalGroup" indicator in type that permits child atoms but indicates that no potentials apply directly to them
	protected void calculate(IMolecule atom, final IPotential[] potentials, final NeighborCriterion[] criteria,
	        PotentialCalculation pc) {

        for(int i=0; i<potentials.length; i++) {
            switch (potentials[i].nBody()) {
            case 1:
                atomSetSinglet.atom = atom;
                pc.doCalculation(atomSetSinglet, potentials[i]);
                break;
            case 2:
                Potential2 p2 = (Potential2) potentials[i];
                NeighborCriterion nbrCriterion = criteria[i];
                neighborIterator.setTarget(atom);
                neighborIterator.reset();
                for (IAtomSet pair = neighborIterator.next(); pair != null;
                     pair = neighborIterator.next()) {
                    if (nbrCriterion.accept(pair)) {
                        pc.doCalculation(pair, p2);
                    }
                }
                break;
            }
        }
            
        //cannot use AtomIterator field because of recursive call
        IAtomSet list = atom.getChildList();
        int size = list.getAtomCount();
        for (int i=0; i<size; i++) {
            calculate((IAtomLeaf)list.getAtom(i), pc);//recursive call
        }
	}
    
    /**
     * Performs given PotentialCalculation using potentials/neighbors associated
     * with the given atom (if any).  Then, if atom is not a leaf atom, iteration over
     * child atoms is performed and process is repeated (recursively) with each on down
     * the hierarchy until leaf atoms are reached.
     */
    protected void calculate(IAtomLeaf atom, PotentialCalculation pc) {
        PotentialArray potentialArray = (PotentialArray)rangedAgentManager.getAgent(atom.getType());
        IPotential[] potentials = potentialArray.getPotentials();
        NeighborCriterion[] criteria = potentialArray.getCriteria();

        for(int i=0; i<potentials.length; i++) {
            switch (potentials[i].nBody()) {
            case 1:
                atomSetSinglet.atom = atom;
                pc.doCalculation(atomSetSinglet, potentials[i]);
                break;
            case 2:
                Potential2 p2 = (Potential2) potentials[i];
                NeighborCriterion nbrCriterion = criteria[i];
                neighborIterator.setTarget(atom);
                neighborIterator.reset();
                for (IAtomSet pair = neighborIterator.next(); pair != null;
                     pair = neighborIterator.next()) {
                    if (nbrCriterion.accept(pair)) {
                        pc.doCalculation(pair, p2);
                    }
                }
                break;
            }
        }
    }
    
    private static final long serialVersionUID = 1L;
	protected final AtomSetSinglet atomSetSinglet;
    private int cellRange;
    protected final AtomsetIteratorPDT neighborIterator;
    private NeighborCriterion[] criteriaArray = new NeighborCriterion[0];
    
    public static class BoxAgentSiteManager implements BoxAgentSource {
        public BoxAgentSiteManager(int nCells, ISpace _space) {
            this.nCells = nCells;
            this.space = _space;
        }
        
        public Class getAgentClass() {
            return NeighborSiteManager.class;
        }
        
        public Object makeAgent(IBox box) {
            return new NeighborSiteManager(box,nCells, space);
        }
        
        public void releaseAgent(Object agent) {
        }
        
        private final int nCells;
        private final ISpace space;
    }
}
