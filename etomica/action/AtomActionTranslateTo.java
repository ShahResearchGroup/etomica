package etomica.action;


import java.io.Serializable;

import etomica.api.IAtom;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IVector;
import etomica.atom.AtomPositionCOM;
import etomica.space.Space;

/**
 * Moves (translates) an atom to a specified position.  Location of the
 * atom (which may be an atom group) is determined by an AtomPositionDefinition
 * instance that may be set for this class.
 */
public class AtomActionTranslateTo implements AtomAction, Serializable {
    
    private static final long serialVersionUID = 1L;
    private final IVector destination;
    private IAtomPositionDefinition atomPositionDefinition;
    private AtomGroupAction atomTranslator;
    private final IVector translationVector;

    /**
     * Creates new action with atom position defined by its
     * center of mass (via AtomPositionCOM).
     * @param space
     */
    public AtomActionTranslateTo(Space space) {
        destination = space.makeVector();
        atomPositionDefinition = new AtomPositionCOM(space);
        atomTranslator = new AtomGroupAction(new AtomActionTranslateBy(space));
        translationVector = ((AtomActionTranslateBy)atomTranslator.getAction()).getTranslationVector();
    }
    
    public void actionPerformed(IAtom atom) {
        IVector currentPosition = atomPositionDefinition.position(atom);
        translationVector.Ev1Mv2(destination, currentPosition);
        atomTranslator.actionPerformed(atom);
    }
       
    /**
     * @return Returns the destination, the position that the
     * atom will be moved to by this action.
     */
    public IVector getDestination() {
        return destination;
    }
    /**
     * @param destination The destination to set.  A local copy
     * is made of the given vector.
     */
    public void setDestination(IVector newDestination) {
        destination.E(newDestination);
    }
    /**
     * @return Returns the atomPositionDefinition.
     */
    public IAtomPositionDefinition getAtomPositionDefinition() {
        return atomPositionDefinition;
    }
    /**
     * @param atomPositionDefinition The atomPositionDefinition to set.
     */
    public void setAtomPositionDefinition(
            IAtomPositionDefinition atomPositionDefinition) {
        this.atomPositionDefinition = atomPositionDefinition;
    }
    
    /**
     * Returns the vector that was used to accomplish the most recent translation action.
     * This vector can be used to reverse the translation by multiplying it by -1 and 
     * performing an atomActionTranslateBy with it.
     */
    public IVector getTranslationVector() {
        return translationVector;
    }
}