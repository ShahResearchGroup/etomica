package etomica.models.hexane;

import etomica.atom.Atom;
import etomica.atom.AtomLeaf;
import etomica.atom.AtomPair;
import etomica.atom.iterator.AtomIteratorAllMolecules;
import etomica.atom.iterator.AtomIteratorTree;
import etomica.lattice.Primitive;
import etomica.phase.Phase;
import etomica.space.BoundaryDeformablePeriodic;
import etomica.space.BoundaryRectangular;
import etomica.space.Tensor;
import etomica.space.Vector;


/**
 * Given an AtomPair, this class returns an integer index for it, based on the positions of the atoms.
 * 
 * @author nancycribbin
 */

public class PairIndexer {
//make sure you aren't loosing the redundancy:
    // fcc lattice = simple cell with basis of 4 atoms.
    //says: I've got a primitive and phase.
    //I need to look at the molecule layer.
    //AtomPositionDefinition comes from AtomFactory of 'cule which is held by the type or the species
    //
    
    public PairIndexer(Phase ph, Primitive pr){
        this(ph, pr, 1, 1);
    }
    
    public PairIndexer(Phase ph, Primitive pr, int a) {
        this(ph, pr, a, a);
    }
    
    /**
     * Dude, you better be sure that you want to use these primitives!!!!
     * 
     * REALLY SURE!!!!
     * 
     */
    public PairIndexer(Phase ph, Primitive pr, int a, int b){
        // Initialize everything we can.
        this.phase = ph;
        this.prim = pr.copy();
        mSize0 = a;
        mSize1 = b;
        dim = phase.space().D();
        atom0 = new AtomLeaf(phase.space());
        atom1 = new AtomLeaf(phase.space());
        temp = phase.space().makeVector();
        angleTol = 0.0005;
        
        //This chunk of code determines whether or not the primitive and the boundary
        // are matched (pointing in the same directions).  
        boolean sameDirection = false;
        //Store a copy of the primitives in temp2.
        Vector[] temp2 = new Vector[dim];
        for(int i = 0; i < dim; i++){
            temp2[i] = (Vector)prim.vectors()[i].clone();
        }

        //Temp3 may be used to store the expanded dimensions (when the boundary is
        // rectangular).
        Vector[] temp3 = new Vector[dim];
        for(int i = 0; i < dim; i++){
            temp3[i] = phase.space().makeVector();
            temp3[i].E(0.0);
        }
        
        double theta = 90;
        
        //Thank Andrew!!
        if(phase.getBoundary() instanceof BoundaryRectangular){
            temp = phase.getBoundary().getDimensions();
            //Since we are rectangular, we can split the dimensions vector up into
            // separate orthogonal vectors.
            for(int i = 0; i < dim; i++){
               temp3[i].setX(i, temp.x(i));
            }
            
            //Now we check the angle between the primitives (temp2) and the boundary (temp3)
            for(int i = 0; i < dim; i++){
                theta =  (temp3[i].dot(temp2[i])) 
                        / Math.sqrt(temp2[i].dot(temp2[i]))
                        / Math.sqrt(temp3[i].dot(temp3[i]))  ;
            }

            if((1 - angleTol < theta) && (theta < 1.0 + angleTol)){
                sameDirection = true;
            }
        }
        else if (phase.getBoundary() instanceof BoundaryDeformablePeriodic){
            //temp3.E(((BoundaryDeformablePeriodic)phase.getBoundary()).getEdgeVectors());
            
            ((BoundaryDeformablePeriodic)phase.getBoundary()).boundaryTensor().assignTo(temp3);
            //Check the angle between each primitive and the corresponding boundary
            for(int i = 0; i < dim; i++){
                theta = (temp3[i].dot(temp2[i])) 
                        / Math.sqrt(temp2[i].dot(temp2[i]))
                        / Math.sqrt(temp3[i].dot(temp3[i]))  ;
            }
            
            if((1 - angleTol < theta) && (theta < 1.0 + angleTol)){
                sameDirection = true;
            }
        }
        else {
            sameDirection = false;
        }
        
        if(!sameDirection){
            throw new IllegalStateException("PairIndexer requires the boundary " +
                    "and the primitives to be pointed in the same direction.");
        }
        
        inverter = phase.space().makeTensor();
        inverter.E(prim.vectors());
        inverter.inverse();
        
        calculateAllAtomIndices();
        makeMaxLength();
    }
    
    /**
     * 
     */
    private void calculateAllAtomIndices(){
        indices = new int[phase.getSpeciesMaster().getMaxGlobalIndex()+1][];
        AtomIteratorAllMolecules aim = new AtomIteratorAllMolecules(phase);
        aim.reset();
        Atom firstatom = (Atom)aim.peek();
        Vector r0 = phase.space().makeVector();
        r0.E(firstatom.type.getPositionDefinition().position(firstatom)); 
        Vector r1 = phase.space().makeVector();
        AtomIteratorTree ait = new AtomIteratorTree();
        while(aim.hasNext()){
            Atom molecule = aim.nextAtom();
            r1.E(molecule.type.getPositionDefinition().position(molecule));
            r1.ME(r0);
            
            int[] inds = calculateTheseIndices(r1);
            indices[molecule.getGlobalIndex()] = inds;
       //nan     
            ait.setRoot(molecule);
            ait.reset();
            while(ait.hasNext()){
                Atom leaf = ait.nextAtom();
                indices[leaf.getGlobalIndex()] = inds;
            }
        
        }
        
        
    }
    
    private int[] calculateTheseIndices(AtomLeaf a){
        //nan is this okay after the atom has moved?
        return calculateTheseIndices(a.coord.position());
    }
    
    /**
     * Returns the Miller indices of the vector argument
     */
//  nan is this okay after the atom has moved?
    
    private int[] calculateTheseIndices(Vector v){
        v.transform(inverter);
        int[] w = new int[v.D()];
        for(int i = 0; i < v.D(); i++){
            w[i] = (int)Math.round(v.x(i));
        }
        
        return w;
    }
    
    /**
     * Makes the vector that contains the maximum in each direction.
     * The maximum is expressed as an integer which is the number of times the primitive
     * goes in that direction.
     */
    private void makeMaxes(){
        maxes = new int[dim];
        double[] cellLengths = new double[dim];
        for(int i = 0; i < dim; i++){
            cellLengths[i] = Math.abs(phase.getBoundary().getDimensions().x(i) / prim.getSize()[i]);
            maxes[i] = (int)Math.round(cellLengths[i]);
        }
        
    }
    
    protected int[] getMaxes(){
        return maxes;
    }
    
    protected int getMaxes(int i){
        return maxes[i];
    }
    
    /**
     * Returns the index of the atom.
     * @param a
     * @return
     */
    //nan does this work?
    public int[] getIndex(Atom a){
        int[] inds = new int[dim];
        inds = indices[a.getGlobalIndex()];
        return inds;
    }
    
    /**
     * Returns the original lattice point of the atom.
     * @param a
     * @return
     */
    public Vector getOriginalPosition(Atom a){
        Vector vex = phase.space().makeVector();
        int[] inds = new int[dim];
        inds = getIndex(a);
        for(int i = 0; i < dim; i++){
            ((Vector)prim.vectors()[i].clone()).TE((double)inds[i]);
        }
        for(int i = 0; i < dim; i++){
            vex.PE((Vector)prim.vectors()[i]);
        }
        
        return vex;
    }
    
    /**
     * Makes the maximum length of the storage array.
     * 
     */
    private void makeMaxLength(){
        makeMaxes();
        maxLength = 0;
        
        switch(dim){
        case 1:{
            maxLength = getMaxes(0) * (mSize0 + mSize1);
            } break;
        case 2:{            
            maxLength = getMaxes(0) * (getMaxes(1) + mSize0 + mSize1) +
                                      getMaxes(1) * (mSize0 + mSize1);
            
            } break;
        case 3:{
            maxLength = getMaxes(0) * (getMaxes(1) + getMaxes(2) + mSize0 + mSize1) +
                                      getMaxes(1) * (getMaxes(2) + mSize0 + mSize1) +
                                                    getMaxes(2) * (mSize0 + mSize1);
                                      
            } break;
        }
        
        maxLength += mSize0 * mSize1 + mSize1;
        
        //NB that maxes is always positive, so this length is always positive.
    }
 
    /**
     * Returns the appropriate bin number for storing information for a given pair
     * of atoms
     */
    public int getBin(Atom a, Atom b){
        AtomPair ab = new AtomPair(a,b);
        return getBin(ab);
    }
    
    /**
     * Returns the appropriate bin number for storing information for a given pair
     * of atoms
     */
    public int getBin(AtomPair atompair){
        int[] orary = new int[dim];       //the other half of "temp-orary"

        //create the vector between the atoms.
        atom0 = (AtomLeaf)atompair.getAtom(0);
        atom1 = (AtomLeaf)atompair.getAtom(1);
        temp.E(atom1.coord.position());
        temp.ME(atom0.coord.position());
        
        //Call the method which finds the Miller indices
        orary = calculateTheseIndices(temp);
        
        //Check the periodic boundaries
        for(int i = 0; i < dim; i++){
          while (getMaxes(i) < orary[i]) {
                orary[i] -= getMaxes(i);
            }
        }
        for (int i = 0; i < dim; i++) {
            while (orary[i] < 0) {
                orary[i] += getMaxes(i);
            }
        }
        
        //Calculate the bin number based on the Miller indices and the atom numbers in the molecule.
        return calculateBinNumber(orary, atom0, atom1);
    }
      
    /**
     * 
     * 
     */
    private int calculateBinNumber(int[] tin, AtomLeaf a0, AtomLeaf b1){
        int t = 0;
        
        //Instead of a lot of confusing loops, why not just use a switch statment?
        switch(dim){
        case 1:{
            t = tin[0] * (mSize0 + mSize1);
            } break;
        case 2:{            
            t =  tin[0] * (getMaxes(1) + mSize0 + mSize1) +
                            tin[1] * (mSize0 + mSize1);
            
            } break;
        case 3:{
            t =  tin[0] * (getMaxes(1) + getMaxes(2) + mSize0 + mSize1) +
                               tin[1] * (getMaxes(2) + mSize0 + mSize1) +
                                       tin[2] * (mSize0 + mSize1);
                                      
            } break;
        }
        
        //System.out.println(a0.node.getIndex() +" " + b1.node.getIndex());
        t += a0.node.getIndex() * mSize1 + b1.node.getIndex();
        
        return t;
    }
        
    public int getMaxLength(){
        return maxLength;
    }
    
    
     
    Phase phase;            // Contains the atom information.
    int dim;                // The dimensions of the system
    private AtomLeaf atom0;     // Temporary storage
    private AtomLeaf atom1;     // Temporary storage
    private int[] maxes;    // The maximum values in each physical direction
    private int maxLength;  // The maximum number of index values possible.  May be needed outside of this class.
    Primitive prim;         // The primitives used to define the space 
    Vector temp;            // Temporary storage space.
    int[][] indices;        //The first index is the global index of the atom.
                            // the scond
    Tensor inverter;
    int mSize0, mSize1;     //the number of atoms in each molecule.
    double angleTol;        //The tolerance that is allowed in the angle between the primitives and the boundary.
    public void setAngleTol(double angleTol) {
        this.angleTol = angleTol;
    }
    
}