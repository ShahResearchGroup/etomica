package etomica.potential;

import etomica.atom.DipoleSource;
import etomica.api.IAtomSet;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.INearestImageTransformer;
import etomica.api.IPotential;
import etomica.api.IVector;
import etomica.space.Space;
import etomica.space.Tensor;
import etomica.space3d.IVector3D;

public class P2ReactionFieldDipole extends Potential2 implements PotentialSoft, IPotentialTorque {

    public P2ReactionFieldDipole(Space space) {
        super(space);
        iDipole = (IVector3D)space.makeVector();
        cavityDipole = (IVector3D)space.makeVector();
        dr = space.makeVector();
        gradientAndTorque = new IVector[2][2];
        gradientAndTorque[0][0] = space.makeVector();
        gradientAndTorque[0][1] = space.makeVector();
        gradientAndTorque[1][0] = space.makeVector();
        gradientAndTorque[1][1] = space.makeVector();
    }

    /**
     * Returns the dipole source used by this object.
     */
    public DipoleSource getDipoleSource() {
        return dipoleSource;
    }

    /**
     * Sets the dipole source used by this object should use.
     */
    public void setDipoleSource(DipoleSource newDipoleSource) {
        dipoleSource = newDipoleSource;
    }
    
    public double getRange() {
        return cutoff;
    }
    
    public void setRange(double newRange) {
        cutoff = newRange;
        cutoff2 = newRange * newRange;
        fac = 2*(epsilon-1)/(2*epsilon+1)/(cutoff2*cutoff);
    }
    
    /**
     * Returns the dielectric constant of the fluid surrounding the cavity.
     */
    public double getDielectric() {
        return epsilon;
    }
    
    /**
     * Sets the dielectric constant of the fluid surrounding the cavity.
     */
    public void setDielectric(double newDielectric) {
        epsilon = newDielectric;
        if (cutoff > 0) {
            fac = 2*(epsilon-1)/(2*epsilon+1)/(cutoff2*cutoff);
        }
    }

    public void setBox(IBox box) {
        nearestImageTransformer = box.getBoundary();
    }

    public double energy(IAtomSet atoms) {
        iDipole.E(dipoleSource.getDipole((IMolecule)atoms.getAtom(0)));
        double idotj = iDipole.dot(dipoleSource.getDipole((IMolecule)atoms.getAtom(1)));

        return -fac*idotj;
    }
    
    public IVector[][] gradientAndTorque(IAtomSet atoms) {
        iDipole.E(dipoleSource.getDipole((IMolecule)atoms.getAtom(0)));

        iDipole.XE((IVector3D)dipoleSource.getDipole((IMolecule)atoms.getAtom(1)));
        iDipole.TE(fac);
        gradientAndTorque[0][0].E(0);
        gradientAndTorque[0][1].E(0);
        gradientAndTorque[1][0].E(iDipole);
        gradientAndTorque[1][1].Ea1Tv1(-1,iDipole);

        return gradientAndTorque;
    }

    public IVector[] gradient(IAtomSet atoms) {
        return gradientAndTorque[0];
    }

    public IVector[] gradient(IAtomSet atoms, Tensor pressureTensor) {
        return gradient(atoms);
    }

    public double virial(IAtomSet atoms) {
        return 0;
    }

    /**
     * Returns a 0-body potential that should be added along with this
     * potential.
     */
    public IPotential makeP0() {
        return new P0ReactionField(this);
    }

    private static final long serialVersionUID = 1L;
    protected final IVector3D iDipole, cavityDipole;
    protected final IVector dr;
    protected DipoleSource dipoleSource;
    protected INearestImageTransformer nearestImageTransformer;
    protected double cutoff2, cutoff;
    protected double epsilon;
    protected final IVector[][] gradientAndTorque;
    protected double fac;
    
    /**
     * A 0-body potential that should be added along with this potential.  The
     * 0-body potential includes the effective self-interaction of the
     * molecules (the molecule induces a dipole in the surrounding fluid, which
     * has an interaction energy with the molecule).  This part of the
     * potential does not result in a gradient or torque on the molecule and is
     * independent of position or orientation.
     */
    public static class P0ReactionField extends Potential0Lrc {

        public P0ReactionField(P2ReactionFieldDipole p) {
            super(p.getSpace(), new IAtomType[2], p);
            this.potential = p;
            gradient = new IVector[0];
        }
        
        public double energy(IAtomSet atoms) {
            double epsilon = potential.getDielectric();
            double cutoff = potential.getRange();
            DipoleSource dipoleSource = potential.getDipoleSource();
            double fac = 2*(epsilon-1)/(2*epsilon+1)/(cutoff*cutoff*cutoff);
            double u = 0;
            if (targetAtom != null) {
                IVector iDipole = dipoleSource.getDipole(targetAtom);
                u = -0.5 * fac * iDipole.squared();
            }
            else {
                IAtomSet moleculeList = box.getMoleculeList();
                for (int i=0; i<moleculeList.getAtomCount(); i++) {
                    IVector iDipole = dipoleSource.getDipole((IMolecule)moleculeList.getAtom(i));
                    u += -0.5 * fac * iDipole.squared();
                }
            }
            return u;
        }
        
        public void setBox(IBox newBox) {
            box = newBox;
        }
        
        public void setTargetAtoms(IAtomSet atoms) {
            if (atoms == null || atoms.getAtom(0) == null || !(atoms.getAtom(0) instanceof IMolecule)) {
                targetAtom = null;
                return;
            }
            targetAtom = (IMolecule)atoms.getAtom(0);
        }
        
        public IVector[] gradient(IAtomSet atoms) {
            return gradient;
        }
        
        public IVector[] gradient(IAtomSet atoms, Tensor pressureTensor) {
            return gradient(atoms);
        }
        
        public double virial(IAtomSet atoms) {
            return 0;
        }

        private static final long serialVersionUID = 1L;
        protected final P2ReactionFieldDipole potential;
        protected final IVector[] gradient;
        protected IMolecule targetAtom;
    }
}
