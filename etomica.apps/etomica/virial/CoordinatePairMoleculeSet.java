package etomica.virial;

import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.space.ISpace;
import etomica.util.Debug;

/**
 * @author David Kofke
 *
 * Class that holds a set of atom pairs.  Takes a list of atoms in its
 * constructor, and forms an instance of CoordinatePair for each pair formed in the
 * list.  Each CoordinatePair instance can be accessed via the getPair method.  The
 * CoordinatePairLeafSet has an ID, which changes whenever the atom positions change 
 * (after which reset() should be called).  Cluster.value() depends on a one-to-one
 * correspondence between this ID and the positions of atoms in the CoordinatePairLeafSet. 
 */
public class CoordinatePairMoleculeSet implements java.io.Serializable, CoordinatePairSet {

    /**
     * Constructor for CoordinatePairLeafSet.
     * @param list The list of atoms for which the set of pairs is formed.
     */
    public CoordinatePairMoleculeSet(IAtomSet list, ISpace space) {
        atoms = new IAtom[list.getAtomCount()];
        numAtoms = list.getAtomCount();
        r2 = new double[numAtoms*numAtoms];
        setAtoms(list);
        dr = space.makeVector();
        iPosition = space.makeVector();
    }
    
    /**
     * Returns atom pair for ith and jth atoms in set.
     */
    public double getr2(int i, int j) {
        if(Debug.ON && !(i<j)) throw new IllegalArgumentException("Error: i must be less than j");
        return r2[i*numAtoms+j];
    }

    private void setAtoms(IAtomSet list) {
        AtomIteratorArrayListSimple iterator = new AtomIteratorArrayListSimple(list);
        iterator.reset();
        int k=0;
        for (IAtom atom = iterator.nextAtom(); atom != null;
             atom = iterator.nextAtom()) {
            atoms[k++] = atom;
        }
    }
    
    public void reset() {
        for(int i=0; i<numAtoms-1; i++) {
            IAtom iAtom = atoms[i];
            iPosition.E(((ISpecies)iAtom.getType()).getPositionDefinition().position(iAtom));
            for(int j=i+1; j<numAtoms; j++) {
                IAtom jAtom = atoms[j];
                IVector jPosition = ((ISpecies)jAtom.getType()).getPositionDefinition().position(jAtom);
                dr.Ev1Mv2(iPosition, jPosition);
                r2[i*numAtoms+j] = dr.squared();
            }
        }
        ID = staticID++;
    }
    
    public void E(CoordinatePairLeafSet c) {
        for(int i=0; i<numAtoms-1; i++) {
            for(int j=i+1; j<numAtoms; j++) {
                r2[i*numAtoms+j] =  c.r2[i*numAtoms+j];
            }
        }
    }
    
    public int getID() {
        return ID;
    }
    
    private static final long serialVersionUID = 1L;
    protected final double[] r2;
    protected final IAtom[] atoms;
    protected final int numAtoms;
    protected final IVector dr;
    private final IVector iPosition;
    private int ID;
    private static int staticID;
}