package etomica.graphics2;


import etomica.api.IAtomLeaf;

/**
 * Does nothing at any time to set atom's color, leaving color to be set to default value.
 * @author David Kofke
 *
 */

public final class ColorSchemeNull implements ColorScheme {
    
    public ColorSchemeNull() {}
        
 /**
  * Return without changing atom's color.
  */
    public final int atomColor(IAtomLeaf a) {return 0;}

/* (non-Javadoc)
 * @see etomica.graphics2.ColorScheme#getNumColors()
 */
public int getNumColors() {
	return 1;
}

/* (non-Javadoc)
 * @see etomica.graphics2.ColorScheme#getColor(int)
 */
public Color getColor(int index) {
	// TODO Auto-generated method stub
	return new Color(0,0,0);
}

}
