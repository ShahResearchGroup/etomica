package etomica.potential; 

import etomica.nbr.NeighborCriterion;
import etomica.space.ISpace;

/**
 * Potential that does not depend on any atom positions.
 * Typically used to implement long-range corrections for potential truncation.
 * Potential thus depends on box parameters, such as the number of molecules 
 * and the volume.
 *
 * @author David Kofke
 */

public abstract class Potential0 extends Potential {
      
    public Potential0(ISpace space) {
        super(0, space);
    }
    
    /**
     * Returns zero.
     */
    public double getRange() {
        return 0.0;
    }
    
    public NeighborCriterion getCriterion() {
        return null;
    }
                        
}//end of Potential0



