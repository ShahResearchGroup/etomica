/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package etomica.atom.iterator;

import etomica.atom.MoleculesetAction;
import etomica.box.Box;
import etomica.molecule.IMoleculeList;
import etomica.molecule.iterator.MpiInterspeciesAA;
import etomica.simulation.Simulation;
import etomica.species.ISpecies;
import org.junit.jupiter.api.Test;

import static etomica.UnitTestUtil.makeStandardSpeciesTree;
import static etomica.atom.iterator.MoleculeIteratorTestAbstract.*;
import static org.junit.jupiter.api.Assertions.assertTrue;


/**
 * Unit test for ApiInterspeciesAA
 *
 * @author David Kofke
 */
class MpiInterspeciesAATest {

    @Test
    public void testIterator() {

        int[] n0 = new int[]{10, 1, 0};
        int nA0 = 5;
        int[] n1 = new int[]{5, 1, 6};
        Simulation sim = makeStandardSpeciesTree(n0, nA0, n1);

        ISpecies[] species = new ISpecies[sim.getSpeciesCount()];
        for (int i = 0; i < sim.getSpeciesCount(); i++) {
            species[i] = sim.getSpecies(i);
        }
        boxTest(sim.getBox(0), species);
        boxTest(sim.getBox(1), species);

        MpiInterspeciesAA api = new MpiInterspeciesAA(new ISpecies[]
                {species[0], species[1]});

        //test documented exceptions
        boolean exceptionThrown = false;
        try {
            // using iterator before calling setBox
            api.reset();
        } catch (RuntimeException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        try {
            // species array of length != 2
            new MpiInterspeciesAA(new ISpecies[]{species[0]});
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        try {
            // species array with species[0] = species[1]
            new MpiInterspeciesAA(new ISpecies[]{species[0], species[0]});
        } catch (IllegalArgumentException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        try {
            // species array with species[1] = null
            new MpiInterspeciesAA(new ISpecies[]{species[0], null});
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
        exceptionThrown = false;
        try {
            // null species array
            new MpiInterspeciesAA(null);
        } catch (NullPointerException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);


    }

    /**
     * Performs tests on different species combinations in a particular box.
     */
    private void boxTest(Box box, ISpecies[] species) {
        speciesTestForward(box, species, 0, 1);
        speciesTestForward(box, species, 1, 0);
    }

    /**
     * Test iteration in various directions with different targets.
     */
    private void speciesTestForward(Box box, ISpecies[] species, int species0Index, int species1Index) {
        MpiInterspeciesAA api = new MpiInterspeciesAA(new ISpecies[]{species[species0Index], species[species1Index]});
        MoleculesetAction speciesTest = new SpeciesTestAction();

        api.setBox(box);
        int count = box.getMoleculeList(species[species0Index]).size() * box.getMoleculeList(species[species1Index]).size();

        countTest(api, count);
        allAtoms(api, speciesTest);

        //test null box throws an exception
        boolean exceptionThrown = false;
        try {
            api.setBox(null);
        } catch (RuntimeException e) {
            exceptionThrown = true;
        }
        assertTrue(exceptionThrown);
    }

    private class SpeciesTestAction implements MoleculesetAction {
        public SpeciesTestAction() {
        }

        public void actionPerformed(IMoleculeList atomSet) {
            assertTrue(atomSet.get(0).getType().getIndex() < atomSet.get(1).getType().getIndex());
        }
    }

}
