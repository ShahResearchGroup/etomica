package etomica.potential;

import java.util.Arrays;

import etomica.api.IAtom;
import etomica.api.IAtomList;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IMoleculeList;
import etomica.api.IPotential;
import etomica.api.IPotentialAtomic;
import etomica.api.IPotentialMaster;
import etomica.atom.iterator.ApiBuilder;
import etomica.atom.iterator.AtomIteratorBasisFilteredType;
import etomica.atom.iterator.AtomsetIteratorAllLeafAtoms;
import etomica.atom.iterator.AtomsetIteratorBasisDependent;
import etomica.atom.iterator.AtomsetIteratorDirectable;
import etomica.atom.iterator.IteratorDirective;
import etomica.atom.iterator.MoleculesetIterator;
import etomica.nbr.CriterionAll;
import etomica.nbr.NeighborCriterion;

/**
 * Collection of potentials that act between the atoms contained in
 * one or more groups of atoms.  This group iterates over all such atom-groups
 * assigned to it.  For each group it iterates over the potentials it contains,
 * instructing these sub-potentials to perform their calculations over the atoms
 * relevant to them in the groups.
 */
public class PotentialGroup extends PotentialMolecular {
    

    /**
     * Makes a potential group defined on the position of nBody atom or atom groups.
     * This constructor should only be called by the PotentialMaster.  Use 
     * potentialMaster.makePotentialGroup method to create PotentialGroups.
     */
    public PotentialGroup(int nBody) {
        super(nBody, null);
    }
    
    public void setPotentialMaster(IPotentialMaster newPotentialMaster) {
        potentialMaster = newPotentialMaster;
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if (link.potential instanceof PotentialGroup) {
                ((PotentialGroup)link.potential).setPotentialMaster(newPotentialMaster);
            }
            potentialMaster.potentialAddedNotify(link.potential, this);
        }
    }

	/**
	 * Indicates if a given potential is a sub-potential of this group.
	 * @param potential the potential in question
	 * @return boolean true if potential has been added to this group
	 */
    public boolean contains(IPotentialAtomic potential) {
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if(link.potential.equals(potential)) return true;
        }//end for
        return false;
    }
    
    /**
     * Adds the given potential and sets it up to apply to the atoms in the basis
     * having the given types.  Another addPotential method should be used if iteration
     * is not based on atom types. Length of types array must not exceed the order of
     * the potential (as given by the nBody method, and set in the constructor).
     * <br>
     * If length of types array is 1, this must be a one-body potential and iteration is
     * done over the atoms of the basis having the given type.  If types is of length 2
     * and this is a one-body potential, pairs are formed from atoms in the (single)
     * basis having the two given types (which might be the same); if this is a two-body
     * potential, pairs are formed from the first-type atoms taken from the first basis
     * atom, with the second-type atoms taken from the second basis.
     */
    public void addPotential(IPotentialAtomic potential, IAtomType[] types) {
        if(this.nBody() != Integer.MAX_VALUE && this.nBody() > types.length) throw new IllegalArgumentException("Order of potential cannot exceed length of types array.");
        Arrays.sort(types);
        if (this.nBody() == Integer.MAX_VALUE){addPotential(potential, new AtomsetIteratorAllLeafAtoms(), types);}
        else { 
        	switch(types.length) {
	            case 1:
	                addPotential(potential, new AtomIteratorBasisFilteredType(types[0]),types);
	                break;
	            case 2:
	                if(this.nBody() == 1) {
	                    throw new RuntimeException("It doesn't make sense to have type-based pair iteration within a molecule");
	                }
	                else if(this.nBody() == 2) {
	                    addPotential(potential,
	                            ApiBuilder.makeIntergroupTypeIterator(types),types);
	                }
	                break;
        	}
        }
        if(potential instanceof PotentialTruncated) {
            Potential0Lrc lrc = ((PotentialTruncated)potential).makeLrcPotential(types);
            if(lrc != null) {
                potentialMaster.lrcMaster().addPotential(lrc);
            }
        }
    }
 
    /**
     * Adds the given potential to this group, defining it to apply to the atoms
     * provided by the given basis-dependent iterator.  
     */
    public synchronized void addPotential(IPotentialAtomic potential, AtomsetIteratorBasisDependent iterator) {
        addPotential(potential,iterator,null);
    }
    
    protected void addPotential(IPotentialAtomic potential, AtomsetIteratorBasisDependent iterator, IAtomType[] types) {
        //the order of the given potential should be consistent with the order of the iterator
        if(potential.nBody() != iterator.nBody()) {
            throw new RuntimeException("Error: adding to PotentialGroup a potential and iterator that are incompatible");
        }
        //the given iterator should expect a basis of atoms equal in number to the order of this potential
        if(this.nBody() != iterator.basisSize()) {
            throw new RuntimeException("Error: adding an iterator that requires a basis size different from the nBody of the containing potential");
        }
        //put new potentials at beginning of list
        first = new PotentialLinker(potential, iterator, types, first);
        if (potentialMaster != null) {
            potentialMaster.potentialAddedNotify(potential, this);
        }
    }

    /**
     * Returns the AtomTypes that the given potential applies to if the given 
     * potential is within this potential group.  If the potential is not 
     * within this group or does not apply to specific AtomTypes, null is 
     * returned.
     */
    public IAtomType[] getAtomTypes(IPotential potential) {
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if (link.potential == potential) {
                return link.types;
            }
        }
        return null;
    }
    
    //TODO this needs some work
    public double energy(IMoleculeList basisAtoms) {
        if(basisAtoms.getMoleculeCount() != this.nBody()) {
            throw new IllegalArgumentException("Error: number of atoms for energy calculation inconsistent with order of potential");
        }
        double sum = 0.0;
        for (PotentialLinker link=first; link!= null; link=link.next) {	
            if(!link.enabled) continue;
            //if(firstIterate) ((AtomsetIteratorBasisDependent)link.iterator).setDirective(id);
            link.iterator.setBasis(basisAtoms);
            link.iterator.reset();
            for (IAtomList atoms = link.iterator.next(); atoms != null; atoms = link.iterator.next()) {
                sum += link.potential.energy(atoms);
            }
        }
        return sum;
    }
    
    /**
     * Returns the maximum of the range of all potentials in the group.
     */
    public double getRange() {
        double range = 0;
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if (link.potential.getRange() > range) {
                range = link.potential.getRange();
            }
        }
        return range;
    }
	
    /**
     * Removes given potential from the group.  No error is generated if
     * potential is not in group.  Returns true if the given Potential was
     * found and removed.
     */
    public boolean removePotential(IPotentialAtomic potential) {
        PotentialLinker previous = null;
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if(link.potential == potential) {//found it
                if(previous == null) first = link.next;  //it's the first one
                else previous.next = link.next;          //it's not the first one
                return true;
            }//end if
            previous = link;
        }
        return false;
    }
    
    /**
     * Performs the specified calculation over the iterates given by the iterator,
     * using the directive to set up the iterators for the sub-potentials of this group.
     */
    //TODO consider what to do with sub-potentials after target atoms are reached
    public void calculate(MoleculesetIterator iterator, IteratorDirective.Direction direction, IAtom targetAtom , PotentialCalculation pc) {
    	//loop over sub-potentials
    	//TODO consider separate loops for targetable and directable
    	for (PotentialLinker link=first; link!= null; link=link.next) {
    	    link.iterator.setTarget(targetAtom);
            // are all iterators with basis size=1 directable and all
            // iterators with basis size=2 not-directable
            if (link.iterator instanceof AtomsetIteratorDirectable) {
                ((AtomsetIteratorDirectable)link.iterator).setDirection(direction);
            }
    	}
    	iterator.reset();//loop over molecules affected by this potential group
    	for (IMoleculeList basisAtoms = iterator.next(); basisAtoms != null;
             basisAtoms = iterator.next()) {
    	    for (PotentialLinker link=first; link!= null; link=link.next) {
    	        if(!link.enabled) continue;
    	        final AtomsetIteratorBasisDependent atomIterator = link.iterator;
    	        atomIterator.setBasis(basisAtoms);
    	        atomIterator.reset();
    	        final IPotentialAtomic potential = link.potential;
    	        for (IAtomList atoms = atomIterator.next(); atoms != null;
    	             atoms = atomIterator.next()) {
    	            pc.doCalculation(atoms, potential);
    	        }
    	    }
    	}
    }
    
    public void setBox(IBox box) {
    	this.box = box;
  		for (PotentialLinker link=first; link!= null; link=link.next) {
  		    link.potential.setBox(box);
  		}
    }
    
    /**
     * Indicates that the specified potential should not contribute to potential
     * calculations. If potential is not in this group, no action is taken.
     */
    public void setEnabled(IPotentialAtomic potential, boolean enabled) {
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if(link.potential == potential) {
                link.enabled = enabled;
                return;
            }
        }
    }
    
    /**
     * Returns true if the potential is in this group and has not been disabled
     * via a previous call to setEnabled; returns false otherwise.
     */
    public boolean isEnabled(IPotentialAtomic potential) {
        for(PotentialLinker link=first; link!=null; link=link.next) {
            if(link.potential == potential) {
                return link.enabled;
            }
        }
        return false;
    }

    public IPotential[] getPotentials() {
        int nPotentials=0;
        for(PotentialLinker link=first; link!=null; link=link.next) {
            nPotentials++;
        }
        IPotential[] potentials = new Potential[nPotentials];
        int i=0;
        for(PotentialLinker link=first; link!=null; link=link.next) {
            potentials[i++] = link.potential;
        }
        return potentials;
    }
    
    private static final long serialVersionUID = 1L;
    protected PotentialLinker first;
    protected IBox box;
    protected IPotentialMaster potentialMaster;
    protected NeighborCriterion criterion = new CriterionAll();

    protected static class PotentialLinker implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        public final IPotentialAtomic potential;
        public final AtomsetIteratorBasisDependent iterator;
        public final IAtomType[] types;
        public PotentialLinker next;
        public boolean enabled = true;
        //Constructors
        public PotentialLinker(IPotentialAtomic a, AtomsetIteratorBasisDependent i, IAtomType[] t, PotentialLinker l) {
            potential = a;
            iterator = i;
            next = l;
            if (t != null) {
                types = t.clone();
            }
            else {
                types = null;
            }
        }
    }

}
