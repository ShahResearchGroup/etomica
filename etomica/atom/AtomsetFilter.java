package etomica.atom;

import etomica.api.IAtomList;


/**
 * Interface for a class that screens atoms according
 * to some criterion.
 */
 
 /* History of changes
  * 09/07/02 (DAK) new
  */
  
public interface AtomsetFilter {
    
    /**
     * Returns true if atom is passes test of filter.
     */
    public boolean accept(IAtomList a);

//------------ end of interface ---------------//


    /**
     * Static instance of a filter that accepts all atoms.
     * Returns true for null atom also.
     */
    public static final AtomsetFilter ACCEPT_ALL = new AtomsetFilter() {
        public boolean accept(IAtomList a) {return true;}
    };
    
    /**
     * Static instance of a filter that rejects all atoms.
     * Returns false for null atom also.
     */
    public static final AtomsetFilter ACCEPT_NONE = new AtomsetFilter() {
        public boolean accept(IAtomList a) {return false;}
    };
    
}