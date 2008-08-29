package etomica.virial;

import etomica.api.IBox;
import etomica.api.IMolecule;
import etomica.api.IPotentialMaster;
import etomica.api.IRandom;
import etomica.api.ISimulation;
import etomica.integrator.mcmove.MCMoveMolecule;
import etomica.space.ISpace;

/**
 * Standard Monte Carlo molecule-displacement trial move for cluster integrals.
 */
public class MCMoveClusterMolecule extends MCMoveMolecule {
    
    private static final long serialVersionUID = 1L;
    private final MeterClusterWeight weightMeter;

    public MCMoveClusterMolecule(ISimulation sim, IPotentialMaster potentialMaster,
    		                     ISpace _space) {
    	this(potentialMaster,sim.getRandom(), _space, 1.0);
    }
    
    public MCMoveClusterMolecule(IPotentialMaster potentialMaster, IRandom random,
    		                     ISpace _space, double stepSize) {
        super(potentialMaster,random,_space, stepSize,Double.POSITIVE_INFINITY,false);
        weightMeter = new MeterClusterWeight(potential);
    }
    
    public void setBox(IBox p) {
        super.setBox(p);
        weightMeter.setBox(p);
    }
    
    public boolean doTrial() {
        if(box.getMoleculeList().getAtomCount()==1) return false;
        
        atom = atomSource.getAtom();
        while (((IMolecule)atom).getIndex() == 0) {
            atom = atomSource.getAtom();
        }
        
        uOld = weightMeter.getDataAsScalar();
        groupTranslationVector.setRandomCube(random);
        groupTranslationVector.TE(stepSize);
        moveMoleculeAction.actionPerformed(atom);
        uNew = Double.NaN;
//        System.out.println(((AtomTreeNodeGroup)((AtomTreeNodeGroup)boxs[0].speciesMaster.node.childList.getFirst().node).childList.getLast().node).childList.getFirst().coord.position());
        ((BoxCluster)box).trialNotify();
        return true;
    }
    
    public double getB() {return 0.0;}
    
    public double getA() {
        uNew = weightMeter.getDataAsScalar();
//        if (Simulation.random.nextInt(200000) == 5) {
//            System.out.println("uOld "+uOld+" uNew "+uNew);
//        }
        return (uOld==0.0) ? Double.POSITIVE_INFINITY : uNew/uOld;
    }
    
    public void acceptNotify() {
        super.acceptNotify();
        ((BoxCluster)box).acceptNotify();
        System.out.println("acceptNotify");
//        System.out.println(atom+" accepted => "+atom.type.getPositionDefinition().position(atom));
    }
    
    public void rejectNotify() {
        super.rejectNotify();
        ((BoxCluster)box).rejectNotify();
        System.out.println("rejectNotify");
        //        System.out.println(atom+" rejected => "+atom.type.getPositionDefinition().position(atom));
    }
        
}