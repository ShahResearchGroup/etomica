package etomica;

import etomica.atom.AtomLinker;
import etomica.atom.AtomList;
import etomica.atom.AtomSequencerFactory;
import etomica.atom.AtomTreeNode;
import etomica.atom.AtomTreeNodeFactory;
import etomica.atom.AtomTreeNodeGroup;
import etomica.atom.AtomTreeNodeLeaf;
import etomica.atom.AtomLinker.Tab;
import etomica.atom.iterator.AtomIteratorList;
import etomica.atom.iterator.AtomIteratorTree;

/**
 * Coordinator of all species agents in a phase.  
 *
 * @author David Kofke
 */
 
public final class SpeciesMaster extends Atom {
    
    final AtomType childType;
    private int moleculeCount;
    //manager and events for addition/removal of descendant atoms
    private final SimulationEventManager eventManager = new SimulationEventManager();
    private final PhaseEvent additionEvent = new PhaseEvent(this, PhaseEvent.ATOM_ADDED);
    private final PhaseEvent removalEvent = new PhaseEvent(this, PhaseEvent.ATOM_REMOVED);
    public final AtomTreeNodeGroup node;//shadow superclass field of same name to avoid casts
    public final static int SPECIES_TAB = Tab.requestTabType();
    //reference to phase is kept in node
    
    /**
     * List of leaf atoms in phase, suitable for iteration via an AtomIteratorList.
     */
    public final AtomList atomList = new AtomList();

    SpeciesMaster(Simulation sim, Phase p) {
        super(sim.space, sim.speciesRoot.childType, new NodeFactory(p), AtomSequencerFactory.SIMPLE);
        node = (AtomTreeNodeGroup)super.node;
        childType = new AtomType(type.getIndexManager().makeChildManager());
    }
    
    public SpeciesAgent firstSpecies() {return (SpeciesAgent)node.childList.getFirst();}
    public SpeciesAgent lastSpecies() {return (SpeciesAgent)node.childList.getLast();}
        
    
//    public int atomCount() {return atomList.size();}//or could use node.leafAtomCount()
    public int moleculeCount() {
    	return moleculeCount;
    }
    
    public String signature() {return node.parentPhase().getName();}
    
    
    //event management
    public synchronized void addListener(PhaseListener listener) {
        eventManager.addListener(listener);
    }

    public synchronized void removeListener(PhaseListener listener) {
        eventManager.removeListener(listener);
    }
    
    private static final class MasterAtomTreeNode extends AtomTreeNodeGroup {
        
        MasterAtomTreeNode(Phase parentPhase, Atom atom) {
            super(atom);
            speciesMaster = (SpeciesMaster)atom;
            this.parentPhase = parentPhase;
            leafIterator.setAsLeafIterator();
        }
        public Phase parentPhase() {return parentPhase;}
        
        public Species parentSpecies() {
            throw new RuntimeException("Error:  Unexpected call to parentSpecies in SpeciesMaster");
        }
        public SpeciesAgent parentSpeciesAgent() {
            throw new RuntimeException("Error:  Unexpected call to parentSpeciesAgent in SpeciesMaster");
        }
        /**
         * Throws a RuntimeException, because a species master is not contained within a molecule.
         */
        public final Atom parentMolecule() {
            throw new RuntimeException("Error:  Unexpected call to parentMolecule in SpeciesMaster");
        }
        
        /**
         * Returns true, because children are SpeciesAgent instances.
         */
        public final boolean childrenAreGroups() {return true;}
        
        //does not pass notification up to parent (root)
        public void addAtomNotify(Atom atom) {
        	if(atom.node.parentGroup() instanceof SpeciesAgent) {
            	speciesMaster.moleculeCount++;
            }
            else if(atom instanceof SpeciesAgent) {
            	speciesMaster.moleculeCount += ((SpeciesAgent)atom).moleculeCount();
            	AtomLinker.Tab newTab = AtomLinker.Tab.newTab(speciesMaster.atomList, SPECIES_TAB);
            	speciesMaster.atomList.add(newTab);
            	((SpeciesAgent)atom).firstLeafAtomTab = newTab;
            }
        	AtomLinker.Tab nextTab = atom.node.parentSpeciesAgent().firstLeafAtomTab.nextTab;
        	
            leafAtomCount += atom.node.leafAtomCount();
            if (atom.node.isLeaf()) {
                speciesMaster.atomList.addBefore(((AtomTreeNodeLeaf)atom.node).leafLinker, nextTab);
            }
            else {
                leafIterator.setRoot(atom);
                leafIterator.reset();
                while(leafIterator.hasNext()) {
                    speciesMaster.atomList.addBefore(((AtomTreeNodeLeaf)leafIterator.nextAtom().node).leafLinker, nextTab);
                }
            }
            speciesMaster.eventManager.fireEvent(speciesMaster.additionEvent.setAtom(atom));
        }

        //updating of leaf atomList may not be efficient enough for repeated use, but is probably ok
        public void removeAtomNotify(Atom atom) {
            if(atom.node.parentGroup() instanceof SpeciesAgent) {speciesMaster.moleculeCount--;}
            else if(atom instanceof SpeciesAgent) {speciesMaster.moleculeCount -= ((SpeciesAgent)atom).moleculeCount();}
            leafAtomCount -= atom.node.leafAtomCount();
            leafIterator.setRoot(atom);
            leafIterator.reset();
            while(leafIterator.hasNext()) {
                speciesMaster.atomList.remove(((AtomTreeNodeLeaf)leafIterator.nextAtom().node).leafLinker);
            }
            speciesMaster.eventManager.fireEvent(speciesMaster.removalEvent.setAtom(atom));
        }
        
        private final Phase parentPhase;
        private final SpeciesMaster speciesMaster;
        private final AtomIteratorTree leafIterator = new AtomIteratorTree();
    } //end of MasterAtomTreeNode           
    
    private static final class NodeFactory implements AtomTreeNodeFactory {
        Phase phase;
        NodeFactory(Phase p) {
            phase = p;
        }
        public AtomTreeNode makeNode(Atom atom) {
            return new MasterAtomTreeNode(phase, atom);
        }
    }


    /**
     * non-graphic main method to test handling of leaf atom list.
     */
    public static void main(String args[]) {
        
        Simulation sim = new Simulation();
        Simulation.instance = sim;
        AtomSequencerFactory seqFactory = sim.potentialMaster.sequencerFactory();
        Species species2 = new SpeciesSpheresMono(sim);
        Species species1 = new SpeciesSpheres(sim,seqFactory,3);
        Species species0 = new SpeciesSpheres(sim,seqFactory,2);
        species0.setNMolecules(4);
        species1.setNMolecules(2);
        species2.setNMolecules(2);
        Phase phase = new Phase(sim);
//        sim.elementCoordinator.go();
        
        AtomIteratorList listIterator = new AtomIteratorList();
        listIterator.setList(phase.speciesMaster.atomList);
        listIterator.reset();
        while(listIterator.hasNext()) System.out.println(listIterator.next().toString());
        System.out.println();
    }//end of main

}//end of SpeciesMaster