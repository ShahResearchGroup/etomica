/*
 * History
 * Created on Dec 17, 2004 by kofke
 */
package etomica.lattice;

import java.awt.Color;
import java.awt.Graphics;

import javax.swing.JPanel;

import etomica.IteratorDirective;
import etomica.graphics.SimulationGraphic;

/**
 * Basic implementation of the AbstractLattice interface, providing construction
 * and access of sites for a lattice of arbitrary dimension.  Internally, sites are stored
 * in a 1-D array of objects, and are accessed by unrolling the index specification to
 * determine the storage-array index.  Provides a configurable neighbor iterator that
 * returns the sites within a rectangular box centered on a given site.
 */

// Example showing internal ordering of elements
//  0     1     2     3     4     5     6     7     8     9    10    11   arrayIndex
//(000) (001) (002) (010) (011) (012) (100) (101) (102) (110) (111) (112) index
//  for this example, size = {2, 2, 3}, jumpCount = {6, 3, 1}
//  note that number of sites = size[0]*jumpCount[0]

public class SimpleLattice implements AbstractLattice {

    /**
     * Constructs a lattice of the given dimension (D) with sites
     * made from the given factory.  Default size gives a lattice
     * containing a single site.
     */
    public SimpleLattice(int D, SiteFactory siteFactory) {
        this.D = D;
        jumpCount = new int[D];
        jumpCount[D-1] = 1;
        size = new int[D];
        this.siteFactory = siteFactory;
        int[] defaultSize = new int[D];
        for(int i=0; i<D; i++) defaultSize[i] = 1;
        setSize(defaultSize);
    }

    /* (non-Javadoc)
     * @see etomica.lattice.AbstractLattice#D()
     */
    public final int D() {
        return D;
    }

    /* (non-Javadoc)
     * @see etomica.lattice.AbstractLattice#siteList()
     */
    public Object[] sites() {
        return sites;
    }

    /* (non-Javadoc)
     * @see etomica.lattice.AbstractLattice#site(int[])
     */
    public Object site(int[] index) {
        return sites[arrayIndex(index)];
    }
    
    /**
     * Returns the index in the 1-d array for the site corresponding
     * to the given lattice index.
     */
    private final int arrayIndex(int[] index) {
        int idx = 0;
        for(int i=0; i<D; i++) {
            idx += index[i]*jumpCount[i];
        }
        return idx;
    }

    /* (non-Javadoc)
     * @see etomica.lattice.AbstractLattice#getDimensions()
     */
    public int[] getSize() {
        return size;
    }

    /* (non-Javadoc)
     * @see etomica.lattice.AbstractLattice#setDimensions(int[])
     */
    public void setSize(int[] dim) {
        if(dim.length != D) throw new IllegalArgumentException("Incorrect dimension dimension");
        System.arraycopy(dim, 0, size, 0, D);
        for(int i=D-1; i>0; i--) {
            jumpCount[i-1] = jumpCount[i]*size[i];
        }
        sites = new Object[jumpCount[0]*size[0]];
        int[] idx = new int[D];
        for(int i=0; i<sites.length; i++) {
            sites[i] = siteFactory.makeSite(this, idx);
            increment(idx, D-1);
        }
//        siteFactory.makeSites(this, sites);
    }

    //method used by setDimensions method to cycle the index array through its values
    //used recursively to advance each element as needed.
    private void increment(int[] idx, int d) {
        idx[d]++;
        if(d == 0) return;
        if(idx[d] == size[d]) {
            idx[d] = 0;
            increment(idx,d-1);
        }
    }
    
    protected Object[] sites;
    protected final int[] size;
//  jumpCount[i] gives the number of sites skipped when the i-th index is incremented by 1
    private final int[] jumpCount;
    private final int D;
    protected SiteFactory siteFactory;
    
    /**
     *  Iterates over all sites of the lattice. 
     */
    public static class Iterator implements SiteIterator {
        
        public boolean hasNext() {
            return cursor < size;
        }
        public Object next() {
            return hasNext() ? lattice.sites[cursor++] : null;
        }
        public Object peek() {
            return hasNext() ? lattice.sites[cursor] : null;
        }
        public void reset() {
            size = size();
            cursor = 0;
        }
        public int size() {
            return (lattice != null) ? lattice.sites.length : 0;
        }
        public void unset() {
            cursor = Integer.MAX_VALUE;
        }
        public void setLattice(AbstractLattice lattice) {
            this.lattice = (SimpleLattice)lattice;
            unset();
        }
        private int cursor = Integer.MAX_VALUE;
        private SimpleLattice lattice;
        private int size = 0;
    }//end of SiteIterator

    /**
     * An iterator that generates the neighboring sites of a given site.  The
     * neighbors are defined as those within a rectangular box centered on the 
     * site, with consideration of periodic boundaries.  Iterator can be configured
     * to yield the up- and/or down-neighbors of the site.  
     */
    public static class NeighborIterator implements SiteIterator {

        /**
         * Constructs iterator that needs to be configured before use.  Must specify
         * the central site and the range before reset and iteration.
         */
        public NeighborIterator(int D) {
            super();
            this.D = D;
            centralSite = new int[D];
            range = new int[D];
            cursor = Integer.MAX_VALUE;
        }
        
        public final int D() {
            return D;
        }
        
        /**
         * Identifies the site whose neighbors will be given on iteration.
         * Requires subsequent call to reset before iteration.
         * @param index the coordinate of the central site
         */
        public void setSite(int[] index) {
            if(index.length != D) throw new IllegalArgumentException("Incorrect length of array passed to setSite");
            System.arraycopy(index, 0, centralSite, 0, D);
            needNeighborUpdate = true;
            unset();
        }
        
        /**
         * Indicates whether the iterator has another site.
         */
        public final boolean hasNext() {
            return cursor < neighborCount;
        }
        
        /**
         * Resets the iterator to loop through its iterates again.  This
         * must be done after any call to setRange, setDirection, or setSite.
         */
        public void reset() {
            if(needNeighborUpdate) updateNeighborList();
            cursor = 0;
        }
        
        /**
         * Sets the range used to define the neighboring sites.  Each of the values
         * in newRange is used to determine the size of the neighbor box in the 
         * corresponding dimension: (edge length) = 2*value + 1, as the value describes
         * the neighbor distance in each direction (e.g., left and right) from the 
         * Non-negative values (including zero) are acceptable, to a maximum of
         * (lattice dimension - 1)/2, beyond which the neighbor distances extend beyond
         * the size of the lattice.  An IllegalArgumentException is thrown if any index
         * is outside its acceptable range.  
         * A copy of the newRange array is made, so it may be re-used elsewhere after
         * calling this method.
         */
        public void setRange(int[] newRange) {
            if(newRange.length != D) throw new IllegalArgumentException("Incorrect length of array passed to setRange");
            for(int i=0; i<D; i++) {
                if(newRange[i] < 0) 
                    throw new IllegalArgumentException("Neighbor range cannot be negative");
                if(2*newRange[i]+1 > lattice.size[i]) 
                    throw new IllegalArgumentException("Neighbor range exceeds lattice site");
            }
            System.arraycopy(newRange, 0, range, 0, D);
            furthestNeighborDelta = lattice.arrayIndex(range);
            halfNeighborCount = 1;
            for(int i=0; i<D; i++) {
                halfNeighborCount *= (2*range[i]+1);
            }
            halfNeighborCount = (halfNeighborCount-1)/2;
            neighbors = new int[2*halfNeighborCount];
            needNeighborUpdate = true;
            unset();
        }
        
        /**
         * Returns (a copy of) the array specifying the neighbor range.
         */
        public int[] getRange() {
            return (int[])range.clone();
        }

        /**
         * Puts iterator in a state in which hasNext() returns false.
         */
        public void unset() {
            cursor = neighborCount;
        }
        
        /**
         * Returns the next atom in the iteration sequence, or null
         * if hasNext is false.
         */
        public Object next() {
            return hasNext() ? lattice.sites[neighbors[cursor++]] : null;
        }
        
        /**
         * Returns the next iterate without advancing the iterator.
         */
        public Object peek() {
            return hasNext() ? lattice.sites[neighbors[cursor]] : null;
        }
        
        /**
         * The number of iterates returned by this iterator in its current state.
         */
        public int size() {
            return neighborCount;
        }

        /**
         * Gets the direction (up or down list) of the neighbors given by the iterator.
         * A null value indicates that all neighbors will be returned.
         */
       public IteratorDirective.Direction getDirection() {
            return direction;
        }
        /**
         * Sets the iterator to return only up-list neighbors or down-list neighbors.
         * A null value indicates that all neighbors are to be returned.
         */
        public void setDirection(IteratorDirective.Direction direction) {
            this.direction = direction;
            doUp = (direction != IteratorDirective.DOWN);//also handles case where
            doDown = (direction != IteratorDirective.UP);//direction is null
            needNeighborUpdate = true;
            unset();
        }
        
        /**
         * Returns the lattice from which the iterates are given.
         */
        public SimpleLattice getLattice() {
            return lattice;
        }
        
        /**
         * Sets the lattice from which the iterates are given.  Dimension
         * of lattice must be the same as that specified to constructor. Also,
         * size of lattice must be compatible with range of neighbor interactions.
         */
        public void setLattice(AbstractLattice lattice) {
            if(lattice.D() != this.D) throw new IllegalArgumentException("Iterator given lattice with incompatible dimension");
            for(int i=0; i<D; i++) {
               if(2*range[i]+1 > lattice.getSize()[i]) 
                    throw new IllegalArgumentException("Neighbor range exceeds lattice size");
            }
            this.lattice = (SimpleLattice)lattice;
        }
        /**
         * Method called on reset if any calls were previously made
         * to setSite, setRange, or setDirection.
         */
        private void updateNeighborList() {
            neighborCount = (direction == null) ? 2*halfNeighborCount : halfNeighborCount;
            int centralSiteIndex = lattice.arrayIndex(centralSite);
            cursor = 0;
            if(doDown) gatherDownNeighbors(0, centralSiteIndex - furthestNeighborDelta);
            if(doUp) gatherUpNeighbors(0, centralSiteIndex+1);
            needNeighborUpdate = false;
        }
        
        /**
         * Identifies and adds to neighbor list all downlist neighbors
         * in the sub-dimension d.  For example, if a 3D lattice and
         * d = 0, gathers all neighbors; d = 1, gathers all in a plane;
         * d = 2, gathers all in a line.  Neighbors are identified by their 
         * index in the sites array, and startIndex is the position in the array
         * where the first neighbor to be collected by the method is found.  
         */
        private void gatherDownNeighbors(int d, int startIndex) {
            int centralIndex = centralSite[d];
            int iMin = centralIndex-range[d];
            int dim = lattice.size[d];
            
            //this block gathers all neighbors in the dimension d,
            //up to but not including the row where the central site is located
            if(iMin < 0) {//need to implement periodic boundaries
                gatherNeighbors(d, -iMin, startIndex+dim*lattice.jumpCount[d]);
                gatherNeighbors(d, centralIndex, startIndex-iMin*lattice.jumpCount[d]);//note that iMin<0
            } else {//no concern for PBC; gather all neighbors in plane
                gatherNeighbors(d, range[d], startIndex);
            }
            //handle row containing central site
            if(d < D-1) gatherDownNeighbors(d+1, startIndex+range[d]*lattice.jumpCount[d]);
        }

        /**
         * Similar to gatherDownNeighbors, but does upList neighbors.
         */
        //algorithm looks a bit different from gatherDownNeighbors because
        //central-site row is handled first
        private int gatherUpNeighbors(int d, int startIndex) {
            int centralIndex = centralSite[d];
            int iMax = centralIndex+range[d];
            int dim = lattice.size[d];
            
            //handle central-site row
            if(d < D-1) {
                startIndex = gatherUpNeighbors(d+1, startIndex);
                startIndex += lattice.jumpCount[d] - (range[d+1]+1)*lattice.jumpCount[d+1];
            }
            //advance through other rows
            if(iMax >= dim) {
                gatherNeighbors(d, dim-centralIndex-1, startIndex);
                gatherNeighbors(d, iMax-dim+1, startIndex-(centralIndex+1)*lattice.jumpCount[d]);
            } else {
                gatherNeighbors(d, range[d], startIndex);
            }
            return startIndex;
        }
                
        /**
         * @param d specifies row, plane, block, etc. of neighbors gathered
         * @param nSteps number of steps to take in row
         * @param startIndex array of site index for first neighbor
         */
        private void gatherNeighbors(int d, int nSteps, int startIndex) {
            if(d == D-1) {//end of recursion -- here's where the actual gathering is done
                for(int i=0; i<nSteps; i++) {
                    neighbors[cursor++] = startIndex++;
                }
            } else {//step from one row to the next and gather neighbors in each row
                int d1 = d+1;
                int centralIndex = centralSite[d1];
                int iMin = centralIndex-range[d1];
                int iMax = centralIndex+range[d1];
                int dim = lattice.size[d1];
                if(iMin < 0) {//need to consider PBC below
                    for(int i=0; i<nSteps; i++) {
                        int startIndex1 = startIndex+i*lattice.jumpCount[d];
                        gatherNeighbors(d1, -iMin, startIndex1+dim*lattice.jumpCount[d1]);
                        gatherNeighbors(d1, iMax+1, startIndex1-iMin*lattice.jumpCount[d1]);//note that iMin<0
                    }
                } else if(iMax >= dim) {//need to consider PBC above
                    for(int i=0; i<nSteps; i++) {
                        int startIndex1 = startIndex+i*lattice.jumpCount[d];
                        gatherNeighbors(d1, dim-iMin, startIndex1);
                        gatherNeighbors(d1, iMax-dim+1, startIndex1-iMin*lattice.jumpCount[d1]);
                    }
                } else {//no need to consider PBC
                    for(int i=0; i<nSteps; i++) {
                        gatherNeighbors(d1, 2*range[d1]+1, startIndex+i*lattice.jumpCount[d]);
                    }
                }
            }
        }
//        private void gatherAllNeighbors(int d, int startIndex) {
//            int centralIndex = centralSite[d];
//            int iMin = centralIndex-range[d];
//            int iMax = centralIndex+range[d];
//            int dim = dimensions[d];
//            
//            if(iMin < 0) {//need to consider PBC
//                gatherNeighbors(d, -iMin, startIndex+dim*jumpCount[d]);
//                gatherNeighbors(d, iMax+1, startIndex-iMin*jumpCount[d]);//note that iMin<0
//            } else if(iMax >= dim) {//need to consider PBC
//                gatherNeighbors(d, dim-iMin, startIndex);
//                gatherNeighbors(d, iMax-dim+1, startIndex-iMin*jumpCount[d]);
//            } else {//no need to consider PBC
//                gatherNeighbors(d, 2*range[d]+1, startIndex);
//            }
//        }
//        
//        /**
//         * @param d specifies row, plane, block, etc. of neighbors gathered
//         * @param nSteps number of steps to take in row
//         * @param startIndex array of site index for first neighbor
//         */
//        private void gatherNeighbors(int d, int nSteps, int startIndex) {
//            if(d == D-1) {//end of recursion -- here's where the actual gathering is done
//                for(int i=0; i<nSteps; i++) {
//                    neighbors[cursor++] = startIndex++;
//                }
//            } else {//step from one row to the next and gather neighbors in each row
//                for(int i=0; i<nSteps; i++) {
//                     gatherAllNeighbors(d+1,startIndex+i*jumpCount[d]);
//                }
//            }
//        }
        
        protected final int D;
        private final int[] range;
        private int[] neighbors;
        private final int[] centralSite;
        protected SimpleLattice lattice;
        private int cursor;
        private int furthestNeighborDelta;
        private int neighborCount, halfNeighborCount;
        private boolean doUp, doDown;
        private boolean needNeighborUpdate = true;
        private IteratorDirective.Direction direction;
    }//end of NeighborIterator
  
    /**
     * Method to test the neighbor iterator.  Constructs a lattice 
     * with sites defined to hold a color.  Configures iterator, 
     * loops over neighbor sites and defines up/down neighbors to
     * have colors yellow and blue, respectively.  Then puts up a window
     * with the lattice drawn as an array of squares, with central site colored
     * red and with neighbor sites identified by their colors.  3-D lattice is shown
     * with a set of slices through the planes in the 3rd dimension.
     * @param args
     */
    public static void main(String[] args) {
        
        //define the site class
        class MySite {
            final int index;
            java.awt.Color color = java.awt.Color.white;
            MySite(int i) {this.index = i;}
        }
        //define a factory making the sites
        SiteFactory factory = new SiteFactory() {
            public Object makeSite(AbstractLattice lattice, int[] coord) {
                return new MySite(((SimpleLattice)lattice).arrayIndex(coord));
            }
//            public void makeSites(AbstractLattice lattice, Object[] sites) {
//                for(int i=0; i<sites.length; i++) {
//                    sites[i] = new MySite(i);
//                }
//            }
        };
        //construct the lattice and iterator
        int dimension = 3;
        final SimpleLattice lattice = new SimpleLattice(dimension, factory);
        final SimpleLattice.NeighborIterator iterator = new NeighborIterator(dimension);
        final SiteIterator siteIterator = new Iterator();
        iterator.setLattice(lattice);
        siteIterator.setLattice(lattice);
        //configure lattice
        //******change these values to perform different tests *********//
        //remember to change value of dimension in lattice constructor if using different dimensions here 
        lattice.setSize(new int[] {19,11,15});
        iterator.setSite(new int[] {10,5,8});
        iterator.setRange(new int[] {2,3,5});

        //define panel for display, so that it draws lattice with appropriately colored sites
        JPanel canvas = new JPanel() {
            public java.awt.Dimension getPreferredSize() {
                return new java.awt.Dimension(300,300);
            }
            public void paint(Graphics g) {
                int w = 8;  //size (pixels) of each square representing a site
                setSize(800,800);

                //generate down neighbors and color them yellow
                iterator.setDirection(IteratorDirective.DOWN);
                iterator.reset();
                while(iterator.hasNext()) {
                    MySite site = (MySite)iterator.next();
                    System.out.println(site.index);
                    site.color = java.awt.Color.YELLOW;
                }
                //generate up neighbors and color them blue
                iterator.setDirection(IteratorDirective.UP);
//                iterator.setDirection(null);
                iterator.reset();
                while(iterator.hasNext()) {
                    MySite site = (MySite)iterator.next();
                    System.out.println(site.index);
                    site.color = java.awt.Color.BLUE;
                }
//                siteIterator.reset();
//                while(siteIterator.hasNext()) {
//                    MySite site = (MySite)siteIterator.next();
//                    System.out.println(site.index);
//                    site.color = java.awt.Color.BLUE;
//                }
                
                //color central site red
                ((MySite)lattice.site(iterator.centralSite)).color = Color.RED;
                int nx = 5; //number of planes to draw before moving down to another line of planes
                for(int k=0; k<lattice.size[0]; k++) {//loop over planes
                    int ox = k % nx;       //set origin for drawing each lattice plane
                    int oy = (k - ox)/nx;
                    ox = 3 + ox*w*(lattice.size[1]+2);
                    oy = 3 + oy*w*(lattice.size[2]+2);
                    //draw lattice plane
                    for(int j=0; j<lattice.size[2]; j++) {
                        for(int i=0; i<lattice.size[1]; i++) {
                            g.setColor(((MySite)(lattice.site(new int[] {k,i,j}))).color);
                            g.fillRect(ox+i*w,oy+j*w,w,w);
                            g.setColor(Color.black);
                            g.drawRect(ox+i*w,oy+j*w,w,w);
                        }
                    }
                }
            }
        };
        //draw to screen
        javax.swing.JFrame f = new javax.swing.JFrame();
        f.setSize(700,500);
        f.getContentPane().add(canvas);
        f.pack();
        f.show();
        f.addWindowListener(SimulationGraphic.WINDOW_CLOSER);
    }//end of main
}//end of SimpleLattice
