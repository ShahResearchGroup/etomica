// Source file generated by Etomica

package etomica.simulation.prototypes;

import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomFactoryHomo;
import etomica.atom.AtomType;
import etomica.config.ConfigurationLattice;
import etomica.config.ConformationLinear;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.CriterionSimple;
import etomica.nbr.NeighborCriterion;
import etomica.nbr.list.PotentialMasterNbr;
import etomica.phase.Phase;
import etomica.potential.P1BondedHardSpheres;
import etomica.potential.P2HardSphere;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.Species;
import etomica.species.SpeciesSpheres;

public class ChainHSMD3D extends Simulation {

    public Phase phase;
    public IntegratorHard integrator;
    public SpeciesSpheres species;
    public P2HardSphere potential;
    
    public ChainHSMD3D() {
        this(Space3D.getInstance());
    }
    private ChainHSMD3D(Space space) {
//        super(space, new PotentialMaster(space));
        super(space, true, new PotentialMasterNbr(space, 1.6));
        defaults.ignoreOverlap = true;
        int numAtoms = 704;
        int chainLength = 4;
        double neighborRangeFac = 1.6;
        defaults.makeLJDefaults();
        defaults.atomSize = 1.0;
        defaults.boxSize = 14.4573*Math.pow((chainLength*numAtoms/2020.0),1.0/3.0);
        ((PotentialMasterNbr)potentialMaster).setRange(neighborRangeFac*defaults.atomSize);
//FIXME        ((PotentialMasterNbr)potentialMaster).setAtomPositionDefinition(new DataSourceCOM(space));

        integrator = new IntegratorHard(this);
        integrator.setIsothermal(false);
        integrator.addListener(((PotentialMasterNbr)potentialMaster).getNeighborManager());
        integrator.setTimeStep(0.01);
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(this,integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheres(this,chainLength);
        species.setNMolecules(numAtoms);
        ((ConformationLinear)((AtomFactoryHomo)species.getFactory()).getConformation()).setBondLength(defaults.atomSize);
        ((ConformationLinear)((AtomFactoryHomo)species.getFactory()).getConformation()).setAngle(1,0.5);
        
        phase = new Phase(this);
        new ConfigurationLattice(new LatticeCubicFcc()).initializeCoordinates(phase);
        
        P1BondedHardSpheres p1Intra = new P1BondedHardSpheres(this);
        potentialMaster.addPotential(p1Intra,new Species[]{species});
        
        //PotentialGroup p2Inter = new PotentialGroup(2, space);
        potential = new P2HardSphere(this);
        NeighborCriterion criterion = new CriterionSimple(space,potential.getRange(),neighborRangeFac*potential.getRange());
//FIXME        ApiFiltered interIterator = new ApiFiltered(new ApiIntergroup(),criterion);
//FIXME        p2Inter.addPotential(potential,interIterator);
//FIXME        NeighborCriterionWrapper moleculeCriterion = new NeighborCriterionWrapper(new NeighborCriterion[]{criterion});
//FIXME        moleculeCriterion.setNeighborRange(3.45 + criterion.getNeighborRange());
//FIXME        ((PotentialMasterNbr)potentialMaster).setSpecies(p2Inter,new Species[]{species,species},moleculeCriterion);
        ((PotentialMasterNbr)potentialMaster).getNeighborManager().addCriterion(criterion,
                new AtomType[]{((AtomFactoryHomo)species.moleculeFactory()).getChildFactory().getType()});
        
        //        Crystal crystal = new LatticeCubicFcc(space);
//        ConfigurationLattice conformation = new ConfigurationLattice(space, crystal);
//        phase.setConfiguration(conformation);
//        potential = new P2HardSphere(space);
//        this.potentialMaster.setSpecies(potential,new Species[]{species,species});

//        NeighborCriterion criterion = new NeighborCriterionSimple(space,potential.getRange(),neighborRangeFac*potential.getRange());
//        ((PotentialMasterNbr)potentialMaster).setSpecies(potential,new Species[]{species,species},criterion);

//      elementCoordinator.go();
        //explicit implementation of elementCoordinator activities
        integrator.setPhase(phase);
 //       integrator.addIntervalListener(new PhaseImposePbc(phase));
        
        //ColorSchemeByType.setColor(speciesSpheres0, java.awt.Color.blue);

 //       MeterPressureHard meterPressure = new MeterPressureHard(integrator);
 //       DataManager accumulatorManager = new DataManager(meterPressure);
        // 	DisplayBox box = new DisplayBox();
        // 	box.setDatumSource(meterPressure);
 //       phase.setDensity(0.7);
    } //end of constructor

}//end of class
