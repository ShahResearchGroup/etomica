/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.interfacial;

import java.util.List;

import etomica.action.activity.ActivityIntegrate;
import etomica.api.IAtomList;
import etomica.api.IAtomType;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomSourceRandomSpecies;
import etomica.atom.DiameterHashByType;
import etomica.box.Box;
import etomica.chem.elements.ElementSimple;
import etomica.data.AccumulatorAverage;
import etomica.data.AccumulatorAverageFixed;
import etomica.data.AccumulatorHistory;
import etomica.data.DataDump;
import etomica.data.DataFork;
import etomica.data.DataPump;
import etomica.data.DataPumpListener;
import etomica.data.DataSourceCountSteps;
import etomica.data.meter.MeterNMolecules;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.meter.MeterProfileByVolume;
import etomica.graphics.DisplayPlot;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorMC;
import etomica.integrator.IntegratorMD.ThermostatType;
import etomica.integrator.mcmove.MCMoveAtom;
import etomica.integrator.mcmove.MCMoveStepTracker;
import etomica.nbr.list.PotentialMasterList;
import etomica.potential.P2LennardJones;
import etomica.potential.P2SoftSphericalTruncatedForceShifted;
import etomica.potential.Potential1;
import etomica.simulation.Simulation;
import etomica.space.BoundaryRectangularSlit;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;
import etomica.util.HistoryCollapsingAverage;
import etomica.util.ParameterBase;
import etomica.util.ParseArgs;
import etomica.util.RandomNumberGenerator;

/**
 * Simple Lennard-Jones molecular dynamics simulation in 3D
 */
 
public class LJMD extends Simulation {
    
    public final PotentialMasterList potentialMaster;
    public final ActivityIntegrate ai;
    public IntegratorFixedWall integrator;
    public SpeciesSpheresMono speciesFluid, speciesTopWall, speciesBottomWall;
    public IBox box;
    public P2SoftSphericalTruncatedForceShifted pFF, pTW, pBW;
    public ConfigurationLammps config;

    public LJMD(double temperature, double tStep, boolean fixedWall, double spring, double springPosition, double Psat, int hybridInterval, int mcSteps, String lammpsFile) {
        super(Space3D.getInstance());
        setRandom(new RandomNumberGenerator(2));
        BoundaryRectangularSlit boundary = new BoundaryRectangularSlit(2, space);
        box = new Box(boundary, space);
        addBox(box);
        
        speciesFluid = new SpeciesSpheresMono(space, new ElementSimple("F"));
        speciesFluid.setIsDynamic(true);
        addSpecies(speciesFluid);
        speciesTopWall = new SpeciesSpheresMono(space, new ElementSimple("TW",fixedWall?Double.POSITIVE_INFINITY:1));
        speciesTopWall.setIsDynamic(true);
        addSpecies(speciesTopWall);
        speciesBottomWall = new SpeciesSpheresMono(space, new ElementSimple("BW",Double.POSITIVE_INFINITY));
        speciesBottomWall.setIsDynamic(true);
        addSpecies(speciesBottomWall);
        
        config = new ConfigurationLammps(space, lammpsFile, speciesTopWall, speciesBottomWall, speciesFluid);
        config.setTopPadding(5);
        config.initializeCoordinates(box);
        
        potentialMaster = new PotentialMasterList(this, 1.2*5.49925, space);
        potentialMaster.setCellRange(2);
        integrator = new IntegratorFixedWall(potentialMaster, random, tStep, temperature, space);
        integrator.setIsothermal(true);
        integrator.setTemperature(temperature);
        integrator.setThermostat(hybridInterval > 0 ? ThermostatType.HYBRID_MC : ThermostatType.ANDERSEN);
        integrator.setThermostatInterval(hybridInterval > 0 ? hybridInterval : 2000);
        
        ai = new ActivityIntegrate(integrator);
        getController().addAction(ai);

        pFF = new P2SoftSphericalTruncatedForceShifted(space, new P2LennardJones(space, 1.0, 1.0), 2.5);
        IAtomType leafType = speciesFluid.getLeafType();
        potentialMaster.addPotential(pFF,new IAtomType[]{leafType,leafType});

        pBW = new P2SoftSphericalTruncatedForceShifted(space, new P2LennardJones(space, 1.09985, 0.4), 5.49925);
        potentialMaster.addPotential(pBW,new IAtomType[]{leafType,speciesBottomWall.getLeafType()});
        
        pTW = new P2SoftSphericalTruncatedForceShifted(space, new P2LennardJones(space, 1.5, 0.1), 1.68);
        potentialMaster.addPotential(pTW,new IAtomType[]{leafType,speciesTopWall.getLeafType()});

        integrator.setBox(box);
        
        if (!fixedWall) {
            FixedWall fixedWallListener = new FixedWall(space, box, integrator.getAgentManager(), speciesTopWall);
            integrator.setFixedWall(fixedWallListener);
            int nWall = box.getMoleculeList(speciesTopWall).getMoleculeCount();
            double Lz = boundary.getBoxSize().getX(2);
            double Lxy = boundary.getBoxSize().getX(0);
            P1Wall p1Wall = new P1Wall(space, spring/nWall, springPosition-0.5*Lz, Psat*Lxy*Lxy/nWall);
            potentialMaster.addPotential(p1Wall, new IAtomType[]{speciesTopWall.getLeafType()});
        }
        
        if (mcSteps > 0 && hybridInterval > 0) {
            IntegratorMC integratorMC = new IntegratorMC(this, potentialMaster);
            integratorMC.setTemperature(temperature);
            MCMoveAtomNbr mcMoveAtomBig = new MCMoveAtomNbr(random, potentialMaster, space);
            mcMoveAtomBig.setAtomSource(new AtomSourceRandomSpecies(getRandom(), speciesFluid));
            mcMoveAtomBig.setStepSize(0.5*config.getLxy());
            ((MCMoveStepTracker)mcMoveAtomBig.getTracker()).setTunable(false);
            
            integratorMC.getMoveManager().addMCMove(mcMoveAtomBig);

            integrator.setIntegratorMC(integratorMC, mcSteps);
            
            Potential1 p1F = new Potential1(space) {
                
                public double energy(IAtomList atoms) {
                    double pz = atoms.getAtom(0).getPosition().getX(2);
                    double zMin = -0.5*boundary.getBoxSize().getX(2);
                    double zMax = box.getMoleculeList(speciesTopWall).getMolecule(0).getChildList().getAtom(0).getPosition().getX(2);
                    return (pz < zMin || pz > zMax) ? Double.POSITIVE_INFINITY : 0;
                }
            };
            potentialMaster.addPotential(p1F,new IAtomType[]{leafType});
        }

        integrator.getEventManager().addListener(potentialMaster.getNeighborManager(box));
    }
    
    public static void main(String[] args) {

        LJMDParams params = new LJMDParams();
        ParseArgs.doParseArgs(params, args);
        if (args.length==0) {
            params.graphics = true;
            params.lammpsFile = "eq.data";
            params.steps = 10000;
            params.T = 0.8;
            params.fixedWall = false;
            params.springPosition = 79;
            params.hybridInterval = 100;
            params.tStep = 0.01;
            params.mcSteps = 1000;
        }

        final double temperature = params.T;
        long steps = params.steps;
        double tStep = params.tStep;
        boolean fixedWall = params.fixedWall;
        double spring = params.spring;
        double springPosition = params.springPosition;
        double Psat = params.Psat;
        int hybridInterval = params.hybridInterval;
        int mcSteps = params.mcSteps;
        String lammpsFile = params.lammpsFile;
        boolean graphics = params.graphics;

        if (!graphics) {
            System.out.println("Running MC with T="+temperature);
            System.out.println(steps+" steps");
        }

        final LJMD sim = new LJMD(temperature, tStep, fixedWall, spring, springPosition, Psat, hybridInterval, mcSteps, lammpsFile);

        int dataInterval = 10;
        if (hybridInterval > 0) {
            dataInterval = (dataInterval/hybridInterval)*hybridInterval;
            if (dataInterval == 0) dataInterval = hybridInterval;
        }
        
        MeterPotentialEnergy meterPE = new MeterPotentialEnergy(sim.potentialMaster);
        meterPE.setBox(sim.box);
        DataFork forkPE = new DataFork();
        DataPumpListener pumpPE = new DataPumpListener(meterPE, forkPE, dataInterval);
        sim.integrator.getEventManager().addListener(pumpPE);

        MeterWallForce meterWF = new MeterWallForce(sim.space, sim.potentialMaster, sim.box, sim.speciesTopWall);
        DataFork forkWF = new DataFork();
        DataPumpListener pumpWF = new DataPumpListener(meterWF, forkWF, dataInterval);
        sim.integrator.getEventManager().addListener(pumpWF);
        
        MeterWallPosition meterWP = new MeterWallPosition(sim.box, sim.speciesTopWall);
        DataFork forkWP = new DataFork();
        DataPumpListener pumpWP = new DataPumpListener(meterWP, forkWP, dataInterval);
        sim.integrator.getEventManager().addListener(pumpWP);
        
        MeterPotentialEnergy meterPE2 = new MeterPotentialEnergy(sim.potentialMaster);
        meterPE2.setBox(sim.box);
        double u = meterPE2.getDataAsScalar();
        System.out.println("Potential energy: "+u);
        System.out.println("Wall force: "+meterWF.getDataAsScalar());
        
        MeterProfileByVolume densityProfileMeter = new MeterProfileByVolume(sim.space);
        densityProfileMeter.setProfileDim(2);
        densityProfileMeter.setBox(sim.box);
        MeterNMolecules meterNMolecules = new MeterNMolecules();
        meterNMolecules.setSpecies(sim.speciesFluid);
        densityProfileMeter.setDataSource(meterNMolecules);
        AccumulatorAverageFixed densityProfileAvg = new AccumulatorAverageFixed(10);
        densityProfileAvg.setPushInterval(10);
        DataPumpListener profilePump = new DataPumpListener(densityProfileMeter, densityProfileAvg, dataInterval);
        DataDump profileDump = new DataDump();
        densityProfileAvg.addDataSink(profileDump, new AccumulatorAverage.StatType[]{densityProfileAvg.AVERAGE});
        sim.integrator.getEventManager().addListener(profilePump);
        
        
        if (graphics) {
            final String APP_NAME = "LJMD";
            final SimulationGraphic simGraphic = new SimulationGraphic(sim, SimulationGraphic.TABBED_PANE, APP_NAME, 3, sim.getSpace(), sim.getController());

            List<DataPump> dataStreamPumps = simGraphic.getController().getDataStreamPumps();
            dataStreamPumps.add(profilePump);

            simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));

            simGraphic.makeAndDisplayFrame(APP_NAME);
            DiameterHashByType dh = (DiameterHashByType)simGraphic.getDisplayBox(sim.box).getDiameterHash();
            dh.setDiameter(sim.speciesFluid.getLeafType(), 1.0);
            dh.setDiameter(sim.speciesBottomWall.getLeafType(), 1.09885);
            dh.setDiameter(sim.speciesTopWall.getLeafType(), 1.5);
            
            DataSourceCountSteps dsSteps = new DataSourceCountSteps(sim.integrator);

            AccumulatorHistory historyPE = new AccumulatorHistory(new HistoryCollapsingAverage());
            historyPE.setTimeDataSource(dsSteps);
            forkPE.addDataSink(historyPE);
            DisplayPlot plotPE = new DisplayPlot();
            historyPE.setDataSink(plotPE.getDataSet().makeDataSink());
            plotPE.setLabel("PE");
            simGraphic.add(plotPE);
            
            AccumulatorHistory historyWF = new AccumulatorHistory(new HistoryCollapsingAverage());
            historyWF.setTimeDataSource(dsSteps);
            forkWF.addDataSink(historyWF);
            DisplayPlot plotWF = new DisplayPlot();
            historyWF.setDataSink(plotWF.getDataSet().makeDataSink());
            plotWF.setLabel("Force");
            simGraphic.add(plotWF);

            if (!fixedWall) {
                AccumulatorHistory historyWP = new AccumulatorHistory(new HistoryCollapsingAverage());
                historyWP.setTimeDataSource(dsSteps);
                forkWP.addDataSink(historyWP);
                DisplayPlot plotWP = new DisplayPlot();
                historyWP.setDataSink(plotWP.getDataSet().makeDataSink());
                plotWP.setLabel("Position");
                simGraphic.add(plotWP);
            }
            
            DisplayPlot profilePlot = new DisplayPlot();
            densityProfileAvg.addDataSink(profilePlot.getDataSet().makeDataSink(), new AccumulatorAverage.StatType[]{densityProfileAvg.AVERAGE});
            profilePlot.setLabel("density");
            simGraphic.add(profilePlot);
            
            return;
        }

        long bs = steps/100000;
        if (bs==0) bs=1;
        AccumulatorAverageFixed accPE = new AccumulatorAverageFixed(bs);
        forkPE.addDataSink(accPE);
        AccumulatorAverageFixed accWF = new AccumulatorAverageFixed(bs);
        forkWF.addDataSink(accWF);

        sim.ai.setMaxSteps(steps);
        sim.getController().actionPerformed();
        
        u = meterPE2.getDataAsScalar();
        System.out.println("Potential energy: "+u);
        System.out.println("Wall force: "+meterWF.getDataAsScalar());
        
        double avgPE = accPE.getData().getValue(accPE.AVERAGE.index);
        double errPE = accPE.getData().getValue(accPE.ERROR.index);
        double corPE = accPE.getData().getValue(accPE.BLOCK_CORRELATION.index);
        double avgWF = accWF.getData().getValue(accPE.AVERAGE.index);
        double errWF = accWF.getData().getValue(accPE.ERROR.index);
        double corWF = accWF.getData().getValue(accPE.BLOCK_CORRELATION.index);
        
        if (steps>100000) {
            System.out.println(String.format("Average potential energy: %25.15e %10.4e % 5.3f\n",avgPE,errPE,corPE));
            System.out.println(String.format("Average wall force: %25.15e %10.4e % 5.3f\n",avgWF,errWF,corWF));
        }
        else {
            System.out.println("Average potential energy: "+avgPE);
            System.out.println("Average wall force: "+avgWF);
        }
        
        WriteConfigurationInterfacial configWriter = new WriteConfigurationInterfacial(sim.space);
        configWriter.setSpecies(sim.speciesFluid);
        IVectorMutable unshift = sim.space.makeVector();
        unshift.Ea1Tv1(-1, sim.config.getShift());
        configWriter.setShift(unshift);
        configWriter.setBox(sim.box);
        configWriter.setFileName("xyz_000.dat");
        configWriter.actionPerformed();
    }
    
    public static class LJMDParams extends ParameterBase {
        public double T = 2.0;
        public long steps = 10000;
        public double tStep = 0.001;
        public boolean graphics = false;
        public String lammpsFile = "";
        public boolean fixedWall = true;
        public double spring = 0.3;
        public double springPosition = 70;
        public double Psat = 0.030251;
        public int hybridInterval = 0;
        public int mcSteps = 0;
    }
}
