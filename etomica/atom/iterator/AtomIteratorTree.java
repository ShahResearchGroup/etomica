package etomica.atom.iterator;

import etomica.Atom;
import etomica.AtomIterator;
import etomica.AtomSet;
import etomica.AtomTreeNode;
import etomica.AtomTreeNodeGroup;
import etomica.Phase;
import etomica.Simulation;
import etomica.Species;
import etomica.SpeciesSpheres;
import etomica.SpeciesSpheresMono;
import etomica.action.AtomsetAction;
import etomica.action.AtomsetCount;
import etomica.action.AtomsetDetect;
import etomica.atom.AtomList;

/**
 * Atom iterator that traverses all atoms at a specified depth below a
 * specified root atom in the atom tree hierarchy.  The depth may be
 * specified as any non-negative integer.  If the bottom of the hierarchy
 * is reached before the specified depth, the leaf atoms encountered
 * there are the iterates.
 *
 * @author David Kofke
 * 02.02.16
 */
 
 /* History of changes
  * 07/26/02 started history of changes
  * 07/26/02 (DAK/DW) fixed error in setBasis that left iterator null if given atom is null.
  * 08/24/02 (DAK) revised with overhaul of iterators
  */
  
public class AtomIteratorTree implements AtomIterator {
    
	/**
	 * Default gives a leaf-atom iterator.  Must set a root node and
	 * reset before using.
	 */
    public AtomIteratorTree() {
    	this(Integer.MAX_VALUE);
    }

    /**
     * Constructs iterator that will iterate over atoms at the given depth below
     * a (to-be-specified) root node.  Iterates atoms only at the given level, and
     * not those above it (doAllNodes = false by default).  Must set a root node
     * and reset before using.
     * @param d depth in tree for iteration.
     */
	public AtomIteratorTree(int d) {
		setIterationDepth(d);
        setRoot(null);
        detector = new AtomsetDetect(null);
        counter = new AtomsetCount();
	}

    /**
     * Performs action on all iterates.  Clobbers iteration state.
     */
    public void allAtoms(AtomsetAction act) {
        if(rootNode == null) return;
		if(doAllNodes || iterationDepth == 0) {
            act.actionPerformed(rootNode.atom());
            if (iterationDepth == 0) return;
        }
		listIterator.reset();
		while(listIterator.hasNext()) {
            Atom atom = listIterator.nextAtom();
            if (atom.node.isLeaf() || iterationDepth == 1) {
                act.actionPerformed(atom);
            }
            else {
                if (treeIterator == null) {
                    treeIterator = new AtomIteratorTree(iterationDepth-1);
                    treeIterator.setDoAllNodes(doAllNodes);
                }
				treeIterator.setRoot(atom);
				treeIterator.allAtoms(act);
			}
		}
    	unset();
    }

    /**
     * Indicates if the iterate has another atom to give.
     */
    public boolean hasNext() {return next != null;}
    
    
    /**
     * Puts iterator in state in which hasNext is false.
     */
    public void unset() {next = null;}
    
    /**
     * Returns true if the given atom is among the iterates as
     * currently configured.  Clobbers iteration state.
     */
    public boolean contains(AtomSet atoms) {
    	unset();
    	if(atoms == null || atoms.count() != 1) return false;
        if(doAllNodes && rootNode!=null && rootNode.atom()==atoms.getAtom(0)) return true;
        detector.setAtoms(atoms);
        detector.reset();
        allAtoms(detector);
        return detector.detectedAtom();
    }
    
    
    /**
     * Reinitializes the iterator according to the most recently specified basis
     * and iteration depth.
     */
    public void reset() {
        if(rootNode == null) {
            unset();
            return;
        }
        listIterator.reset();
        if (treeIterator != null) treeIterator.unset();
        next = rootNode.atom();
        if(!doAllNodes && iterationDepth>0 && !rootNode.isLeaf()) nextAtom();
    }

    /**
     * Returns the next atom in the iteration sequence.
     */
    public Atom nextAtom() {
    	if(next == null) return null;
        Atom nextAtom = next;
        next = null;
        if (treeIterator != null && treeIterator.hasNext()) {
            next = treeIterator.nextAtom();
            return nextAtom;
        }
        while(listIterator.hasNext()) {
            Atom atom = listIterator.nextAtom();
            if (atom.node.isLeaf() || iterationDepth == 1) {
                next = atom;
                break;
            }
            if (treeIterator == null) {
                treeIterator = new AtomIteratorTree(iterationDepth-1);
                treeIterator.setDoAllNodes(doAllNodes);
            }
            treeIterator.setRoot(atom); 
            treeIterator.reset();
            if(treeIterator.hasNext()) {
                next = treeIterator.nextAtom();
                break;
            }
        }
        return nextAtom;
    }
    
    public AtomSet next() {
    	return nextAtom();
    }
    
    public AtomSet peek() {
    	return next;
    }
        
    /**
     * Defines the root of the tree under which the iteration is performed.
     * If atom is null, hasNext will report false.
     * User must perform a subsequent call to reset() before beginning iteration.
     */
    public void setRoot(Atom rootAtom) {
        if(rootAtom == null) {
        	rootNode = null;
        } else if(iterationDepth == 0 || rootAtom.node.isLeaf()) {//singlet iteration of basis atom
            if (!wealreadyknowyourstupid) {
                System.err.println("don't use AtomIteratorTree as a singlet iterator.");
                wealreadyknowyourstupid = true;
            }
            rootNode = rootAtom.node;
            listIterator.setList(emptyList);
        } else {
	        rootNode = rootAtom.node;
	        listIterator.setList(((AtomTreeNodeGroup)rootNode).childList);
        }
        unset();
    }
        
    /**
     * Returns the number of iterates given by a full cycle of this iterator.
     */
    public int size() {
        if(rootNode == null) return 0;
    	unset();
        counter.reset();
        allAtoms(counter);
    	return counter.callCount();
    }
    
    public final int nBody() {return 1;}
    
    /**
     * Sets the depth below the current basis for which the iteration will occur.
     * Any non-negative value is permitted.  A value of zero causes singlet iteration
     * returning just the root atom. A value of 1 returns all children of the basis
     * atom, a value of 2 returns all children of all children of the basis, etc.
     * Iterator returns only atoms at the specified level, and not those above it.
     * Returns atoms at bottom of hierarchy (i.e., leafs) if specified depth
     * exceeds depth of hierarchy.  Default is Integer.MAX_VALUE, which causes
     * all leaf atoms to be iterated.
     */
    public void setIterationDepth(int depth) {
        if (iterationDepth == depth) return;
        if(depth < 0) throw new IllegalArgumentException("Error: attempt to set iteration depth to negative value in AtomIteratorTree");
        iterationDepth = depth;
        if(rootNode != null) setRoot(rootNode.atom());
        unset();
    }
    /**
     * Returns the currently set value of iteration depth.
     */
    public int getIterationDepth() {return iterationDepth;}
    
    /**
     * Sets iterator to iterate over all leaf atoms below the basis atom.
     * Equivalent to setDoAllNodes(false) and setIterationDepth(Integer.MAX_VALUE)
     */
    public void setAsLeafIterator() {
        setDoAllNodes(false);
        setIterationDepth(Integer.MAX_VALUE);
    }
    
	public boolean isDoAllNodes() {
		return doAllNodes;
	}
	public void setDoAllNodes(boolean doAllNodes) {
		this.doAllNodes = doAllNodes;
		if(treeIterator != null) treeIterator.setDoAllNodes(doAllNodes);
		unset();
	}

            
    private AtomTreeNode rootNode;
    private AtomIteratorTree treeIterator;//used for recursive iteration to lower levels in tree
    private final AtomIteratorListSimple listIterator = new AtomIteratorListSimple();
    private int iterationDepth = Integer.MAX_VALUE;
    private Atom next;
    private boolean doAllNodes = false;
    private boolean wealreadyknowyourstupid = false;
    private final AtomsetDetect detector;
    private final AtomsetCount counter;
    private final AtomList emptyList = new AtomList();
    
    /**
     * main method to test and demonstrate use of this class.
     */
    public static void main(String args[]) {
        
        Simulation sim = new Simulation();
        Species species2 = new SpeciesSpheresMono(sim);
        SpeciesSpheres species1 = new SpeciesSpheres(sim);
        SpeciesSpheres species0 = new SpeciesSpheres(sim);
        species0.setNMolecules(3);
        species1.setNMolecules(2);
        species2.setNMolecules(2);
        species1.setAtomsPerMolecule(3);
        species0.setAtomsPerMolecule(2);
        Phase phase = new Phase(sim);
//        sim.elementCoordinator.go();
        
        int k = 0;
        AtomIteratorTree treeIterator = new AtomIteratorTree();
 //       treeIterator.setIterationDepth(2);
        treeIterator.setRoot(phase.speciesMaster);
        treeIterator.reset(); k = 0;
        while(treeIterator.hasNext()) System.out.println(k++ + "  " + treeIterator.next().toString());
        System.out.println(treeIterator.size() + "  " + (treeIterator.size() == k));
        System.out.println();
        
        treeIterator.setIterationDepth(2);
        treeIterator.reset(); k = 0;
        while(treeIterator.hasNext()) System.out.println(k++ + "  " + treeIterator.next().toString());
        System.out.println(treeIterator.size() + "  " + (treeIterator.size() == k));
        System.out.println();
        
        treeIterator.setIterationDepth(1);
        treeIterator.reset(); k = 0;
        while(treeIterator.hasNext()) System.out.println(k++ + "  " + treeIterator.next().toString());
        System.out.println(treeIterator.size() + "  " + (treeIterator.size() == k));
        System.out.println();
        
        treeIterator.setIterationDepth(0);
        treeIterator.reset(); k = 0;
        while(treeIterator.hasNext()) System.out.println(k++ + "  " + treeIterator.next().toString());
        System.out.println(treeIterator.size() + "  " + (treeIterator.size() == k));
        System.out.println();
        
        treeIterator.setRoot(phase.speciesMaster.node.childList.getFirst());
        treeIterator.setAsLeafIterator();
        treeIterator.setIterationDepth(1);
        treeIterator.reset(); k = 0;
        while(treeIterator.hasNext()) System.out.println(k++ + "  " + treeIterator.next().toString());
        System.out.println(treeIterator.size() + "  " + (treeIterator.size() == k));
        System.out.println();
        
        System.out.print("null-basis test ");
        treeIterator.setRoot(null);
        treeIterator.setAsLeafIterator();
        treeIterator.reset();
        while(treeIterator.hasNext()) System.out.println(k++ + "  " + treeIterator.next().toString());
        System.out.println("ok");
        
    }//end of main
    
}//end of AtomIteratorTree
