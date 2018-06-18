package etomica.normalmode;

import etomica.atom.AtomLeafAgentManager;
import etomica.box.Box;
import etomica.lattice.crystal.PrimitiveCubic;
import etomica.space.Space;
import etomica.space.Vector;

public class CoordinateDefinitionLeafNull extends CoordinateDefinitionLeaf {

    public CoordinateDefinitionLeafNull(Box box, Space space){
        super(box, new PrimitiveCubic(space, 1), space);

    }
    public void initializeCoordinates(int[] nCells){
        siteManager = new AtomLeafAgentManager<Vector>(new SiteSource(space), box);

    }
}
