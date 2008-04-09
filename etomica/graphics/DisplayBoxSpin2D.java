package etomica.graphics;

import java.awt.Color;
import java.awt.Graphics;

import etomica.api.IAtomPositioned;
import etomica.lattice.RectangularLattice;
import etomica.nbr.site.AtomSite;
import etomica.nbr.site.NeighborSiteManager;
import etomica.space.ISpace;


/**
 * TODO To change the template for this generated type comment go to
 * Window - Preferences - Java - Code Style - Code Templates
 *
 * @author David Kofke
 *
 */

/*
 * History
 * Created on May 23, 2005 by kofke
 */
public class DisplayBoxSpin2D extends DisplayBoxCanvas2D {

    /**
     * @param box
     */
    public DisplayBoxSpin2D(DisplayBox _box, NeighborSiteManager neighborSiteManager, ISpace space) {
        super(_box, space);
        latticeIndex = new int[displayBox.getBox().getBoundary().getDimensions().getD()];
        spinWidth = 5;
        this.neighborSiteManager = neighborSiteManager;
    }
    
    protected void drawAtom(Graphics g, int origin[], IAtomPositioned atom) {
        AtomSite site = neighborSiteManager.getSite(atom);
        if (site == null) return;
        RectangularLattice lattice = neighborSiteManager.getLattice();
        lattice.latticeIndex(site.getLatticeArrayIndex(),latticeIndex);

        //color central site red
//        ((MySite)lattice.site(iterator.centralSite)).color = Color.RED;
        int nx = 5; //number of planes to draw before moving down to another line of planes
        int k = latticeIndex.length < 3 ? 0 : latticeIndex[2];
        int ox = k % nx;       //set origin for drawing each lattice plane
        int oy = (k - ox)/nx;
        ox = origin[0] + ox*spinWidth*(lattice.getSize()[0]) + 1;
        oy = origin[1] + oy*spinWidth*(lattice.getSize()[1]) + 1;
        //draw lattice plane
        g.setColor(atom.getPosition().x(0) > 0 ? Color.green : Color.white);
        g.fillRect(ox+latticeIndex[0]*spinWidth,oy+latticeIndex[1]*spinWidth,spinWidth,spinWidth);
//        g.setColor(Color.black);
//        g.drawRect(ox+latticeIndex[0]*spinWidth,oy+latticeIndex[1]*spinWidth,spinWidth,spinWidth);
    }

    private int spinWidth;
    private final int[] latticeIndex;
    private final NeighborSiteManager neighborSiteManager;
}
