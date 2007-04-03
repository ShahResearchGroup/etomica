package etomica.species;
import java.lang.reflect.Constructor;

import etomica.EtomicaElement;
import etomica.EtomicaInfo;
import etomica.atom.AtomFactoryMono;
import etomica.atom.AtomTypeLeaf;
import etomica.atom.AtomTypeSphere;
import etomica.chem.elements.Element;
import etomica.chem.elements.ElementSimple;
import etomica.simulation.Simulation;
import etomica.space.CoordinateFactorySphere;

/**
 * Species in which molecules are each made of a single spherical atom.
 * Does not permit multiatomic molecules.  The advantage of this species
 * over the multiatomic version (used with 1 atom), is that one layer of
 * the atom hierarchy is eliminated in SpeciesSpheresMono.  Each atom is
 * the direct child of the species agent (i.e., each atom is at the "molecule"
 * level in the hierarchy, without an intervening AtomGroup).
 * 
 * @author David Kofke
 */
public class SpeciesSpheresMono extends Species implements EtomicaElement {

    /**
     * Constructs instance with a default element
     */
    public SpeciesSpheresMono(Simulation sim) {
        this(sim, new ElementSimple(sim));
    }
    
    public SpeciesSpheresMono(Simulation sim, Element element) {
        super(new AtomFactoryMono(new CoordinateFactorySphere(sim), new AtomTypeSphere(sim, element)));
    }
    
    public static EtomicaInfo getEtomicaInfo() {
        EtomicaInfo info = new EtomicaInfo("Species with molecules composed of one or more spherical atoms");
        return info;
    }

    public SpeciesSignature getSpeciesSignature() {
        Constructor constructor = null;
        try {
            constructor = this.getClass().getConstructor(new Class[]{Simulation.class,Element.class});
        }
        catch(NoSuchMethodException e) {
            System.err.println("you have no constructor.  be afraid");
        }
        return new SpeciesSignature(getName(),constructor,new Object[]{((AtomTypeLeaf)factory.getType()).getElement()});
    }
    
    private static final long serialVersionUID = 1L;
}
