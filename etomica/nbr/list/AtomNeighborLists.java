/*
 * History
 * Created on Sep 20, 2004 by kofke
 */
package etomica.nbr.list;

import java.io.IOException;

import etomica.atom.Atom;
import etomica.atom.AtomArrayList;
import etomica.util.DirtyObject;
import etomica.util.EtomicaObjectInputStream;

/**
 * Class used to maintain neighbor lists.  Holds lists of atoms that were 
 * elsewhere deemed to be neighbors of the another atom.  A separate 
 * AtomArrayList is kept for each potential (potential => AtomArrayList mapping
 * is the responsibility of the consumer). 
 */
public class AtomNeighborLists implements DirtyObject, java.io.Serializable {

    private static final long serialVersionUID = 1L;
    protected transient AtomArrayList[] upList, downList;
	
    /**
     * Constructs sequencer for the given atom.
     */
    public AtomNeighborLists() {
        upList = new AtomArrayList[0];
        downList = new AtomArrayList[0];
    }
    
    /**
     * Adds the given atom as a "down" neighbor interacting via the potential 
     * with the given index.  
     * @param a the new downlist neighbor atom
     * @param index the of the potential between the atoms
     */
    public void addUpNbr(Atom a, int index) {
        upList[index].add(a);
    }

    /**
     * Adds the given atom as a "down" neighbor interacting via the potential 
     * with the given index.  
     * @param a the new downlist neighbor atom
     * @param index the of the potential between the atoms
     */
    public void addDownNbr(Atom a, int index) {
        downList[index].add(a);
    }

    /**
     * Returns an array of uplist-neighbor-atom lists.  Each list in the
     * array corresponds to a specific potential. A zero-length list indicates
     * that no concrete potentials apply to the atom.
     */
    public AtomArrayList[] getUpList() {
        return upList;
    }
	
    /**
     * Returns an array of downlist-neighbor-atom lists.  Each list in the
     * array corresponds to a specific potential. A zero-length list indicates
     * that no concrete potentials apply to the atom.
     */
    public AtomArrayList[] getDownList() {
        return downList;
    }
    
    /**
     * Sets the number of up and down lists maintained by this instance.
     */
    protected void setCapacity(int newCapacity) {
        if (newCapacity == upList.length) {
            return;
        }
        upList = new AtomArrayList[newCapacity];
        downList = new AtomArrayList[newCapacity];
        for (int i=0; i<newCapacity; i++) {
            upList[i] = new AtomArrayList();
            downList[i] = new AtomArrayList();
        }
    }
	
    /**
     * Clears neighbor lists, removing all listed neighbor atoms.
     */
	public void clearNbrs() {
		int length = upList.length;
		for (int i=0; i<length; i++) {
			upList[i].clear();
			downList[i].clear();
		}
	}

    /**
     * Write out neighbor lists as arrays of Atom indices.  Include
     * the SpeciesMaster so that rebuild() can find the Atoms corresponding
     * to those indices.
     */
    private void writeObject(java.io.ObjectOutputStream out)
    throws IOException
    {
        // write nothing.
        out.defaultWriteObject();
        
        int[][][] atomListInts = new int[2][][];
        atomListInts[0] = new int[upList.length][];
        for (int i=0; i<upList.length; i++) {
            atomListInts[0][i] = new int[upList[i].size()];
            for (int j=0; j<atomListInts[0][i].length; j++) {
                atomListInts[0][i][j] = upList[i].get(j).getNode().getAddress();
                
            }
        }
        
        atomListInts[1] = new int[downList.length][];
        for (int i=0; i<upList.length; i++) {
            atomListInts[1][i] = new int[downList[i].size()];
            for (int j=0; j<atomListInts[1][i].length; j++) {
                atomListInts[1][i][j] = downList[i].get(j).getNode().getAddress();
            }
        }
        out.writeObject(atomListInts);
    }

    /**
     * Just reads the data from the stream and stashes it back into the stream
     * object for later use.
     */
    private void readObject(java.io.ObjectInputStream in)
    throws IOException, ClassNotFoundException
    {
        EtomicaObjectInputStream etomicaIn = (EtomicaObjectInputStream)in; 
        //read nothing.
        etomicaIn.defaultReadObject();

        etomicaIn.objectData.put(this,etomicaIn.readObject());
        etomicaIn.dirtyObjects.add(this);
    }

    /**
     * Rebuilds the neighbor lists from the previously-read Atom indices.
     */
    public void rebuild(Object data) {
        int[][][] atomListInts = (int[][][])data;
        upList = new AtomArrayList[atomListInts[0].length];
        for (int i=0; i<upList.length; i++) {
            upList[i] = new AtomArrayList();
            for (int j=0; j<atomListInts[0][i].length; j++) {
                upList[i].add(EtomicaObjectInputStream.getAtomForIndex(atomListInts[0][i][j]));
            }
        }
        downList = new AtomArrayList[atomListInts[1].length];
        for (int i=0; i<downList.length; i++) {
            downList[i] = new AtomArrayList();
            for (int j=0; j<atomListInts[1][i].length; j++) {
                upList[i].add(EtomicaObjectInputStream.getAtomForIndex(atomListInts[1][i][j]));
            }
        }
    }
    
}
