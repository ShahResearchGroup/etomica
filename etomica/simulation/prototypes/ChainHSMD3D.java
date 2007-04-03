package etomica.simulation.prototypes;

import etomica.action.PhaseImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomType;
import etomica.atom.AtomTypeGroup;
import etomica.atom.AtomTypeLeaf;
import etomica.chem.models.ModelChain;
import etomica.config.ConfigurationLattice;
import etomica.config.ConformationLinear;
import etomica.graphics.BondListener;
import etomica.graphics.DisplayPhaseCanvasG3DSys;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHard;
import etomica.integrator.IntervalActionAdapter;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.list.PotentialMasterList;
import etomica.phase.Phase;
import etomica.potential.P2HardBond;
import etomica.potential.P2HardSphere;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheres;

public class ChainHSMD3D extends Simulation {

    private static final long serialVersionUID = 2L;
    public Phase phase;
    public IntegratorHard integrator;
    public SpeciesSpheres species;
    public P2HardSphere potential;
    
    public ChainHSMD3D() {
        this(Space3D.getInstance());
    }
    private ChainHSMD3D(Space space) {
        super(space, true, new PotentialMasterList(space));
        defaults.ignoreOverlap = true;
        int numAtoms = 704;
        int chainLength = 4;
        double neighborRangeFac = 1.6;
        defaults.makeLJDefaults();
        defaults.atomSize = 1.0;
        defaults.boxSize = 14.4573*Math.pow((chainLength*numAtoms/2020.0),1.0/3.0);
        ((PotentialMasterList)potentialMaster).setRange(neighborRangeFac*defaults.atomSize);

        integrator = new IntegratorHard(this);
        integrator.setIsothermal(false);
        integrator.addListener(((PotentialMasterList)potentialMaster).getNeighborManager());
        integrator.setTimeStep(0.01);
        
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(this,integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        
        ModelChain model = new ModelChain();
        model.setNumAtoms(chainLength);
        model.setBondingPotential(new P2HardBond(this));
        
        species = (SpeciesSpheres)model.makeSpecies(this);
        ((ConformationLinear)model.getConformation()).setBondLength(defaults.atomSize);
        ((ConformationLinear)model.getConformation()).setAngle(1,0.5);
        
        phase = new Phase(this);
        ConfigurationLattice config = new ConfigurationLattice(new LatticeCubicFcc());
        species.getAgent(phase).setNMolecules(numAtoms);
        config.initializeCoordinates(phase);

        PhaseImposePbc pbc = new PhaseImposePbc(phase);
        integrator.addListener(new IntervalActionAdapter(pbc));
        pbc.setApplyToMolecules(true);
        
        potential = new P2HardSphere(this);
        AtomTypeLeaf leafType = (AtomTypeLeaf)((AtomTypeGroup)species.getMoleculeType()).getChildTypes()[0];
        potentialMaster.addPotential(potential, new AtomType[]{leafType,leafType});

        integrator.setPhase(phase);
    }

    public static void main(String[] args) {
      final etomica.simulation.prototypes.ChainHSMD3D sim = new etomica.simulation.prototypes.ChainHSMD3D();
      final SimulationGraphic simGraphic = new SimulationGraphic(sim);
      new BondListener(sim.phase, (DisplayPhaseCanvasG3DSys)simGraphic.getDisplayPhase(sim.phase).canvas);
      
      simGraphic.makeAndDisplayFrame();
    }
}//end of class
