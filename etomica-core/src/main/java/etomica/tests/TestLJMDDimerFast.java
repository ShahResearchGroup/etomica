/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.tests;

import etomica.action.BoxImposePbc;
import etomica.action.BoxInflate;
import etomica.action.activity.ActivityIntegrate;
import etomica.action.activity.Controller;
import etomica.atom.AtomType;
import etomica.atom.iterator.ApiIndexList;
import etomica.box.Box;
import etomica.config.ConfigurationLattice;
import etomica.config.ConformationChainLinear;
import etomica.config.ConformationLinear;
import etomica.data.AccumulatorAverageCollapsing;
import etomica.data.DataPump;
import etomica.data.meter.MeterPotentialEnergy;
import etomica.data.meter.MeterPotentialEnergyFromIntegratorFasterer;
import etomica.graphics.ColorSchemeRandomByMolecule;
import etomica.graphics.DisplayTextBoxesCAE;
import etomica.graphics.SimulationGraphic;
import etomica.integrator.IntegratorListenerAction;
import etomica.integrator.IntegratorVelocityVerlet;
import etomica.integrator.IntegratorVelocityVerletFasterer;
import etomica.lattice.LatticeCubicFcc;
import etomica.nbr.cell.PotentialMasterCellFasterer;
import etomica.nbr.list.PotentialMasterListFasterer;
import etomica.potential.*;
import etomica.simulation.Simulation;
import etomica.space3d.Space3D;
import etomica.species.ISpecies;
import etomica.species.SpeciesSpheres;
import etomica.units.Degree;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Simple Lennard-Jones molecular dynamics simulation in 3D
 */
public class TestLJMDDimerFast extends Simulation {

    public IntegratorVelocityVerletFasterer integrator;
    public SpeciesSpheres species;
    public Box box;
    public P2LennardJones potential;
    public Controller controller;
    public MeterPotentialEnergyFromIntegratorFasterer energy;
    public AccumulatorAverageCollapsing avgEnergy;
    public DataPump pump;


    public TestLJMDDimerFast(int moleculeSize, int totalAtoms, boolean nbrListing) {
        super(Space3D.getInstance());

        species = new SpeciesSpheres(this, space, moleculeSize);
        species.setConformation(new ConformationChainLinear(space, 0.5, new double[]{Degree.UNIT.toSim(45), Degree.UNIT.toSim(45), 0}));
        species.setIsDynamic(true);
        addSpecies(species);

        double sigma = 1.0;
        box = this.makeBox();
        PotentialMasterFasterer potentialMaster = nbrListing ? new PotentialMasterListFasterer(this, box, 2, 4) : new PotentialMasterFasterer(this, box);
        integrator = new IntegratorVelocityVerletFasterer(this, potentialMaster, box);
        integrator.setTimeStep(0.005);
        integrator.setTemperature(2);
        integrator.setIsothermal(true);
        ActivityIntegrate activityIntegrate = new ActivityIntegrate(integrator);
        activityIntegrate.setSleepPeriod(0);
//        activityIntegrate.setMaxSteps(1000);
        getController().addAction(activityIntegrate);
        box.setNMolecules(species, totalAtoms / moleculeSize);
        new BoxInflate(box, space, 0.5).actionPerformed();
        System.out.println("box size: "+box.getBoundary().getBoxSize());

        potential = new P2LennardJones(space, sigma, 1.0);
        AtomType leafType = species.getLeafType();
        P2SoftSphericalTruncatedForceShifted p2 = new P2SoftSphericalTruncatedForceShifted(space, potential, 3.0);
        potentialMaster.setPairPotential(leafType, leafType, p2);

        P2Harmonic pBond = new P2Harmonic(space, 100, 0.51);
        List<int[]> bonds = IntStream.range(0, moleculeSize - 1)
                .mapToObj(i -> new int[]{i, i+1})
                .collect(Collectors.toList());

        potentialMaster.setBondingPotential(species, pBond, bonds);

        BoxImposePbc imposepbc = new BoxImposePbc(space);
        imposepbc.setBox(box);
        integrator.getEventManager().addListener(new IntegratorListenerAction(imposepbc));

        if (nbrListing) {
            integrator.getEventManager().addListener(((PotentialMasterListFasterer) potentialMaster));
        }

        ConfigurationLattice configuration = new ConfigurationLattice(new LatticeCubicFcc(space), space);
        configuration.initializeCoordinates(box);
        integrator.reset();
        energy = new MeterPotentialEnergyFromIntegratorFasterer(integrator);
        System.out.println("u0: "+energy.getDataAsScalar());
        avgEnergy = new AccumulatorAverageCollapsing();
        avgEnergy.setPushInterval(10);
        pump = new DataPump(energy, avgEnergy);
        IntegratorListenerAction pumpListener = new IntegratorListenerAction(pump);
        pumpListener.setInterval(10);
        integrator.getEventManager().addListener(pumpListener);
    }

    public static void main(String[] args) {
        final String APP_NAME = "LJMDDimer";
        final TestLJMDDimerFast sim = new TestLJMDDimerFast(2, 512, true);
//        long t0 = System.nanoTime();
//        sim.getController().actionPerformed();
//        long t1 = System.nanoTime();
//        System.out.println((t1 - t0) / 1e6);
        final SimulationGraphic simGraphic = new SimulationGraphic(sim, APP_NAME, 3);

        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(sim.box));
        simGraphic.getController().getDataStreamPumps().add(sim.pump);
        simGraphic.getDisplayBox(sim.box).setColorScheme(new ColorSchemeRandomByMolecule(sim, sim.box, sim.getRandom()));

        simGraphic.makeAndDisplayFrame(APP_NAME);

        DisplayTextBoxesCAE display = new DisplayTextBoxesCAE();
        display.setAccumulator(sim.avgEnergy);
        simGraphic.add(display);
    }
}
