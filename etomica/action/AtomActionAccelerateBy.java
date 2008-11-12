package etomica.action;
import etomica.api.IAtom;
import etomica.api.IAtomKinetic;
import etomica.api.IVector;
import etomica.space.ISpace;

/**
 * 
 * Changes the velocity of an atom by a specified vector amount.
 * To accelerate all atoms in a molecule (or atom group), wrap an
 * instance of this class in an AtomGroupAction.
 * 
 * @author David Kofke
 */
public class AtomActionAccelerateBy implements AtomAction {
    
    private static final long serialVersionUID = 1L;
    private final IVector accelerationVector;
    
    public AtomActionAccelerateBy(ISpace space) {
        accelerationVector = space.makeVector();
    }
    
    public void actionPerformed(IAtom atom) {
        ((IAtomKinetic)atom).getVelocity().PE(accelerationVector);
    }
       
    /**
     * Returns the acceleration vector, the acceleration that will be
     * applied by this action. Returns the vector used by this
     * instance, not a copy, so any manipulation of the returned vector will
     * affect the action of this instance.
     */
    public IVector getAccelerationVector() {
        return accelerationVector;
    }
    /**
     * @param accelerationVector The acceleration vector to set.  A local copy
     * is made of the given vector.
     */
    public void setAccelerationVector(IVector accelerationVector) {
        this.accelerationVector.E(accelerationVector);
    }
}