package etomica.config;

import java.util.ArrayList;
import java.util.HashMap;

import etomica.api.IAtom;
import etomica.api.IBox;
import etomica.api.IPotentialMaster;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.atom.AtomTypeSphere;
import etomica.atom.iterator.AtomIteratorArrayListSimple;
import etomica.box.Box;
import etomica.integrator.IntegratorHard;
import etomica.lattice.BravaisLatticeCrystal;
import etomica.lattice.IndexIteratorRectangular;
import etomica.lattice.IndexIteratorSizable;
import etomica.lattice.LatticeCubicFcc;
import etomica.lattice.SpaceLattice;
import etomica.math.geometry.Plane;
import etomica.potential.PotentialMaster;
import etomica.simulation.Simulation;
import etomica.space.Space;
import etomica.space3d.Space3D;
import etomica.species.SpeciesSpheresMono;

/**
 * Constructs configuration that has the molecules placed on the sites of a
 * lattice. Molecules are placed sequentially on the sites of the lattice in an
 * order specified by an index iterator set at construction.
 * <p>
 * Iteration over the indexes yields integer arrays, and with each iteration the
 * array is passed to the site method of the lattice which returns the position
 * for placement of the next molecule. Each array index is iterated to a maximum
 * value determined by the number of molecules to be placed, the dimensions of
 * the box in which they are placed, and the lattice constants of the lattice.
 * <p>
 * An instance of this class may be configured to place atoms such that they
 * uniformly fill the volume of the box. It will attempt this by scaling the
 * lattice constants of the configuration in an appropriate way. Success in
 * getting a good spatial distribution may vary.
 * <p>
 * An instance can also be configured to remember the indices used to get
 * lattice position for each molecule that is placed. This can be useful if it
 * is desired to associate each molecule with a lattice site.
 */
public class ConfigurationLatticeWithPlane extends ConfigurationLattice {

    private static final long serialVersionUID = 2L;
    private Plane plane;
    private ArrayList species;
    private HashMap allocation;
    
    private final int LEFT  = 0;
    private final int RIGHT = 1;

    /**
     * Constructs class using instance of IndexIteratorRectangular as the default
     * index iterator.
     */
    public ConfigurationLatticeWithPlane(SpaceLattice lattice, Plane plane, Space space) {
        this(lattice, plane, new IndexIteratorRectangular(lattice.D()), space);
    }

    /**
     * Construct class that will place atoms on sites of the given lattice,
     * proceeding in the order resulting from iteration through the given index
     * iterator.
     */
    private ConfigurationLatticeWithPlane(SpaceLattice lattice,
            Plane plane, IndexIteratorSizable indexIterator, Space space) {
    	super(lattice, indexIterator, space);

        if(indexIterator.getD() != lattice.D()) {
            throw new IllegalArgumentException("Dimension of index iterator and lattice are incompatible");
        }
        this.plane = plane;
        species = new ArrayList();
        allocation = new HashMap();
    }

    /**
     * 
     * @param newSpecies : species that will need to be configured
     */
    public void addSpecies(ISpecies newSpecies) {
    	if(species.indexOf(newSpecies) == -1) {
    	    species.add(newSpecies);
    	    allocation.put(newSpecies, new Float(0.5));
    	}
    }

    /**
     * 
     * @param newSpecies : species that should be removed from configuration
     */
    public void removeSpecies(ISpecies remSpecies) {
    	if(species.indexOf(remSpecies) != -1) {
    		species.remove(remSpecies);
    	    allocation.remove(remSpecies);
    	}
    }

    /**
     * Sets the initial allocation of the species on the left side
     * of the plane.  The remaining molecules in the species will
     * be initially allocated to the right side of the plane.
     * 
     * @param sp : Species to set allocation for
     * @param pct : Percentage of species to initially allocate
     * on the left side of the plane (0.0 <= pct <= 1.0)
     */
    public void setSpeciesAllocation(ISpecies sp, float pct) {
    	if(species.indexOf(sp) != -1 && pct <= 1.0) {
    		allocation.remove(sp);
    		allocation.put(sp, new Float(pct));
    	}
    }

    /**
     * Sets the initial allocation of the species on the left side
     * of the plane.  The remaining molecules in the species will
     * be initially allocated to the right side of the plane.
     * 
     * @param sp : Species to set allocation for
     * @param pct : Percentage of species to initially allocate
     * on the left side of the plane (0.0 <= pct <= 1.0)
     */
    public float getSpeciesAllocation(ISpecies sp) {
    	float fValue = 0.0f;
    	if(allocation.containsKey(sp)) {
    		Float value = ((Float)allocation.get(sp));
    		fValue = value.floatValue();
    	}
		return fValue;
    }

    /**
     * Places the molecules in the given box on the positions of the
     * lattice.  The molecules must be of a species added with addSpecies()
     * or they will not be displayed.
     */
// Method is assuming plane is in the middle.  Calculations making this
// assumption are noted.
    public void initializeCoordinates(IBox box) {

        int numSpecies = species.size();
        int[] speciesCount = new int[numSpecies];

        int sumOfMolecules = 0;
        for (int i = 0; i < numSpecies; i++) {
            speciesCount[i] = box.getNMolecules((ISpecies)species.get(i));
            sumOfMolecules = sumOfMolecules + speciesCount[i];
        }

        if (sumOfMolecules == 0) {
            return;
        }

        // Allocate species to left or right side of plane
        int [][] molecules = new int[RIGHT+1][numSpecies];
        int [] maxMolecules = { 0, 0 };

        for (int i = 0; i < numSpecies; i++) {
        	ISpecies sp = ((ISpecies)species.get(i));
            molecules[LEFT][i] =  (int)(box.getNMolecules(sp) * getSpeciesAllocation(sp));
            molecules[RIGHT][i] = box.getNMolecules(sp) - molecules[LEFT][i];
            maxMolecules[LEFT] += molecules[LEFT][i];
            maxMolecules[RIGHT] += molecules[RIGHT][i];
        }

        // determine scaled shape of simulation volume
        IVector halfShape = space.makeVector();
        halfShape.E(box.getBoundary().getDimensions());

	    int planeDimIdx = 0;
	    if(plane.getA() > plane.epsilon) {
	        planeDimIdx = 0;
	    }
	    else if(plane.getB() > plane.epsilon) {
	    	planeDimIdx = 1;
	    }
	    else if(space.D() == 3 &&
	    		plane.getC() > plane.epsilon) {
	    	planeDimIdx = 2;
	    }

	    IVector entireShape = space.makeVector();
	    entireShape.E(halfShape);

	    //  NOTE, JUST DIVIDING BY 2 ASSUMES PLANE DOWN CENTER
        // Need to adjust shape on either side of plane in plane dimension.
	    halfShape.setX(planeDimIdx, halfShape.x(planeDimIdx) / 2);


        IVector latticeConstantV = Space.makeVector(lattice.getLatticeConstants());
        halfShape.DE(latticeConstantV);

        int[][] latticeDimensions;
        latticeDimensions = new int[RIGHT+1][];

        for(int side = LEFT; side <= RIGHT; side++) {
		    // determine number of cells in each direction
		    latticeDimensions[side] = calculateLatticeDimensions(maxMolecules[side], halfShape);
        }

        AtomIteratorArrayListSimple[] atomIterator = new AtomIteratorArrayListSimple[numSpecies];
        for (int i = 0; i < numSpecies; i++) {
            atomIterator[i] = new AtomIteratorArrayListSimple(box.getMoleculeList((ISpecies)species.get(i)));
            atomIterator[i].reset();
        }

        for(int side = LEFT; side <= RIGHT; side++) {

	        // determine lattice constant
	        IVector latticeScaling = space.makeVector();
	        if (rescalingToFitVolume) {
                latticeScaling.E(halfShape);
	            latticeScaling.DE(Space.makeVector(latticeDimensions[side]));
	        } else {
	            latticeScaling.E(1.0);
	        }

            indexIterator.setSize(latticeDimensions[side]);
            indexIterator.reset();

	        // determine amount to shift lattice so it is centered in volume
	        IVector offset = space.makeVector();
	        offset.E(box.getBoundary().getDimensions());

	        IVector temp3 = space.makeVector();
            temp3.E(entireShape);
            temp3.TE(-0.5);
            temp3.setX(planeDimIdx, halfShape.x(planeDimIdx) * (side-1));
            offset.E(temp3);

	        IVector temp2 = space.makeVector();
	        temp2.E(latticeScaling);
	        temp2.TE(0.5);
	        offset.PE(temp2);

	        myLat = new MyLattice(lattice, latticeScaling, offset);
	
	        indexIterator.reset();

	        // Place molecules
	        for (int i = 0; i < numSpecies; i++) {
	        	for (int y = 0; y < molecules[side][i]; y++) {
	                IAtom a = atomIterator[i].nextAtom();

			        int[] idx = indexIterator.next();

			        atomActionTranslateTo.setDestination((IVector)myLat.site(idx));
			        atomActionTranslateTo.actionPerformed(a);
	
		        }
	        }
        }

    }

    public static void main(String[] args) {
    	Space sp = Space3D.getInstance();
        Simulation sim = new Simulation(sp);
        IPotentialMaster potentialMaster = new PotentialMaster(sim.getSpace());
        IBox box = new Box(sim, sp);
        sim.addBox(box);
        SpeciesSpheresMono species = new SpeciesSpheresMono(sim, sp);
        sim.getSpeciesManager().addSpecies(species);
        ((AtomTypeSphere)species.getLeafType()).setDiameter(5.0);
        int k = 4;
        box.setNMolecules(species, 4 * k * k * k);
        IntegratorHard integrator = new IntegratorHard(sim, potentialMaster, sp);
        integrator.setBox(box);
//	        ColorSchemeByType colorScheme = new ColorSchemeByType();
        // CubicLattice lattice = new LatticeCubicBcc();
        BravaisLatticeCrystal lattice = new LatticeCubicFcc();
        // CubicLattice lattice = new LatticeCubicSimple();
        ConfigurationLattice configuration = new ConfigurationLattice(lattice, sp);
        // box.boundary().setDimensions(new Space3D.Vector(15.,30.,60.5));
        configuration.initializeCoordinates(box);
        // etomica.graphics.DisplayBox display = new
        // etomica.graphics.DisplayBox(box);

        etomica.graphics.SimulationGraphic simGraphic = new etomica.graphics.SimulationGraphic(
                sim, sp);
//	        ((ColorSchemeByType) ((DisplayBox) simGraphic.displayList()
//	                .getFirst()).getColorScheme()).setColor(species
//	                .getMoleculeType(), java.awt.Color.red);
        simGraphic.makeAndDisplayFrame();
    }

}
