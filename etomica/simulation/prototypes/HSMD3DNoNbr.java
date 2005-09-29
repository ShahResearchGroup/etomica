// Source file generated by Etomica

package etomica.simulation.prototypes;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import etomica.action.PhaseImposePbc;
import etomica.action.activity.ActivityIntegrate;
import etomica.atom.AtomList;
import etomica.config.ConfigurationLattice;
import etomica.integrator.IntegratorHard;
import etomica.lattice.LatticeCubicFcc;
import etomica.phase.Phase;
import etomica.potential.P2HardSphere;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.Species;
import etomica.species.SpeciesSpheresMono;
import etomica.util.Default;
import etomica.util.EtomicaObjectInputStream;

public class HSMD3DNoNbr extends Simulation {

    public Phase phase;
    public IntegratorHard integrator;
    public SpeciesSpheresMono species;
    public P2HardSphere potential;
    
    public HSMD3DNoNbr() {
        this(new Default());
    }
    
    public HSMD3DNoNbr(Default defaults) {
        this(Space3D.getInstance(), defaults);
    }
    private HSMD3DNoNbr(Space space, Default defaults) {
        super(space, true, new PotentialMaster(space), Default.BIT_LENGTH, defaults);

        int numAtoms = 256;
        defaults.makeLJDefaults();
        defaults.atomSize = 1.0;
        defaults.boxSize = 14.4573*Math.pow((numAtoms/2020.0),1.0/3.0);

        integrator = new IntegratorHard(this);
        integrator.setIsothermal(false);
        integrator.setTimeStep(0.01);

        ActivityIntegrate activityIntegrate = new ActivityIntegrate(this,integrator);
        activityIntegrate.setDoSleep(true);
        activityIntegrate.setSleepPeriod(1);
        getController().addAction(activityIntegrate);
        species = new SpeciesSpheresMono(this);
        species.setNMolecules(numAtoms);
        potential = new P2HardSphere(this);
        this.potentialMaster.setSpecies(potential,new Species[]{species,species});

        phase = new Phase(this);
//        phase.setBoundary(new BoundaryTruncatedOctahedron(space));
        integrator.addPhase(phase);
        integrator.addListener(new PhaseImposePbc(phase));
        new ConfigurationLattice(new LatticeCubicFcc()).initializeCoordinates(phase);
        
        //ColorSchemeByType.setColor(speciesSpheres0, java.awt.Color.blue);

 //       MeterPressureHard meterPressure = new MeterPressureHard(integrator);
 //       DataAccumulator accumulatorManager = new DataAccumulator(meterPressure);
        // 	DisplayBox box = new DisplayBox();
        // 	box.setDatumSource(meterPressure);
 //       phase.setDensity(0.7);
    } //end of constructor

    public static void main( String[] args )
    {
    	String filename = "test.bin";
		
    	try
    	{
    	    FileOutputStream fos = null;
    	    ObjectOutputStream out = null;
    	    HSMD3DNoNbr simulation = new HSMD3DNoNbr();
    	    fos = new FileOutputStream( filename);
			out = new ObjectOutputStream(fos);
			out.writeObject( simulation );
			out.close();
			fos.close();
			System.out.println( "Serialization of class HSMD3DNoNbr succeeded.");
    	}
    	catch(IOException ex)
    	{
    	    System.err.println( "Exception:" + ex.getMessage() );
    	    ex.printStackTrace();
    	}
    	
    	// Serialize back
    	Simulation simulation = null;
    	try
    	{
    	    FileInputStream fis = null;
    	    EtomicaObjectInputStream in = null;
    	    fis = new FileInputStream(filename);
    	    in = new EtomicaObjectInputStream(fis);
    	    simulation = (etomica.simulation.Simulation) in.readObject();
    	    AtomList.rebuildAllLists(in);
    	    in.close();
    	    fis.close();
    	    
    	    System.out.println( "DeSerialization of class HSMD3DNoNbr succeeded.");

    	}
    	catch( Exception ex ) {
    	    System.err.println( "Could not read simulation from file " + filename + ". Cause: " + ex.getMessage() );
    	    ex.printStackTrace();
    	}
		
	    // go daddy
	    simulation.getController().run();
	    System.out.println( "Simulation run ok");
		
    }
}//end of class
