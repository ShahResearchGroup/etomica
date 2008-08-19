package etomica.simulation.prototypes;

import etomica.action.BoxImposePbc;
import etomica.action.BoxInflate;
import etomica.action.SimulationRestart;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DeviceNSelector;
import etomica.graphics.DisplayBox;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.list.NeighborListManager;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2HardSphere;
import etomica.potential.PotentialMasterMonatomic;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.ParameterBase;

/**
 * 
 * Three-dimensional hard-sphere molecular dynamics simulation, using
 * neighbor listing.  
 * <p>
 * Developed as a prototype and example for the construction of a basic simulation.
 *
 * @author David Kofke and Andrew Schultz
 *
 */
public class HSMD3D extends Simulation {

    //the following fields are made accessible for convenience to permit simple
    //mutation of the default behavior

    private static final long serialVersionUID = 1L;
    /**
     * The Box holding the atoms. 
     */
    public final IBox box;
    /**
     * The Integrator performing the dynamics.
     */
    public final IntegratorHard integrator;
    /**
     * The single hard-sphere species.
     */
    public final SpeciesSpheresMono species;
    /**
     * The hard-sphere potential governing the interactions.
     */
    public final P2HardSphere potential;
    
    public final IPotentialMaster potentialMaster;
    
    /**
     * Sole public constructor, makes a simulation using a 3D space.
     */
    public HSMD3D(Space _space) {
        this(_space, new HSMD3DParam());
    }
    
    public HSMD3D(Space _space, HSMD3DParam params) {

        // invoke the superclass constructor
        // "true" is indicating to the superclass that this is a dynamic simulation
        // the PotentialMaster is selected such as to implement neighbor listing
        super(_space, true);

        potentialMaster = params.useNeighborLists ? new PotentialMasterList(this, 1.6, space) : new PotentialMasterMonatomic(this, space);

        int numAtoms = params.nAtoms;
        double neighborRangeFac = 1.6;
        double sigma = 1.0;
        if (params.useNeighborLists) {
            ((PotentialMasterList)potentialMaster).setRange(neighborRangeFac*sigma);
        }

        integrator = new IntegratorHard(this, potentialMaster, space);
        integrator.setIsothermal(false);
        integrator.setTimeStep(0.01);

        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);

        species = new SpeciesSpheresMono(this, space);
        getSpeciesManager().addSpecies(species);
        potential = new P2HardSphere(space, sigma, false);
        IAtomTypeLeaf leafType = species.getLeafType();

        potentialMaster.addPotential(potential,new IAtomTypeLeaf[]{leafType, leafType});

        box = new Box(this, space);
        addBox(box);
        box.setNMolecules(species, numAtoms);
        BoxInflate inflater = new BoxInflate(box, space);
        inflater.setTargetDensity(params.eta * 6 / Math.PI);
        inflater.actionPerformed();
        new ConfigurationLattice(new LatticeCubicFcc(), space).initializeCoordinates(box);
        //deformed
//        box.setBoundary(
//            new etomica.space.BoundaryDeformablePeriodic(
//            space,getRandom(),
//            new IVector[]{
//              new Vector3D(-4,1,1),
//              new Vector3D(2,6,4),
//              new Vector3D(1,2,6)}));
        //truncated octahedron
//        box.setBoundary(
//            new etomica.space3d.BoundaryTruncatedOctahedron(this));
        
        integrator.setBox(box);

        if (params.useNeighborLists) { 
            NeighborListManager nbrManager = ((PotentialMasterList)potentialMaster).getNeighborManager(box);
            ((PotentialMasterList)potentialMaster).setRange(sigma*neighborRangeFac);
            integrator.addIntervalAction(nbrManager);
            integrator.addNonintervalListener(nbrManager);
        }
        else {
            integrator.addIntervalAction(new BoxImposePbc(box, space));
        }
    }

    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
    	final String APP_NAME = "HSMD3D";

    	Space sp = Space3D.getInstance();
        HSMD3DParam params = new HSMD3DParam();
        params.ignoreOverlap = true;
        final etomica.simulation.prototypes.HSMD3D sim = new etomica.simulation.prototypes.HSMD3D(sp, params);
        final SimulationGraphic simGraphic = new SimulationGraphic(sim, APP_NAME, sim.space);
        DeviceNSelector nSelector = new DeviceNSelector(sim.getController());
        nSelector.setResetAction(new SimulationRestart(sim, sp));
        nSelector.setSpecies(sim.species);
        nSelector.setBox(sim.box);

        nSelector.setPostAction(simGraphic.getPaintAction(sim.box));
        simGraphic.add(nSelector);

        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));

        simGraphic.makeAndDisplayFrame(APP_NAME);
        ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
        colorScheme.setColor(sim.species.getLeafType(), java.awt.Color.red);
    }

    public static HSMD3DParam getParameters() {
        return new HSMD3DParam();
    }

    /**
     * Inner class for parameters understood by the HSMD3D constructor
     */
    public static class HSMD3DParam extends ParameterBase {
        public int nAtoms = 256;
        public double eta = 0.35;
        public boolean ignoreOverlap = false;
        public boolean useNeighborLists = true;
    }
}
