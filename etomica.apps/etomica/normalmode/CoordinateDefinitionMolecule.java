package etomica.normalmode;

import java.io.Serializable;

import etomica.action.AtomActionTranslateTo;
import etomica.atom.Atom;
import etomica.space.IVector;
import etomica.space.Space;

/**
 * CoordinateDefinition implementation for molecules. The class takes the first
 * space.D values of u to be real space displacements of the molecule center of
 * mass from its nominal position. Subclasses should add additional u values for
 * intramolecular degrees of freedom.
 * 
 * @author Andrew Schultz
 */
public class CoordinateDefinitionMolecule extends CoordinateDefinition
        implements Serializable {

    public CoordinateDefinitionMolecule(Space space, int orientationDim) {
        super(space.D() + orientationDim);
        this.space = space;
        work1 = space.makeVector();
        atomActionTranslateTo = new AtomActionTranslateTo(space);

    }

    public void calcU(Atom[] molecule, double[] u) {
        IVector pos = molecule[0].getType().getPositionDefinition().position(molecule[0]);
        IVector site = getLatticePosition(molecule[0]);
        work1.Ev1Mv2(pos, site);
        for (int i = 0; i < pos.getD(); i++) {
            u[i] = work1.x(i);
        }
    }

    /**
     * Override if nominal U is more than the lattice position of the molecule
     */
    public void initNominalU(Atom[] molecule) {
    }

    public void setToU(Atom[] molecule, double[] u) {
        IVector site = getLatticePosition(molecule[0]);
        for (int i = 0; i < space.D(); i++) {
            work1.setX(i, site.x(i) + u[i]);
        }
        atomActionTranslateTo.setDestination(work1);
        atomActionTranslateTo.actionPerformed(molecule[0]);
    }

    public void setNumAtoms(int numAtoms) {
        nominalU = new double[numAtoms][getCoordinateDim()];
    }

    private static final long serialVersionUID = 1L;
    protected final Space space;
    protected final IVector work1;
    protected double[][] nominalU;
    protected final AtomActionTranslateTo atomActionTranslateTo;
}
