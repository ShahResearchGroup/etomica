package etomica.lattice.crystal;
import etomica.Space;
import etomica.lattice.Primitive;
import etomica.math.geometry.Polytope;
import etomica.space.Vector;

/**
 * Primitive group for a face-centered-cubic system.
 */
public class PrimitiveFcc extends Primitive implements Primitive3D {
    
    //primitive vectors are stored internally at unit length.  When requested
    //from the vectors() method, copies are scaled to size and returned.
    //default size is 1.0
    private double size;
    private Vector[] unitVectors;
    private static final double FCC_ANGLE = Math.acos(0.5);
    
    public PrimitiveFcc(Space space) {
        this(space, 1.0);
    }
    public PrimitiveFcc(Space space, double size) {
        super(space); //also makes reciprocal
        //set up orthogonal vectors of unit size
        unitVectors = new Vector[D];
        for(int i=0; i<D; i++) {
            unitVectors[i] = space.makeVector();
            unitVectors[i].E(1.0/Math.sqrt(2.0));
            unitVectors[i].setX(i,0.0);
        }
        setSize(size); //also sets reciprocal via update
    }
    /**
     * Constructor used by makeReciprocal method of PrimitiveBcc.
     */
    PrimitiveFcc(Space space, Primitive direct) {
        super(space, direct);
        unitVectors = new Vector[D];
        for(int i=0; i<D; i++) {
            unitVectors[i] = space.makeVector();
            unitVectors[i].E(1.0/Math.sqrt(2.0));
            unitVectors[i].setX(i,0.0);
        }
    }
    
    //called by superclass constructor
    protected Primitive makeReciprocal() {
        return new PrimitiveBcc(space, this);
    }
    
    //called by update method of superclass
    protected void updateReciprocal() {
        ((PrimitiveBcc)reciprocal()).setSize(4.0*Math.PI/size);
    }
    
    public void setA(double a) {setSize(a);}
    public double getA() {return size;}
    
    public void setB(double b) {setSize(b);}
    public double getB() {return size;}
        
    public void setC(double c) {setSize(c);}
    public double getC() {return size;}
    
    public void setAlpha(double t) {}//no adjustment of angle permitted
    public double getAlpha() {return FCC_ANGLE;}
    
    public void setBeta(double t) {}
    public double getBeta() {return FCC_ANGLE;}
    
    public void setGamma(double t) {}
    public double getGamma() {return FCC_ANGLE;}
    
    public boolean isEditableA() {return true;}
    public boolean isEditableB() {return false;}
    public boolean isEditableC() {return false;}
    public boolean isEditableAlpha() {return false;}
    public boolean isEditableBeta() {return false;}
    public boolean isEditableGamma() {return false;}

    /**
     * Returns a new PrimitiveCubic with the same size as this one.
     */
    public Primitive copy() {
        return new PrimitiveFcc(space, size);
    }
    
    /**
     * Sets the length of all primitive vectors to the given value.
     */
    public void setSize(double size) {
        this.size = size;
        for(int i=0; i<D; i++) latticeVectors[i].Ea1Tv1(size,unitVectors[i]);
        update();
    }
    /**
     * Returns the common length of all primitive vectors.
     */
    public double getSize() {return size;}
    
    /**
     * Multiplies the size of the current vectors by the given value.
     */
    public void scaleSize(double scale) {
        setSize(scale*size);
    }

    public int[] latticeIndex(Vector q) {
        throw new RuntimeException("PrimitiveFcc.latticeIndex not yet implemented");
/*        for(int i=0; i<D; i++) {
            double x = q.x(i)/size;
            idx[i] = (x < 0) ? (int)x - 1 : (int)x; //we want idx to be the floor of x
        }
        return idx;
*/    }
    
    public int[] latticeIndex(Vector q, int[] dimensions) {
        throw new RuntimeException("PrimitiveFcc.latticeIndex not yet implemented");
 /*       for(int i=0; i<D; i++) {
            double x = q.x(i)/size;
            idx[i] = (x < 0) ? (int)x - 1 : (int)x; //we want idx to be the floor of x
            while(idx[i] >= dimensions[i]) idx[i] -= dimensions[i];
            while(idx[i] < 0)              idx[i] += dimensions[i];
        }
        return idx;
 */   }
    
    public Polytope wignerSeitzCell() {
        throw new RuntimeException("method PrimitiveFcc.wignerSeitzCell not yet implemented");
    }
    
    public Polytope unitCell() {
        throw new RuntimeException("method unitCellFactory not yet implemented");
    }
    
    public String toString() {return "Fcc";}
    

    /**
     * Main method to demonstrate use and to aid debugging
     * / 
    public static void main(String[] args) {
        System.out.println("main method for PrimitiveCubic");
        Space space = new Space2D();
        Simulation sim = new Simulation(space);
        int D = space.D();
        PrimitiveCubic primitive = new PrimitiveCubic(sim);
        AtomFactory basis = primitive.unitCellFactory();
        final int nx = 4;
        final int ny = 5;
        BravaisLattice lattice = BravaisLattice.makeLattice(sim, 
                                basis, 
                                new int[] {nx,ny},
                                primitive);
        lattice.shiftFirstToOrigin();
        System.out.println("Total number of sites: "+lattice.siteList().size());
        System.out.println();
        System.out.println("Coordinate printout");
        AtomAction printSites = new AtomAction() {
            public void actionPerformed(Atom s) {
                System.out.print(s.coord.position().toString()+" ");
                if(((Site)s).latticeCoordinate()[1]==ny-1) System.out.println();
            }
        };
        AtomIteratorList iterator = new AtomIteratorList(lattice.siteList());
        iterator.allAtoms(printSites);
        System.out.println();
                
        AbstractCell testSite = (AbstractCell)lattice.site(new int[] {1,2});
        
        Space.Vector vector = space.makeVector(new double[] {1.5, 2.7});
        System.out.print(vector.toString()+" in cell "+testSite.toString()+"? "+testSite.inCell(vector));
        int[] idx = primitive.latticeIndex(vector);
        System.out.print("; is in: ");
        for(int i=0; i<D; i++) System.out.print(idx[i]);
        System.out.println();
 
        vector = space.makeVector(new double[] {3.5, 5.1});
        System.out.print(vector.toString()+" in cell "+testSite.toString()+"? "+testSite.inCell(vector));
        idx = primitive.latticeIndex(vector);
        System.out.print("; is in: ");
        for(int i=0; i<D; i++) System.out.print(idx[i]);
        System.out.println();
        
        System.out.println("cell volume: "+testSite.volume());
        System.out.println();
        System.out.println("setting size to 2.0");
        primitive.setSize(2.0);
        lattice.update();//recompute all lattice positions
        lattice.shiftFirstToOrigin();
        
        iterator.allAtoms(printSites);
        System.out.println();
        vector = space.makeVector(new double[] {1.5, 2.7});
        System.out.print(vector.toString()+" in cell "+testSite.toString()+"? "+testSite.inCell(vector));
        idx = primitive.latticeIndex(vector);
        System.out.print("; is in: ");
        for(int i=0; i<D; i++) System.out.print(idx[i]);
        System.out.println();

        vector = space.makeVector(new double[] {3.5, 5.1});
        System.out.print(vector.toString()+" in cell "+testSite.toString()+"? "+testSite.inCell(vector));
        idx = primitive.latticeIndex(vector);
        System.out.print("; is in: ");
        for(int i=0; i<D; i++) System.out.print(idx[i]);
        System.out.println();

        System.out.println("cell volume: "+testSite.volume());
  
        /*
        //write out vertices of some cells
        System.out.println();
        System.out.println("Unit-cell vertices of first cell");
        Space.Vector[] vertices = ((AbstractCell)lattice.siteList().getFirst()).vertex();
        for(int i=0; i<vertices.length; i++) {
            System.out.println(vertices[i].toString());
        }
        System.out.println();
        System.out.println("Unit-cell vertices of last cell");
        vertices = ((AbstractCell)lattice.siteList().getLast()).vertex();
        for(int i=0; i<vertices.length; i++) {
            System.out.println(vertices[i].toString());
        }
        * /
    }//end of main
  // */  
}//end of PrimitiveFcc
    
