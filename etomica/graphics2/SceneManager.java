package etomica.graphics2;


import etomica.api.IAtom;
import etomica.api.IAtomPositioned;
import etomica.api.IAtomTypeSphere;
import etomica.api.IBoundary;
import etomica.api.IBox;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.atom.AtomFilter;
import etomica.atom.AtomFilterStatic;
import etomica.atom.AtomLeafAgentManager;
import etomica.atom.AtomLeafAgentManager.AgentIterator;
import etomica.math.geometry.LineSegment;
import etomica.math.geometry.Polytope;
import etomica.space.Boundary;
import etomica.space.ISpace;
import etomica.space3d.Vector3D;

/**
 * Converts all drawable information in a Box object into graphics primitives
 *  using the Renderable interface. 
 * It does not handle mouse or keyboard - it's up to the implementation to do it. 
 */
public final class SceneManager {
    
    public SceneManager(ISpace space) {
    	this.space = space;
        setAtomFilter(AtomFilterStatic.ACCEPT_ALL);
        setScale(1.0);
 
    	colorScheme = new ColorSchemeByType();
   }

    public void setRenderer( Renderable r ) {
    	renderer = r;
        renderer.setColorScheme(colorScheme);
    }

   
    public void updateAtomPositions() {
        if(!isVisible() || getBox() == null) return;
        // Create graphical object for the boundary
        IBoundary bnd = box.getBoundary();
        Polytope shape = ((Boundary)bnd).getShape();
        boolean needUpdate = false;
        IVector[] newVertices = shape.getVertices();
        if (boundaryVertices == null || boundaryVertices.length != newVertices.length) {
            boundaryVertices = new IVectorMutable[newVertices.length];
            for (int i=0; i<boundaryVertices.length; i++) {
                boundaryVertices[i] = space.makeVector();
                boundaryVertices[i].E(newVertices[i]);
            }
            needUpdate = true;
        }
        else {
            for (int i=0; i<boundaryVertices.length; i++) {
                if (!boundaryVertices[i].equals(newVertices[i])) {
                    boundaryVertices[i].E(newVertices[i]);
                    needUpdate = true;
                }
            }
        }

        if (needUpdate) {
            if (boundaryPoly != null) {
                boundaryPoly.dispose();
            }
            
            LineSegment[] edges = shape.getEdges();
            IVectorMutable shift = space.makeVector();
            
            boundaryPoly = renderer.createPoly();
            for(int i=0; i<edges.length; i++) 
            {           
                from.Ev1Pv2( edges[i].getVertices()[0], shift );
                to.Ev1Pv2( edges[i].getVertices()[1], shift );
                boundaryPoly.appendLine( from, to );                
            }
            needUpdate = false;
        }

        agentIterator.reset();
        while (agentIterator.hasNext()) {
            SphereShapeWrapper wrapper = (SphereShapeWrapper)agentIterator.next();
            IAtomPositioned a = wrapper.atom;
            int c = colorScheme.atomColor((IAtom)a);
            IVectorMutable r = a.getPosition();

            Renderable.Shape shp = wrapper.shape;
            shp.setPosition( r );
            shp.setColor( c );
        }
    }
    
    public void setBox(IBox newBox) {
        if (newBox == box) {
            return;
        }
        else if(!(newBox.getBoundary() instanceof Boundary)) {
        	throw new RuntimeException("Cannot use a box with a boundary that is not a subclass of etomica.space.Boundary.");
        }
        agentManager = new AtomLeafAgentManager(new SphereShapeSource(), newBox);
        agentIterator = agentManager.makeIterator();
    	box = newBox;
        if (box != null) {
            from = space.makeVector();
            to = space.makeVector();
        }
    }
    
    public IBox getBox() {
    	return box;
    }
    
    public boolean isVisible() {
    	return true;
    }
    
	public ColorScheme getColorScheme() {
		return colorScheme;
	}
	public void setColorScheme(ColorScheme colorScheme) {
		this.colorScheme = colorScheme;
        renderer.setColorScheme(colorScheme);
	}
	
    public void setAtomFilter(AtomFilter filter) {atomFilter = filter;} 

    public void setWriteScale(boolean s) {writeScale = s;}
    public boolean getWriteScale() {return(writeScale);}

    public void setQuality(int q) {
      if(q > DRAW_QUALITY_VERY_HIGH)
        q -= DRAW_QUALITY_MAX;
      if(q < DRAW_QUALITY_VERY_LOW)
        q += DRAW_QUALITY_MAX;
      quality = q;
    }
    public int getQuality() {return(quality);}
    
    public void setDrawBoundary(int b) {
      if(b>DRAW_BOUNDARY_ALL)
        b-=DRAW_BOUNDARY_MAX;
      else if(b<DRAW_BOUNDARY_NONE)
        b+=DRAW_BOUNDARY_MAX;
      drawBoundary = b;
    }
    public int getDrawBoundary() {return drawBoundary;}

	public double getScale() {
		return scale;
	}
	public void setScale(double scale) {
		this.scale = scale;
	}
	
	public IAtom[] getSelectedAtoms() {
		return selectedAtoms;
	}
	public void setSelectedAtoms(IAtom[] selectedAtoms) {
		this.selectedAtoms = selectedAtoms;
	}
	
       
    static final int DRAW_QUALITY_VERY_LOW = 0;
    static final int DRAW_QUALITY_LOW = 1;
    static final int DRAW_QUALITY_NORMAL = 2;
    static final int DRAW_QUALITY_HIGH = 3;
    static final int DRAW_QUALITY_VERY_HIGH = 4;
    static final int DRAW_QUALITY_MAX = 5;
    //Boundary Constants
    static final int DRAW_BOUNDARY_NONE = 0;
    static final int DRAW_BOUNDARY_OUTLINE = 1;
    static final int DRAW_BOUNDARY_SHELL = 2;
    static final int DRAW_BOUNDARY_ALL = 3;
    static final int DRAW_BOUNDARY_MAX = 4;
    

    protected AtomFilter atomFilter;
    protected double scale;
    protected IBox box;
    protected IVectorMutable[] boundaryVertices;
    protected IVectorMutable from, to;
    
    protected ColorScheme colorScheme;
    protected IAtom[] selectedAtoms = new IAtom[1];
    protected Renderable renderer;
    
    private AtomLeafAgentManager agentManager;
    private AgentIterator agentIterator;

//  The groups of atoms


   /**
    * Variable specifying whether a line tracing the boundary of the display should be drawn
    * Default value is <code>BOUNDARY_OUTLINE</code>
    */
    int drawBoundary = DRAW_BOUNDARY_OUTLINE;

    Renderable.Polyline boundaryPoly;

    /**
     * Variable that sets the quality of the rendered image.
     */
    int quality = DRAW_QUALITY_NORMAL;

    /** 
     * Flag to indicate if value of scale should be superimposed on image
     */
    boolean writeScale = false;
    
    /**
     *  Sets the quality of the rendered image, false = low, true = high
      */
    boolean highQuality = false;
    
    private final ISpace space;

    public class SphereShapeSource implements AtomLeafAgentManager.AgentSource {

        public Class getAgentClass() {
            return SphereShapeWrapper.class;
        }
        
        public Object makeAgent(IAtom a) {
            if (!(a.getType() instanceof IAtomTypeSphere)) {
                return null;
            }
            SphereShapeWrapper wrapper = new SphereShapeWrapper();
            wrapper.atom = (IAtomPositioned)a;
            if (a.getType() instanceof IAtomTypeSphere) {
                wrapper.shape = renderer.createSphere();
                
                // Scale to atom's given size
                double diameter = ((IAtomTypeSphere)a.getType()).getDiameter();
                atomScale.E(diameter);
                wrapper.shape.setScale(atomScale);
            }
            return wrapper;
        }
        
        public void releaseAgent(Object agent, IAtom atom) {
            ((SphereShapeWrapper)agent).shape.dispose();
        }
        
        private Vector3D atomScale = new Vector3D();
    }

    public static class SphereShapeWrapper {
        public IAtomPositioned atom;
        public Renderable.Sphere shape;
    }
    
} //end of ConfigurationCanvas class

