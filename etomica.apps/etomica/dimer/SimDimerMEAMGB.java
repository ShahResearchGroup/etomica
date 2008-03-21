package etomica.dimer;

import etomica.action.XYZWriter;
import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomTypeLeaf;
import etomica.api.IBox;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.AtomTypeSphere;
import etomica.box.Box;
import etomica.chem.elements.Tin;
import etomica.config.GrainBoundaryTiltConfiguration;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.DataPump;
import etomica.data.AccumulatorAverage.StatType;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.lattice.BravaisLatticeCrystal;
import etomica.lattice.crystal.BasisBetaSnA5;
import etomica.lattice.crystal.PrimitiveTetragonal;
import etomica.meam.ParameterSetMEAM;
import etomica.meam.PotentialMEAM;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.units.Kelvin;
import etomica.util.HistoryCollapsingAverage;

/**
 * Simulation using Henkelman's Dimer method to find a saddle point for
 * an adatom of Sn on a surface, modeled with MEAM.
 * 
 * @author msellers
 *
 */

public class SimDimerMEAMGB extends Simulation{

    private static final long serialVersionUID = 1L;
    private static final String APP_NAME = "DimerMEAMadatomSn";
    public final PotentialMaster potentialMaster;
    public IntegratorVelocityVerlet integratorMD;
    public IntegratorDimerRT integratorDimer;
    public IBox box;
    public double clusterRadius;
    public IVector [] saddle;
    public SpeciesSpheresMono fixed, movable, dimer;
    public PotentialMEAM potential;
    public PotentialCalculationForcePressureSumGB pcGB;
    public ActivityIntegrate activityIntegrateMD, activityIntegrateDimer;
    public int [] millerPlane;
    

    
    public SimDimerMEAMGB(String file, int[] millerPlane) {
    	super(Space3D.getInstance(), true);
    	
        potentialMaster = new PotentialMaster(space);
        
        integratorMD = new IntegratorVelocityVerlet(this, potentialMaster, space);
        
        
        integratorMD.setTimeStep(0.001);
        integratorMD.setTemperature(Kelvin.UNIT.toSim(295));
        integratorMD.setThermostatInterval(100);
        integratorMD.setIsothermal(true);
        
        activityIntegrateMD = new ActivityIntegrate(integratorMD);
        
        //Sn
        
        //Tin tinFixed = new Tin("SnFix", Double.POSITIVE_INFINITY);
        
        fixed = new SpeciesSpheresMono(this, space, Tin.INSTANCE);
        movable = new SpeciesSpheresMono(this, space, Tin.INSTANCE);
        dimer = new SpeciesSpheresMono(this, space, Tin.INSTANCE);
        
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        getSpeciesManager().addSpecies(dimer);
        
        
        ((AtomTypeSphere)fixed.getLeafType()).setDiameter(3.022); 
        ((AtomTypeSphere)movable.getLeafType()).setDiameter(3.022);
        ((AtomTypeSphere)dimer.getLeafType()).setDiameter(3.022);
        
        
        //Ag
        //Silver silverFixed = new Silver("AgFix", Double.POSITIVE_INFINITY);
        /**
        fixed = new SpeciesSpheresMono(this, Silver.INSTANCE);
        movable = new SpeciesSpheresMono(this, Silver.INSTANCE);
        
        getSpeciesManager().addSpecies(fixed);
        getSpeciesManager().addSpecies(movable);
        
        ((AtomTypeSphere)fixed.getLeafType()).setDiameter(2.8895); 
        ((AtomTypeSphere)movable.getLeafType()).setDiameter(2.8895);
        */
        
        /**
        //Cu
        Copper copperFixed = new Copper("CuFix", Double.POSITIVE_INFINITY);
        
        cuFix = new SpeciesSpheresMono(this, copperFixed);
        cu = new SpeciesSpheresMono(this, copperFixed);
        cuAdatom = new SpeciesSpheresMono(this, Copper.INSTANCE);
        movable = new SpeciesSpheresMono(this, Copper.INSTANCE);
        
        getSpeciesManager().addSpecies(cuFix);
        getSpeciesManager().addSpecies(cu);
        getSpeciesManager().addSpecies(cuAdatom);
        getSpeciesManager().addSpecies(movable);
        
        ((AtomTypeSphere)cuFix.getMoleculeType()).setDiameter(2.5561); 
        ((AtomTypeSphere)cu.getMoleculeType()).setDiameter(2.5561); 
        ((AtomTypeSphere)cuAdatom.getMoleculeType()).setDiameter(2.5561);
        ((AtomTypeSphere)movable.getMoleculeType()).setDiameter(2.5561);
         */
        
        box = new Box(new BoundaryRectangularSlit(random, 2, 5, space), space);
        addBox(box);
        
        integratorDimer = new IntegratorDimerRT(this, potentialMaster, new ISpecies[]{dimer}, space);

        /**
        //Ag
        integratorDimer = new IntegratorDimerRT(this, potentialMaster, new Species[]{agAdatom}, "AgAdatom");
         */
        /**
        //Cu
        integratorDimer = new IntegratorDimerRT(this, potentialMaster, new Species[]{cuAdatom}, "CuAdatom");
         */
        integratorDimer.setOrtho(false, false);
        integratorDimer.setFileName(file);
        activityIntegrateDimer = new ActivityIntegrate(integratorDimer);

        // First simulation style
        getController().addAction(activityIntegrateMD);
        // Second simulation style
        getController().addAction(activityIntegrateDimer);
        
        // Sn
        potential = new PotentialMEAM(space);
        potential.setParameters(fixed.getLeafType(), ParameterSetMEAM.Sn);
        potential.setParameters(movable.getLeafType(), ParameterSetMEAM.Sn);
        potential.setParameters(dimer.getLeafType(), ParameterSetMEAM.Sn);
        
        this.potentialMaster.addPotential(potential, new IAtomTypeLeaf[]{fixed.getLeafType(), movable.getLeafType(), dimer.getLeafType()});
        
        /**
        //Ag
        potential = new PotentialMEAM(space);
        
        potential.setParameters(agFix, ParameterSetMEAM.Ag);
        potential.setParameters(ag, ParameterSetMEAM.Ag);
        potential.setParameters(agAdatom, ParameterSetMEAM.Ag);
        potential.setParameters(movable, ParameterSetMEAM.Ag);
        
        this.potentialMaster.addPotential(potential, new Species[]{ag, agFix, agAdatom, movable});
         */
        
        /**
        //Cu
        potential = new PotentialMEAM(space);
        
        potential.setParameters(cuFix, ParameterSetMEAM.Cu);
        potential.setParameters(cu, ParameterSetMEAM.Cu);
        potential.setParameters(cuAdatom, ParameterSetMEAM.Cu);
        potential.setParameters(movable, ParameterSetMEAM.Cu);
        
        this.potentialMaster.addPotential(potential, new Species[]{cu, cuFix, cuAdatom, movable});
         */
    	
        integratorMD.setBox(box);
    	integratorDimer.setBox(box);
    	
    	pcGB = new PotentialCalculationForcePressureSumGB(space, box);
    	
    	integratorMD.setForceSum(pcGB);
    	
    	//Sn
    	//beta-Sn box
        //The dimensions of the simulation box must be proportional to those of
        //the unit cell to prevent distortion of the lattice.  The values for the 
        //lattice parameters for tin's beta box (a = 5.8314 angstroms, c = 3.1815 
        //angstroms) are taken from the ASM Handbook. 
    	double a = 5.92; 
    	double c = 3.23;
    	//box.setDimensions(new Vector3D((Math.sqrt( (4*Math.pow(c, 2))+Math.pow(a,2)))*3, a*3, c*10));
        PrimitiveTetragonal primitive = new PrimitiveTetragonal(space, a, c);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisBetaSnA5());
        GrainBoundaryTiltConfiguration gbtilt = new GrainBoundaryTiltConfiguration(crystal, crystal, new ISpecies[] {fixed, movable}, 4.5, space);
    	
        
        
        /**
        //Ag
        box.setDimensions(new Vector3D(4.0863*4, 4.0863*4, 4.0863*4));
        PrimitiveCubic primitive = new PrimitiveCubic(space, 4.0863);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisCubicFcc());
        GrainBoundaryTiltConfiguration gbtilt = new GrainBoundaryTiltConfiguration(crystal, crystal, new Species[] {fixed, movable}, 4.56, space);
        */
        
        
        
        /**
        //Cu
        box.setDimensions(new Vector3D(3.6148*5, 3.6148*4, 3.6148*4));
        PrimitiveCubic primitive = new PrimitiveCubic(space, 3.6148);
        BravaisLatticeCrystal crystal = new BravaisLatticeCrystal(primitive, new BasisCubicFcc());
        GrainBoundaryTiltConfiguration gbtilt = new GrainBoundaryTiltConfiguration(crystal, crystal, new Species[] {cuFix, cu}, 4.56);
        */

        gbtilt.setFixedSpecies(fixed);
        gbtilt.setMobileSpecies(movable);
        gbtilt.setGBplane(millerPlane);
        gbtilt.setBoxSize(box, new int[] {4,4,12});
        gbtilt.initializeCoordinates(box);
        
        /*
        IVector rij = space.makeVector();
        AtomArrayList movableList = new AtomArrayList();
        IAtomSet loopSet = box.getMoleculeList(movable);
        for (int i=0; i<loopSet.getAtomCount(); i++){
            rij.E(((IAtomPositioned)((IMolecule)loopSet.getAtom(i)).getChildList().getAtom(0)).getPosition());
            if(rij.squared()<15.0){
               movableList.add(loopSet.getAtom(i));
            } 
        }
        for (int i=0; i<movableList.getAtomCount(); i++){
           ((IAtomPositioned)box.addNewMolecule(dimer).getChildList().getAtom(0)).getPosition().E(((IAtomPositioned)((IMolecule)movableList.getAtom(i)).getChildList().getAtom(0)).getPosition());
           box.removeMolecule((IMolecule)movableList.getAtom(i));
        }
        */
    }
    
    public static void main(String[] args){
    	final String APP_NAME = "DimerMEAMadatomGB";
    	final SimDimerMEAMGB sim = new SimDimerMEAMGB("sngb", new int[] {0,3,1});
    	
    	sim.activityIntegrateMD.setMaxSteps(900);
        sim.activityIntegrateDimer.setMaxSteps(1000);
                
        MeterPotentialEnergy energyMeter = new MeterPotentialEnergy(sim.potentialMaster);
        energyMeter.setBox(sim.box);
        
        AccumulatorHistory energyAccumulator = new AccumulatorHistory(new HistoryCollapsingAverage());
        AccumulatorAverageCollapsing accumulatorAveragePE = new AccumulatorAverageCollapsing();
        
        DataPump energyPump = new DataPump(energyMeter,accumulatorAveragePE);       
        accumulatorAveragePE.addDataSink(energyAccumulator, new StatType[]{StatType.MOST_RECENT});
        
        DisplayPlot plotPE = new DisplayPlot();
        plotPE.setLabel("PE Plot");
        
        energyAccumulator.setDataSink(plotPE.getDataSet().makeDataSink());
        accumulatorAveragePE.setPushInterval(1);        
        
        SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME, 1, sim.space);
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        
        XYZWriter xyzwriter = new XYZWriter(sim.box);
        xyzwriter.setFileName("sngb");
        xyzwriter.setIsAppend(true);
        
        simGraphic.add(/*"PE Plot",*/plotPE);
        /*
        //Load in MD minimized configuration
        ConfigurationFile configurationFile = new ConfigurationFile("filename");
        configurationFile.initializeCoordinates(sim.box);
        
        sim.integratorMD.addIntervalAction(energyPump);
        sim.integratorMD.addIntervalAction(simGraphic.getPaintAction(sim.box));
        sim.integratorMD.addIntervalAction(xyzwriter);
        sim.integratorMD.setActionInterval(xyzwriter, 10);
        
        sim.integratorDimer.addIntervalAction(energyPump);
        sim.integratorDimer.addIntervalAction(simGraphic.getPaintAction(sim.box));
        sim.integratorDimer.addIntervalAction(xyzwriter);
        sim.integratorDimer.setActionInterval(xyzwriter, 10);
        */
        
        ColorSchemeByType colorScheme = ((ColorSchemeByType)((DisplayBox)simGraphic.displayList().getFirst()).getColorScheme());
        
        //Sn
        colorScheme.setColor(sim.fixed.getLeafType(),java.awt.Color.blue);
        colorScheme.setColor(sim.movable.getLeafType(),java.awt.Color.gray);
        colorScheme.setColor(sim.dimer.getLeafType(), java.awt.Color.white);
        
        /**
        //Ag
        colorScheme.setColor(sim.ag.getMoleculeType(),java.awt.Color.darkGray);
        colorScheme.setColor(sim.agFix.getMoleculeType(),java.awt.Color.green);
        colorScheme.setColor(sim.agAdatom.getMoleculeType(),java.awt.Color.red);
        colorScheme.setColor(sim.movable.getMoleculeType(),java.awt.Color.PINK);
         */
        
        /**
        //Cu
        colorScheme.setColor(sim.cu.getMoleculeType(),java.awt.Color.yellow);
        colorScheme.setColor(sim.cuFix.getMoleculeType(),java.awt.Color.cyan);
        colorScheme.setColor(sim.cuAdatom.getMoleculeType(),java.awt.Color.red);
        colorScheme.setColor(sim.movable.getMoleculeType(),java.awt.Color.PINK);
         */
        
        simGraphic.makeAndDisplayFrame(APP_NAME);
    }
    
}
