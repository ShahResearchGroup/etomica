package etomica.normalmode;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IVectorMutable;
import etomica.atom.DiameterHashByType;
import etomica.box.Box;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.AccumulatorHistory;
import etomica.data.DataFork;
import etomica.data.DataPumpListener;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.meter.MeterVolume;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.integrator.mcmove.MCMove;
import etomica.integrator.mcmove.MCMoveRotateMolecule3D;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.integrator.mcmove.MCMoveVolume;
import etomica.lattice.crystal.Basis;
import etomica.lattice.crystal.BasisHcpBaseCentered;
import etomica.lattice.crystal.Primitive;
import etomica.lattice.crystal.PrimitiveMonoclinic;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2HardSphere;
import etomica.simulation.Simulation;
import etomica.space.Boundary;
import etomica.space.BoundaryDeformablePeriodic;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.units.Degree;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;

/**
 * NPT simulation for hard sphere solid using an MCMove that does coordinate
 * scaling with volume changes.
 */
public class HSDimerNPT extends Simulation {
    
    private static final long serialVersionUID = 1L;
    public final PotentialMasterList potentialMaster;
    public final IntegratorMC integrator;
    public final SpeciesHSDimer species;
    public final IBox box;
    public final ActivityIntegrate activityIntegrate;
    public final CoordinateDefinitionHSDimer coordinateDefinition;

    public HSDimerNPT(Space space, int numMolecules, double rho, int[] nC) {
        super(space);
        
        if(false){
	        IVectorMutable aVec = space.makeVector(new double[]{-0.5000051767896645, 0.288672284963261, 0.816494418277053});
	        IVectorMutable bVec = space.makeVector(new double[]{0.0, 0.0, 1.0});
	        aVec.normalize();
	
	        IVectorMutable temp = space.makeVector();
	        temp.E(aVec);
	        temp.XE(bVec);
	        temp.normalize();
	
	        bVec.XE(temp);
	        bVec.normalize();
	
	        IVectorMutable cVec = space.makeVector();
	        cVec.Ea1Tv1(Math.sqrt(aVec.squared()), bVec);
	        cVec.normalize();
	        System.out.println("cVector: " + cVec.toString());
	
	        IVectorMutable xVec = space.makeVector(new double[]{1.0, 0.0, 0.0});
	        System.out.println("beta: "  + Degree.UNIT.fromSim(Math.acos(aVec.dot(cVec))));
	        System.out.println("alpha: " + Degree.UNIT.fromSim(Math.acos(cVec.dot(xVec))));
	
	
	        System.exit(1);
        }
        potentialMaster = new PotentialMasterList(this, space);
        
        double a = 2*Math.sqrt(3.0/4.0);//5.196260909541926/2.999;
        double b = 1.0+1e-9;//3.000070097339371/2.999;
        double c = 1.42804117195;//4.899084514727896/3.06;
        
        System.out.println("a: " + a);
        System.out.println("b: " + b);
        System.out.println("c: " + c);
				
		double arcsinAngle =  Math.PI/2 - Math.asin(0.6/Math.sqrt(3.0)); //
		System.out.println("ArcsinAngle: " + Degree.UNIT.fromSim(arcsinAngle));
		double angle = 0.0;//Degree.UNIT.toSim(180-150.0);//150.0033316180124; 
		System.out.println("angle: "+ Degree.UNIT.fromSim(angle));

		double sigma = 1.0;
        potentialMaster.setCellRange(2);
        potentialMaster.setRange(2.5);
        integrator = new IntegratorMC(potentialMaster, getRandom(), 1.0);
        activityIntegrate = new ActivityIntegrate(integrator);
        getController().addAction(activityIntegrate);
        
        species = new SpeciesHSDimer(space, true);
        addSpecies(species);

        P2HardSphere p2 = new P2HardSphere(space, sigma, false);
        potentialMaster.addPotential(p2, new IAtomType[]{species.getDimerAtomType(), species.getDimerAtomType()});
    
        box = new Box(space);
        addBox(box);
        box.setNMolecules(species, numMolecules);
        
        double boxAngle = Degree.UNIT.toSim(105);
        IVectorMutable[] boxDim = new IVectorMutable[3];
        boxDim[0] = space.makeVector(new double[]{nC[0]*a, 0.0, 0.0});
        boxDim[1] = space.makeVector(new double[]{0.0, nC[1]*b, 0.0});
        boxDim[2] = space.makeVector(new double[]{
        		nC[2]*c*Math.cos(boxAngle), 
        		0.0, 
        		nC[2]*c*Math.sin(boxAngle)});
        
		Primitive primitive = new PrimitiveMonoclinic(space, nC[0]*a, nC[1]*b, nC[2]*c, 
				boxAngle);
		
		Boundary boundary = new BoundaryDeformablePeriodic(space, boxDim);
        Basis basisHCPBase = new BasisHcpBaseCentered();
        Basis basis = new BasisBigCell(space, basisHCPBase, new int[]{nC[0], nC[1], nC[2]});
		box.setBoundary(boundary);
		
		for (int i=0; i<3; i++){
			System.out.println("boxDim["+i+"]: " + boxDim[i]);
		}

        coordinateDefinition = new CoordinateDefinitionHSDimer(this, box, primitive, basis, space);
        coordinateDefinition.setRotationAngle(angle);
        coordinateDefinition.setArcSinAngle(arcsinAngle);
        coordinateDefinition.setOrientationVectorCP2();
        coordinateDefinition.initializeCoordinates(new int[]{1,1,1});
        integrator.setBox(box);

        potentialMaster.getNeighborManager(box).reset();
        
        MeterPotentialEnergy meterPE = new MeterPotentialEnergy(potentialMaster);
        meterPE.setBox(box);
       
        MCMoveMoleculeCoupled mcMove = new MCMoveMoleculeCoupled(potentialMaster,getRandom(),space);
        mcMove.setBox(box);
        mcMove.setDoExcludeNonNeighbors(true);
        //mcMove.setStepSize(0.01);
        integrator.getMoveManager().addMCMove(mcMove);

//        MCMoveRotateMolecule3D rotate = new MCMoveRotateMolecule3D(potentialMaster, getRandom(), space);
//        rotate.setBox(box);
//        integrator.getMoveManager().addMCMove(rotate);
         
        /*
         * for L*=0.6; d= (1792/1000)^(1/3) = (1.792)^(1/3)
         * 
         * p* = pd^3/kT, kT=1.0
         * p = (p*)/ d^3
         */
        double d3 = 1.792;
        double p = 50e5/d3;
        
        MCMove mcMoveVolume;
        if (false) {
        	// fancy move
            mcMoveVolume = new MCMoveVolumeSolidNPTMolecular(potentialMaster, coordinateDefinition, getRandom(), space, p);
            ((MCMoveVolumeSolidNPTMolecular)mcMoveVolume).setTemperature(1.0);
        }
        else {
            // standard move
            mcMoveVolume = new MCMoveVolume(potentialMaster, getRandom(), space, p);
        }
        ((MCMoveStepTracker)mcMoveVolume.getTracker()).setNoisyAdjustment(true);
        integrator.getMoveManager().addMCMove(mcMoveVolume);
        
        MCMoveVolumeMonoclinic mcMoveVolMonoclinic = new MCMoveVolumeMonoclinic(potentialMaster, getRandom(), space, p);
        mcMoveVolMonoclinic.setBox(box);
        mcMoveVolMonoclinic.setStepSize(0.001);
        ((MCMoveStepTracker)mcMoveVolMonoclinic.getTracker()).setNoisyAdjustment(true);
        integrator.getMoveManager().addMCMove(mcMoveVolMonoclinic);
        
        MCMoveVolumeMonoclinicAngle mcMoveVolMonoclinicAngle = new MCMoveVolumeMonoclinicAngle(potentialMaster, getRandom(), space, p, box);
        mcMoveVolMonoclinicAngle.setBox(box);
        mcMoveVolMonoclinicAngle.setStepSize(0.001);
        ((MCMoveStepTracker)mcMoveVolMonoclinicAngle.getTracker()).setNoisyAdjustment(true);
        integrator.getMoveManager().addMCMove(mcMoveVolMonoclinicAngle);
    }
    
    /**
     * Demonstrates how this class is implemented.
     */
    public static void main(String[] args) {
        HSMD3DParameters params = new HSMD3DParameters();
        ParseArgs parseArgs = new ParseArgs(params);
        parseArgs.parseArgs(args);
        
        int[] nC = params.nC;
        int numMolecules = nC[0]*nC[1]*nC[2]*2;
        HSDimerNPT sim = new HSDimerNPT(Space3D.getInstance(), numMolecules, params.rho, params.nC);
        
//        if(true){
//			SimulationGraphic simGraphic = new SimulationGraphic(sim, sim.space, sim.getController());
//		    simGraphic.getDisplayBox(sim.box).setPixelUnit(new Pixel(10));
//						
//			DiameterHashByType diameter = new DiameterHashByType(sim);
//			diameter.setDiameter(sim.species.getDimerAtomType(), 1.0);
//			simGraphic.getDisplayBox(sim.box).setDiameterHash(diameter);
//			
//			simGraphic.makeAndDisplayFrame("HS Dumbbell Crystal Structure");
//
//		    return;
//		    
//        }
        
        MeterVolume meterVolume = new MeterVolume();
        meterVolume.setBox(sim.box);
        DataFork volumeFork = new DataFork();
        DataPumpListener volumePump = new DataPumpListener(meterVolume, volumeFork, numMolecules);
        sim.integrator.getEventManager().addListener(volumePump);
        AccumulatorAverageCollapsing volumeAvg = new AccumulatorAverageCollapsing();
        volumeFork.addDataSink(volumeAvg);
        
//        MeterDisplacement meterDisplacement = new MeterDisplacement(sim.space, sim.coordinateDefinition, 0.001);
//        sim.integrator.getEventManager().addListener(new IntegratorListenerAction(meterDisplacement, params.numAtoms));

//        MeterDisplacementRMS meterDisplacementRMS = new MeterDisplacementRMS(sim.space, sim.coordinateDefinition, 0.001);
//        AccumulatorAverageFixed displacementAvg = new AccumulatorAverageFixed();
//        DataPumpListener displacementAvgPump = new DataPumpListener(meterDisplacementRMS, displacementAvg, params.numAtoms);
//        sim.integrator.getEventManager().addListener(displacementAvgPump);
//
//        MeterDisplacementMax meterDisplacementMax = new MeterDisplacementMax(sim.space, sim.coordinateDefinition, 0.001);
//        AccumulatorAverageFixed displacementMax = new AccumulatorAverageFixed();
//        DataPumpListener displacementMaxPump = new DataPumpListener(meterDisplacementMax, displacementMax, params.numAtoms);
//        sim.integrator.getEventManager().addListener(displacementMaxPump);
//
//        MeterMaxExpansion meterMaxExpansion = new MeterMaxExpansion(sim.space, sim.box, sim.potentialMaster.getNeighborManager(sim.box));
//        AccumulatorAverageFixed maxExpansionAvg = new AccumulatorAverageFixed();
//        DataPumpListener maxExpansionPump = new DataPumpListener(meterMaxExpansion, maxExpansionAvg, params.numAtoms);
//        sim.integrator.getEventManager().addListener(maxExpansionPump);

        if (true) {
            SimulationGraphic graphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, sim.getSpace(), sim.getController());
			DiameterHashByType diameter = new DiameterHashByType(sim);
			diameter.setDiameter(sim.species.getDimerAtomType(), 1.0);
			graphic.getDisplayBox(sim.box).setDiameterHash(diameter);
			
            AccumulatorHistory densityHistory = new AccumulatorHistory(new HistoryCollapsingAverage());
//            densityHistory.setPushInterval();
            volumeFork.addDataSink(densityHistory);
            DisplayPlot densityPlot = new DisplayPlot();
            densityHistory.setDataSink(densityPlot.getDataSet().makeDataSink());
            densityPlot.setLabel("volume");
            graphic.add(densityPlot);
            
            graphic.makeAndDisplayFrame();
            
//            sim.activityIntegrate.setMaxSteps(10000000);
//            sim.getController().actionPerformed();
            
            System.out.println("a: " + sim.box.getBoundary().getEdgeVector(0).toString());
            System.out.println("b: " + sim.box.getBoundary().getEdgeVector(1).toString());
            System.out.println("c: " + sim.box.getBoundary().getEdgeVector(2).toString());
            
            IVectorMutable vec1 = Space3D.makeVector(3);
            IVectorMutable vec2 = Space3D.makeVector(3);
           
            vec1.E(sim.box.getMoleculeList().getMolecule(0).getChildList().getAtom(0).getPosition());
            vec1.ME(sim.box.getMoleculeList().getMolecule(0).getChildList().getAtom(1).getPosition());
            System.out.println("vec1: " + vec1.toString());
            
            vec2.E(sim.box.getMoleculeList().getMolecule(0).getChildList().getAtom(1).getPosition());
            vec2.ME(sim.box.getMoleculeList().getMolecule(0).getChildList().getAtom(0).getPosition());
            System.out.println("vec2: " + vec2.toString());
      
            return;
        }
//        sim.activityIntegrate.setMaxSteps(params.numSteps/10);
//        sim.activityIntegrate.actionPerformed();
//        volumeAvg.reset();
//        System.out.println("equilibration finished");
//        sim.activityIntegrate.setMaxSteps(params.numSteps);
//        sim.activityIntegrate.actionPerformed();
////        try {
////            FileWriter fw = new FileWriter("disp"+params.rho+".dat");
////            IData data = meterDisplacement.getData();
////            IData xData = meterDisplacement.getIndependentData(0);
////            for (int i=0; i<data.getLength(); i++) {
////                fw.write(xData.getValue(i)+" "+data.getValue(i)+"\n");
////            }
////            fw.close();
////        }
////        catch (IOException e) {
////            throw new RuntimeException(e);
////        }
//        
//        double vavg = volumeAvg.getData().getValue(AccumulatorAverage.StatType.AVERAGE.index);
//        double verr = volumeAvg.getData().getValue(AccumulatorAverage.StatType.ERROR.index);
//        double vstdev = volumeAvg.getData().getValue(AccumulatorAverage.StatType.STANDARD_DEVIATION.index);
//        System.out.println("avg density "+numMolecules/vavg+" "+numMolecules/(vavg*vavg)*verr);
//        System.out.println("avg volume "+vavg+" stdev "+vstdev);
//
////        double davg = displacementAvg.getData().getValue(AccumulatorAverage.StatType.AVERAGE.index);
////        double dstdev = displacementAvg.getData().getValue(AccumulatorAverage.StatType.STANDARD_DEVIATION.index);
////        System.out.println("displacement avg "+davg+" stdev "+dstdev);
////
////        double dmaxavg = displacementMax.getData().getValue(AccumulatorAverage.StatType.AVERAGE.index);
////        double dmaxstdev = displacementMax.getData().getValue(AccumulatorAverage.StatType.STANDARD_DEVIATION.index);
////        System.out.println("displacement max avg "+dmaxavg+" stdev "+dmaxstdev);
////
////        double emaxavg = maxExpansionAvg.getData().getValue(AccumulatorAverage.StatType.AVERAGE.index);
////        double emaxstdev = maxExpansionAvg.getData().getValue(AccumulatorAverage.StatType.STANDARD_DEVIATION.index);
////        System.out.println("max expansion avg "+emaxavg+" stdev "+emaxstdev);
    }
    
    public static class HSMD3DParameters extends ParameterBase {
        public double rho = 1.2;
        public int[] nC = new int[]{2,2,2};
        public long numSteps = 10000000;
    }
}
