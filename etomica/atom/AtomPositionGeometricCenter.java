package etomica.atom;

import java.io.Serializable;

import etomica.action.AtomAction;
import etomica.action.AtomGroupAction;
import etomica.api.IAtom;
import etomica.api.IAtomPositionDefinition;
import etomica.api.IAtomPositioned;
import etomica.api.ISimulation;
import etomica.api.IVector;
import etomica.space.Space;

/**
 * Calculates the geometric center over a set of atoms. The position of the
 * atom or child atoms are accumulated and used to compute their
 * center (unweighted by mass). Calculated center is obtained via the getPosition
 * method.
 * 
 * @author David Kofke
 */
public class AtomPositionGeometricCenter implements IAtomPositionDefinition, Serializable {

    public AtomPositionGeometricCenter(ISimulation sim, Space _space) {
        this(_space);
    }
    
    public AtomPositionGeometricCenter(Space space) {
        vectorSum = space.makeVector();
        center = space.makeVector();
        myAction = new MyAction(vectorSum);
        groupWrapper = new AtomGroupAction(myAction);
    }

    public IVector position(IAtom atom) {
        vectorSum.E(0.0);
        myAction.nAtoms = 0;
        groupWrapper.actionPerformed(atom);
        center.Ea1Tv1(1.0 / myAction.nAtoms, vectorSum);
        return center;
    }

    private static class MyAction implements AtomAction, Serializable {
        public MyAction(IVector v) {
            vectorSum = v;
            nAtoms = 0;
        }
        
        public void actionPerformed(IAtom a) {
            vectorSum.PE(((IAtomPositioned)a).getPosition());
            nAtoms++;
        }
        
        private static final long serialVersionUID = 1L;
        private IVector vectorSum;
        public int nAtoms;
    }

    private final IVector center;
    private final IVector vectorSum;
    private AtomGroupAction groupWrapper;
    private MyAction myAction;
}