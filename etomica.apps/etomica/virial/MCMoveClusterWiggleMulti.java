package etomica.virial;

import etomica.api.IVector;
import etomica.api.IAtomSet;
import etomica.api.IAtomPositioned;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.ISimulation;
import etomica.api.IVector3D;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.space.ISpace;
import etomica.space3d.Vector3D;
import etomica.util.Debug;
import etomica.api.IRandom;

/**
 * An MC Move for cluster simulations that "wiggles" a chain molecule.  If the 
 * first or last atom in the chain is chosen, it is moved to a new position 
 * with the same bond length as before, but perturbed by some angle from its
 * original position.  If an Atom in the middle of the chain is chosen, a 
 * crankshaft move is performed that maintains its distances with its 
 * neighbors.  If a middle Atom has a bond angle too close to 180 degrees
 * (such that rotation does nothing) the Atom is not moved at all.
 * In each doTrial, wiggle moves are attempted on all molecules in the box. 
 * 
 * @author Andrew Schultz
 */
public class MCMoveClusterWiggleMulti extends MCMoveMolecule {

    public MCMoveClusterWiggleMulti(ISimulation sim, IPotentialMaster potentialMaster, int nAtoms, ISpace _space) {
    	this(potentialMaster,sim.getRandom(), 1.0, nAtoms, _space);
    }
    
    /**
     * Constructor for MCMoveAtomMulti.
     * @param parentIntegrator
     * @param nAtoms number of atoms to move in a trial.  Number of atoms in
     * box should be at least one greater than this value (greater
     * because first atom is never moved)
     */
    public MCMoveClusterWiggleMulti(IPotentialMaster potentialMaster, 
            IRandom random, double stepSize, int nAtoms, ISpace _space) {
        super(potentialMaster,random,_space, stepSize,Double.POSITIVE_INFINITY,false);
        this.space = _space;
        setStepSizeMax(Math.PI);
        weightMeter = new MeterClusterWeight(potential);
        energyMeter = new MeterPotentialEnergy(potential);
        work1 = (IVector3D)_space.makeVector();
        work2 = (IVector3D)_space.makeVector();
        work3 = (IVector3D)_space.makeVector();
    }

    public void setBox(IBox p) {
        super.setBox(p);
        selectedAtoms = new IAtomPositioned[box.getMoleculeList().getAtomCount()];
        translationVectors = new Vector3D[box.getMoleculeList().getAtomCount()];
        for (int i=0; i<translationVectors.length; i++) {
            translationVectors[i] = (IVector3D)space.makeVector();
        }
        weightMeter.setBox(p);
        energyMeter.setBox(p);
    }
    
    //note that total energy is calculated
    public boolean doTrial() {
        uOld = energyMeter.getDataAsScalar();
        wOld = weightMeter.getDataAsScalar();

        IAtomSet moleculeList = box.getMoleculeList();
        for(int i=0; i<moleculeList.getAtomCount(); i++) {
            IAtomSet childList = ((IMolecule)moleculeList.getAtom(i)).getChildList();
            int numChildren = childList.getAtomCount();

            int j = random.nextInt(numChildren);
            selectedAtoms[i] = (IAtomPositioned)childList.getAtom(j);
//            System.out.println(selectedAtoms[i]+" "+j+" before "+selectedAtoms[i].coord.position());
            IVector position = selectedAtoms[i].getPosition();
            translationVectors[i].Ea1Tv1(-1,position);
            double oldBondLength1 = 0, oldBondLength2 = 0;
                
            if (j == 0 || j == numChildren-1) {
                // this puts atom j in a random orientation without changing
                // the bond length
//                System.out.println("end"+j+" move");

                //work1 is the current vector from the bonded atom to atom j
                work1.E(position);
                if (j == 0) {
                    work1.ME(((IAtomPositioned)childList.getAtom(j+1)).getPosition());
                    position.E(((IAtomPositioned)childList.getAtom(j+1)).getPosition());
                }
                else {
                    work1.ME(((IAtomPositioned)childList.getAtom(j-1)).getPosition());
                    position.E(((IAtomPositioned)childList.getAtom(j-1)).getPosition());
                }
                double bondLength = Math.sqrt(work1.squared());
                if (Debug.ON && Debug.DEBUG_NOW) {
                    oldBondLength1 = bondLength;
                }
                //work2 is a vector perpendicular to work1.  it can be any 
                //perpendicular vector, but that just makes it harder!
                if (work1.x(0)*work1.x(0) < 0.5*bondLength*bondLength) {
                    // if work1 doesn't point in the X direction (mostly) then
                    // find a vector in the plane containing the X axis and work1
                    double a = -work1.x(0)/bondLength;
                    work2.Ea1Tv1(a,work1);
                    work2.setX(0,work2.x(0)+bondLength);
                }
                else {
                    // work1 does point in the X direction (mostly) so
                    // find a vector in the plane containing the Y axis and work1
                    double a = -work1.x(1)/bondLength;
                    work2.Ea1Tv1(a,work1);
                    work2.setX(1,work2.x(1)+bondLength);
                }
                //normalize
                work2.TE(bondLength/Math.sqrt(work2.squared()));
                //work3 is a vector normal to both work1 and work2
                work3.E(work1);
                work3.XE(work2);
                work3.TE(bondLength/Math.sqrt(work3.squared()));
                
                double phi = (random.nextDouble()-0.5)*Math.PI;
                work2.TE(Math.cos(phi));
                work2.PEa1Tv1(Math.sin(phi),work3);
            }
            else {
                // crankshaft move.  atom j is rotated around the j-1 - j+1 bond.
                // j-1 - j and j - j+1 bond lengths are unaltered.

//                System.out.println("middle move "+j);
                IVector position0 = ((IAtomPositioned)childList.getAtom(j-1)).getPosition();
                IVector position2 = ((IAtomPositioned)childList.getAtom(j+1)).getPosition();
                work1.Ev1Mv2(position0, position);
                work2.Ev1Mv2(position2, position);
                if (Debug.ON && Debug.DEBUG_NOW) {
                    oldBondLength1 = Math.sqrt(work1.squared());
                    oldBondLength2 = Math.sqrt(work2.squared());
                }
                double cosTheta = work1.dot(work2)/(Math.sqrt(work1.squared()*work2.squared()));
                if (cosTheta < -0.999) {
                    // current bond angle is almost 180degrees, making crankshaft
                    // difficult to do precisely, so skip it.  we'll explore this
                    // degree of freedom some other time when the bond angle is
                    // different
                    translationVectors[i].E(0);
                    continue;
                }
                work2.Ev1Pv2(position0, position2);
                work2.TE(0.5);
                //work1 is vector between the 0-2 midpoint and 1
                work1.Ev1Mv2(position,work2);
                position.E(work2);
                work2.ME(position0);
                work2.TE(-1);
                work2.XE(work1);
                //work2 is vector between the 0-2 midpoint and 0, normalized to
                //to be the same length as work1
                work2.TE(Math.sqrt(work1.squared()/work2.squared()));
            }
            
            double theta = (random.nextDouble()-0.5)*stepSize;
            position.PEa1Tv1(Math.cos(theta),work1);
            position.PEa1Tv1(Math.sin(theta),work2);

            translationVectors[i].PE(position);
            work1.E(translationVectors[i]);
            work1.TE(1.0/childList.getAtomCount());
            for (int k=0; k<childList.getAtomCount(); k++) {
                ((IAtomPositioned)childList.getAtom(k)).getPosition().ME(work1);
            }
            if (Debug.ON && Debug.DEBUG_NOW) {
                if (j > 0) {
                    work1.Ev1Mv2(position, ((IAtomPositioned)childList.getAtom(j-1)).getPosition());
                    double bondLength = Math.sqrt(work1.squared());
                    if (Math.abs(bondLength - oldBondLength1)/oldBondLength1 > 0.000001) {
                        throw new IllegalStateException("wiggle "+i+" "+j+" bond length should be close to "+oldBondLength1+" ("+bondLength+")");
                    }
                }
                if (j < numChildren-1) {
                    work1.Ev1Mv2(position, ((IAtomPositioned)childList.getAtom(j+1)).getPosition());
                    double bondLength = Math.sqrt(work1.squared());
                    double oldBondLength = oldBondLength2 == 0 ? oldBondLength1 : oldBondLength2;
                    if (Math.abs(bondLength - oldBondLength)/oldBondLength > 0.000001) {
                        throw new IllegalStateException("wiggle "+i+" "+j+" bond length should be close to "+oldBondLength+" ("+bondLength+")");
                    }
                }
            }
        }
        ((BoxCluster)box).trialNotify();
        wNew = weightMeter.getDataAsScalar();
        uNew = energyMeter.getDataAsScalar();
        return true;
    }
    
    public void rejectNotify() {
        IAtomSet moleculeList = box.getMoleculeList();
        for(int i=0; i<selectedAtoms.length; i++) {
            IAtomSet childList = ((IMolecule)moleculeList.getAtom(i)).getChildList();
            work1.E(translationVectors[i]);
            work1.TE(1.0/childList.getAtomCount());
            for (int k=0; k<childList.getAtomCount(); k++) {
                ((IAtomPositioned)childList.getAtom(k)).getPosition().PE(work1);
            }
            selectedAtoms[i].getPosition().ME(translationVectors[i]);
        }
        ((BoxCluster)box).rejectNotify();
    }

    public double getB() {
        return -(uNew - uOld);
    }
    
    public double getA() {
        return wNew/wOld;
    }
	
    private static final long serialVersionUID = 1L;
    protected final MeterClusterWeight weightMeter;
    protected final MeterPotentialEnergy energyMeter;
    protected IAtomPositioned[] selectedAtoms;
    protected final IVector3D work1, work2, work3;
    protected IVector3D[] translationVectors;
    protected double wOld, wNew;
    protected final ISpace space;
}
