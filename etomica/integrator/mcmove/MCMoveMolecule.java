package etomica.integrator.mcmove;

import etomica.action.AtomActionTranslateBy;
import etomica.action.AtomGroupAction;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.atom.AtomSourceRandomMolecule;
import etomica.space.ISpace;
import etomica.space.IVectorRandom;

/**
 * Standard Monte Carlo molecule-displacement trial move.
 *
 * @author David Kofke
 */
public class MCMoveMolecule extends MCMoveAtom {
    
    private static final long serialVersionUID = 1L;
    protected final AtomGroupAction moveMoleculeAction;
    protected final IVectorRandom groupTranslationVector;

    public MCMoveMolecule(ISimulation sim, IPotentialMaster potentialMaster,
    		              ISpace _space) {
        this(potentialMaster, sim.getRandom(), _space, 1.0, 15.0, false);
    }
    
    public MCMoveMolecule(IPotentialMaster potentialMaster, IRandom random,
    		              ISpace _space, double stepSize,
                          double stepSizeMax, boolean ignoreOverlap) {
        super(potentialMaster, random, _space, stepSize,stepSizeMax,ignoreOverlap);
        AtomActionTranslateBy translator = new AtomActionTranslateBy(_space);
        groupTranslationVector = (IVectorRandom)translator.getTranslationVector();
        moveMoleculeAction = new AtomGroupAction(translator);
        
        //set directive to exclude intramolecular contributions to the energy

        //TODO enable meter to do this
        //       iteratorDirective.addCriterion(new IteratorDirective.PotentialCriterion() {
 //           public boolean excludes(Potential p) {return (p instanceof Potential1.Intramolecular);}
 //       });
        AtomSourceRandomMolecule randomMoleculeSource = new AtomSourceRandomMolecule();
        randomMoleculeSource.setRandom(random);
        setAtomSource(randomMoleculeSource);
    }
    

    public boolean doTrial() {
        if(box.getMoleculeList().getAtomCount()==0) return false;
        
        atom = atomSource.getAtom();

        energyMeter.setTarget(atom);
        uOld = energyMeter.getDataAsScalar();
        if(Double.isInfinite(uOld)) {
            throw new RuntimeException("Started with overlap");
        }
        groupTranslationVector.setRandomCube(random);
        groupTranslationVector.TE(stepSize);
        moveMoleculeAction.actionPerformed(atom);
        uNew = energyMeter.getDataAsScalar();
        return true;
    }
    
    public void rejectNotify() {
        groupTranslationVector.TE(-1);
        moveMoleculeAction.actionPerformed(atom);
    }
        
}