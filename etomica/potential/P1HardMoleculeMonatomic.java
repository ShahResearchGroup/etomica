package etomica.potential;

import etomica.api.IAtomSet;
import etomica.api.IMolecule;
import etomica.api.IPotential;
import etomica.space.ISpace;
import etomica.space.Tensor;

/**
 * 2-body hard Potential class for use between two monatomic molecules.
 *
 * @author Andrew Schultz
 */
 public class P1HardMoleculeMonatomic extends P1MoleculeMonatomic implements
        PotentialHard {

    public P1HardMoleculeMonatomic(ISpace space, IPotential potential) {
        super(space, potential);
    }

    public void bump(IAtomSet atoms, double falseTime) {
        leafAtomSet.atom = ((IMolecule)atoms.getAtom(0)).getChildList().getAtom(0);
        ((PotentialHard)wrappedPotential).bump(leafAtomSet, falseTime);
    }

    public double collisionTime(IAtomSet atoms, double falseTime) {
        leafAtomSet.atom = ((IMolecule)atoms.getAtom(0)).getChildList().getAtom(0);
        return ((PotentialHard)wrappedPotential).collisionTime(leafAtomSet, falseTime);
    }

    public double energyChange() {
        return ((PotentialHard)wrappedPotential).energyChange();
    }

    public double lastCollisionVirial() {
        return ((PotentialHard)wrappedPotential).lastCollisionVirial();
    }

    public Tensor lastCollisionVirialTensor() {
        return ((PotentialHard)wrappedPotential).lastCollisionVirialTensor();
    }

    private static final long serialVersionUID = 1L;
}
