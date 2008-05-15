package etomica.models.hexane;

import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomSet;
import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.IVector;
import etomica.atom.AtomArrayList;
import etomica.atom.AtomSource;
import etomica.atom.AtomSourceRandomMolecule;
import etomica.atom.iterator.AtomIterator;
import etomica.atom.iterator.AtomIteratorSinglet;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMoveBox;
import etomica.space.ISpace;
import etomica.util.Constants;

public abstract class MCMoveCBMC extends MCMoveBox {

    public MCMoveCBMC(IPotentialMaster potentialMaster, IRandom random,
            ISpace _space, IntegratorMC integrator, IBox p, int maxAtomsPerMolecule,
            int NTrial) {
        super(potentialMaster);
        this.random = random;

        setNumberOfTrials(NTrial);

        beta = 1.0 / integrator.getTemperature() / Constants.BOLTZMANN_K;
        atomList = new AtomArrayList(maxAtomsPerMolecule);
        affectedAtomIterator = new AtomIteratorSinglet();

        externalMeter = new MeterPotentialEnergy(potentialMaster);

        box = p;

        moleculeSource = new AtomSourceRandomMolecule();
        moleculeSource.setBox(box);
        ((AtomSourceRandomMolecule) moleculeSource).setRandom(random);
        setMoleculeSource(moleculeSource);

        positionOld = new IVector[maxAtomsPerMolecule];
        for (int i = 0; i < maxAtomsPerMolecule; i++) {
            positionOld[i] = _space.makeVector();
        }
    }

    /**
     * Sets the AtomSource used to select molecules acted on by MC trials.
     */
    public void setMoleculeSource(AtomSource newMoleculeSource) {
        moleculeSource = newMoleculeSource;
    }

    /**
     * Returns the AtomSource used to select Atoms acted on by MC trials.
     */
    public AtomSource getMoleculeSource() {
        return moleculeSource;
    }

    public abstract double energyChange();

    public void setBox(IBox p) {
        super.setBox(p);
        externalMeter.setBox(p);
    }

    public void acceptNotify() {
//         System.out.println("ACCEPTED A WHOLE MOVE!!!!!!!!!!!!!!!!!!!!!!");
    }

    public boolean doTrial() {
//        System.out.println("doTrial() CBMC called"); 

        // pick a molecule & get its childlist
        atom = moleculeSource.getAtom();
        if (atom == null){
            return false;
        }
        affectedAtomIterator.setAtom(atom);

        // we assume that that atoms that make the molecule are children of the
        // molecule.
        atomList = ((IMolecule) atom).getChildList();
        chainlength = atomList.getAtomCount();

        // store the old locations of every atom in the molecule in positionOld.
        for (int i = 0; i < chainlength; i++) {
            positionOld[i].E(((IAtomPositioned) atomList.getAtom(i)).getPosition());
        }

        return calcRosenbluthFactors(); // this means we were able to propose a move.
    }

    public boolean doTrial(IAtom atom){
        if (atom == null)
            return false;
        affectedAtomIterator.setAtom(atom);

        // we assume that that atoms that make the molecule are children of the
        // molecule.
        atomList = ((IMolecule) atom).getChildList();
        chainlength = atomList.getAtomCount();

        // store the old locations of every atom in the molecule in positionOld.
        for (int i = 0; i < chainlength; i++) {
            positionOld[i].E(((IAtomPositioned) atomList.getAtom(i)).getPosition());
        }

        calcRosenbluthFactors();
        return true; // this means we were able to propose a move.
    }
    
    public double getA() {
        return wNew / wOld;
    }

    public double getB() {
        return 0.0;
    }

    protected abstract boolean calcRosenbluthFactors();

    public void setNumberOfTrials(int n) {
        numTrial = n;
    }

    protected void setChainlength(int n) {
        chainlength = n;
    }

    public int getNumberOfTrials() {
        return numTrial;
    }

    public int getChainlength() {
        return chainlength;
    }

    public void rejectNotify() {
        for (int i = 0; i < chainlength; i++) {
            ((IAtomPositioned) atomList.getAtom(i)).getPosition().E(positionOld[i]);
        }
//        System.out.println("MCMoveCBMC rejects another!!");
    }

    public AtomIterator affectedAtoms() {
        return affectedAtomIterator;
    }

    
    private static final long serialVersionUID = 1L;

    protected final MeterPotentialEnergy externalMeter;

    protected double wNew; // Rosenbluth factor of new configuration

    protected double wOld; // Rosenbluth factor of old configuration

    protected int chainlength; // the number of atoms in a molecule; some
                                // juggling may be necessary to make this work
                                // if we want to make the chains longer....

    protected double beta;

    protected IAtom atom;

    protected double uOld;

    protected double uNew = Double.NaN;

    protected IVector[] positionOld; // Used to store the position of the
                                        // molecule before mofing it.

    protected IAtomSet atomList;

    protected int numTrial;

    protected AtomSource moleculeSource;

    private AtomIteratorSinglet affectedAtomIterator;

    protected final IRandom random;

}