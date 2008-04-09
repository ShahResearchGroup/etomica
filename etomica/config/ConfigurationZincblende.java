package etomica.config;

import etomica.action.AtomActionTranslateBy;
import etomica.action.AtomGroupAction;
import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBox;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.box.Box;
import etomica.graphics.ColorSchemeByType;
import etomica.graphics.DisplayBox;
import etomica.lattice.LatticeCubicFcc;
import etomica.simulation.Simulation;
import etomica.space.ISpace;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.space3d.Vector3D;

/**
 * Sets the configuration to the zincblende structure, which consists
 * of two fcc lattices, with one shifted in each direction by one-quarter
 * of the lattice constant.
 */
public class ConfigurationZincblende extends ConfigurationLattice {
    
    private static final long serialVersionUID = 2L;
    private AtomGroupAction translator0, translator1;
    protected ISpecies[] species;
    
    public ConfigurationZincblende(double latticeConstant, ISpace space) {
        super(new LatticeCubicFcc(latticeConstant), space);
        species = new ISpecies[2];
    }
    
    public void setSpecies1(ISpecies species1) {
        species[0] = species1;
    }
    
    public void setSpecies2(ISpecies species2) {
        species[1] = species2;
    }
    
    public ISpecies getSpecies1() {
        return species[0];
    }
    
    public ISpecies getSpecies2() {
        return species[1];
    }
    
    /**
     * Initializes positions of atoms to the zincblende structure.  The given
     * array should hold exactly two AtomLists, each with the same number of atoms.
     */
    public void initializeCoordinates(IBox box) {
        translator0 = new AtomGroupAction(new AtomActionTranslateBy(space));
        translator1 = new AtomGroupAction(new AtomActionTranslateBy(space));
        IAtomSet[] lists = new IAtomSet[]{box.getMoleculeList(species[0]), box.getMoleculeList(species[1])};
        if(lists == null || lists.length != 2) {//need an exception for this
            throw new IllegalArgumentException("inappropriate argument to ConfigurationZincBlende");
        }
        if(lists[0].getAtomCount() != lists[1].getAtomCount()) {
            System.err.println("Warning: different numbers of molecules for two species in ConfigurationZincBlende");
        }
        
        int nCells = (int) Math.ceil(lists[0].getAtomCount() / 4.0);

        // determine scaled shape of simulation volume
        IVector shape = space.makeVector();
        shape.E(box.getBoundary().getDimensions());
        IVector latticeConstantV = Space.makeVector(lattice.getLatticeConstants());
        shape.DE(latticeConstantV);

        // determine number of cells in each direction
        int[] latticeDimensions = calculateLatticeDimensions(nCells, shape);
        if (indexIterator.getD() > latticeDimensions.length) {
            int[] iteratorDimensions = new int[latticeDimensions.length+1];
            System.arraycopy(latticeDimensions, 0, iteratorDimensions, 0,
                    latticeDimensions.length);
            iteratorDimensions[latticeDimensions.length] = 4;
            indexIterator.setSize(iteratorDimensions);
        }
        else {
            indexIterator.setSize(latticeDimensions);
        }

        //shift lattice in all three directions by one-quarter the lattice constant
        Vector3D shift = new Vector3D();
        shift.Ea1Tv1(-0.5,box.getBoundary().getDimensions());
        shift.PE(0.125*((LatticeCubicFcc)lattice).getLatticeConstant());
        ((AtomActionTranslateBy)translator0.getAction()).setTranslationVector(shift);

        shift.PE(0.25*((LatticeCubicFcc)lattice).getLatticeConstant());
        ((AtomActionTranslateBy)translator1.getAction()).setTranslationVector(shift);

        // Place molecules
        indexIterator.reset();
        int i = 0;
        while (indexIterator.hasNext()) {
            int[] ii = indexIterator.next();
            IVector site = (IVector) lattice.site(ii);
            atomActionTranslateTo.setDestination(site);

            IAtom a0 = lists[0].getAtom(i);
            IAtom a1 = lists[1].getAtom(i);
            atomActionTranslateTo.actionPerformed(a0);
            atomActionTranslateTo.actionPerformed(a1);

            translator0.actionPerformed(a0);
            translator1.actionPerformed(a1);
        }
    }        
    
    /**
     * Displays configuration without setting up full simulation.
     */
    public static void main(String[] args) {
    	final String APP_NAME = "Configuration Zinc Blende";

    	Space space = Space3D.getInstance();
        Simulation sim = new Simulation(space);
        final IBox box = new Box(sim, space);
        box.getBoundary().setDimensions(new etomica.space3d.Vector3D(30.0, 30.0, 30.0));
        sim.addBox(box);
        etomica.species.SpeciesSpheresMono speciesSpheres0  = new etomica.species.SpeciesSpheresMono(sim, space);
        etomica.species.SpeciesSpheresMono speciesSpheres1  = new etomica.species.SpeciesSpheresMono(sim, space);
        sim.getSpeciesManager().addSpecies(speciesSpheres0);
        sim.getSpeciesManager().addSpecies(speciesSpheres1);
        ((IAtomTypeSphere)speciesSpheres0.getLeafType()).setDiameter(5.0);
        ((IAtomTypeSphere)speciesSpheres1.getLeafType()).setDiameter(5.0);
        box.setNMolecules(speciesSpheres0, 32);
        box.setNMolecules(speciesSpheres1, 32);
        ConfigurationZincblende config = new ConfigurationZincblende(15, space);
        config.initializeCoordinates(box);

        final etomica.graphics.SimulationGraphic simGraphic = new etomica.graphics.SimulationGraphic(sim, APP_NAME, space);
        simGraphic.add(new DisplayBox(sim, box, space));
        ColorSchemeByType colorScheme = (ColorSchemeByType)simGraphic.getDisplayBox(box).getColorScheme();
        colorScheme.setColor(speciesSpheres0.getLeafType(),new java.awt.Color(0,255,0));
        colorScheme.setColor(speciesSpheres1.getLeafType(), java.awt.Color.red);

        simGraphic.getController().getSimRestart().setConfiguration(config);
        simGraphic.getController().getReinitButton().setPostAction(simGraphic.getPaintAction(box));

        simGraphic.makeAndDisplayFrame(APP_NAME);
        simGraphic.getDisplayBox(box).graphic().repaint();
    }
    
}
