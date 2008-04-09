/**
 * 
 */
package etomica.virial;

import etomica.action.AtomAction;
import etomica.api.IVector;
import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IMolecule;
import etomica.models.water.SpeciesWater3P;
import etomica.space.ISpace;

public class AtomActionRelaxWater3P implements AtomAction {
    public AtomActionRelaxWater3P(ISpace space) {
        work = space.makeVector();
        cosAngle = Math.cos(109.5/180.0*Math.PI);
        sinAngle = Math.sin(109.5/180.0*Math.PI);
        distance = 1.0;
    }
    
    public void actionPerformed(IAtom molecule) {
        IAtomPositioned O = (IAtomPositioned)((IMolecule)molecule).getChildList().getAtom(SpeciesWater3P.indexO);
        IAtomPositioned H1 = (IAtomPositioned)((IMolecule)molecule).getChildList().getAtom(SpeciesWater3P.indexH1);
        IAtomPositioned H2 = (IAtomPositioned)((IMolecule)molecule).getChildList().getAtom(SpeciesWater3P.indexH2);
        // normalize OH1
        IVector p1 = H1.getPosition();
        p1.ME(O.getPosition());
        p1.TE(1/Math.sqrt(p1.squared()));
        IVector p2 = H2.getPosition();
        p2.ME(O.getPosition());
        p2.TE(1/Math.sqrt(p2.squared()));
        // move H2 to fix bond angle
        double d = p1.dot(p2);
        work.E(p2);
        work.PEa1Tv1(-d,p1);
        work.TE(1/Math.sqrt(work.squared()));
        p2.Ea1Tv1(sinAngle,work);
        p2.PEa1Tv1(cosAngle,p1);
        p2.TE(distance/Math.sqrt(p2.squared()));
        p1.TE(distance);
        p1.PE(O.getPosition());
        p2.PE(O.getPosition());
    }

    private static final long serialVersionUID = 1L;
    private final IVector work;
    private final double sinAngle, cosAngle, distance;
}