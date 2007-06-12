package etomica.simulation.prototypes;
import etomica.action.activity.ActivityIntegrate;
import etomica.config.ConfigurationLattice;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeOrthorhombicHexagonal;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.phase.Phase;
import etomica.potential.P2HardSphere;
import etomica.potential.Potential2;
import etomica.simulation.Simulation;
import etomica.space2d.Space2D;
import etomica.species.Species;
import etomica.species.SpeciesSpheresMono;
import etomica.util.Default;

/**
 * Simple hard-sphere molecular dynamics simulation in 2D.
 *
 * @author David Kofke
 */
 
public class HSMD2D extends Simulation {
    
    private static final long serialVersionUID = 1L;
    public IntegratorHard integrator;
    public SpeciesSpheresMono species, species2;
    public Phase phase;
    public Potential2 potential;
    public Potential2 potential2;
    public Potential2 potential22;

    public HSMD2D() {
        this(new Default());
    }
    
    public HSMD2D(Default defaults) {
    	this(Space2D.getInstance(), defaults);
    }
    
    private HSMD2D(Space2D space, Default defaults) {
        super(space, true, Default.BIT_LENGTH, defaults);
        PotentialMasterList potentialMaster = new PotentialMasterList(this);
//        super(space, new PotentialMaster(space));//,IteratorFactoryCell.instance));
        defaults.makeLJDefaults();
        defaults.atomSize = 0.38;

        double neighborRangeFac = 1.6;
        potentialMaster.setRange(neighborRangeFac*defaults.atomSize);

        integrator = new IntegratorHard(this, potentialMaster);
        integrator.setIsothermal(false);
        integrator.setTimeStep(0.01);

        potentialMaster.setRange(defaults.atomSize*1.6);

        ActivityIntegrate activityIntegrate = new ActivityIntegrate(this,integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheresMono(this);
	    species2 = new SpeciesSpheresMono(this);
        getSpeciesManager().addSpecies(species);
        getSpeciesManager().addSpecies(species2);
        potential = new P2HardSphere(this);
        potential2 = new P2HardSphere(this);
        potential22 = new P2HardSphere(this);
        
        potentialMaster.addPotential(potential,new Species[]{species,species});

        potentialMaster.addPotential(potential2,new Species[]{species2,species2});

        potentialMaster.addPotential(potential22,new Species[]{species2,species});

        phase = new Phase(this);
        addPhase(phase);
        phase.getAgent(species).setNMolecules(512);
        phase.getAgent(species2).setNMolecules(5);
        NeighborListManager nbrManager = potentialMaster.getNeighborManager(phase);
        integrator.addIntervalAction(nbrManager);
        integrator.addNonintervalListener(nbrManager);
        new ConfigurationLattice(new LatticeOrthorhombicHexagonal()).initializeCoordinates(phase);
        integrator.setPhase(phase);
    }
    
    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
        HSMD2D sim = new HSMD2D();
		sim.getController().actionPerformed();
    }//end of main
    
}