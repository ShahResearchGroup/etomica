package etomica.atom;

import java.util.List;

import etomica.api.IElement;
import etomica.api.IVector;
import etomica.api.IVectorMutable;
import etomica.space.Space;

public class AtomTypeSpheroPolyhedron extends AtomTypeLeaf {

    protected double outRadius, inRadius;
    protected final double sweepRadius;
    protected final List<IVector> vertices;
    
    public AtomTypeSpheroPolyhedron(IElement element, Space space, List<IVector> vertices, double sweepRadius) {
        super(element);
        this.vertices = vertices;
        if (vertices.size() == 1 && sweepRadius == 0) sweepRadius = 0.5;
        this.sweepRadius = sweepRadius;
        int vertexNum = vertices.size();
        IVectorMutable normal = space.makeVector();
        IVectorMutable drij = space.makeVector();
        inRadius = Double.MAX_VALUE;
        
        if (vertexNum >= 3) {
            for (int i = 0; i < vertexNum - 2; i++) {       
                for (int j = i + 1; j < vertexNum - 1; j++) {
                    drij.Ev1Mv2(vertices.get(i), vertices.get(j));
                    for (int k = j + 1; k < vertexNum; k++) {      
                        // loop over all triangles and generate normal vector
                        normal.Ev1Mv2(vertices.get(i), vertices.get(k));
                        normal.XE(drij);
                        double prod = normal.dot(vertices.get(i));
                        if (Math.abs(prod) < 1e-12) {
                            // this isn't a face
                            continue;
                        }
                        normal.TE(1/prod);
                        // check if plane is on the surface
                        boolean isSurface = true;
                        for (int l = 0; l < vertexNum; l++) {
                            // allow a small error due to numerical inaccuracies
                            if (normal.dot(vertices.get(l)) > 1.0 + 1e-12) {
                                // the plane sits between the point and the origin
                                isSurface = false;
                                break;
                            }
                        }
                        if (isSurface) {
                            double radius = 1.0 / Math.sqrt(normal.squared());
                            if (radius < inRadius) inRadius = radius;
                        }
                    }
                }
            }
        }
        if (inRadius == Double.MAX_VALUE) inRadius = 0.0;
        inRadius += sweepRadius;

        // calculate circumscribed sphere radius
        outRadius = 0.0;
        for (int i = 0; i < vertexNum; i++)
        {
            double radius = Math.sqrt(vertices.get(i).squared());
            if (radius > outRadius) outRadius = radius;
        }
        outRadius += sweepRadius;
    }
    
    public double getSweepRadius() {
        return sweepRadius;
    }

    public double getInnerRadius() {
        return inRadius;
    }
    
    public double getOuterRadius() {
        return outRadius;
    }
    
    public List<IVector> getVertices() {
        return vertices;
    }
}
