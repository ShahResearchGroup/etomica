package etomica.api;


/**
 * Interface for a class that performs a nearest-image transformation to a
 * separation vector. A class implementing this interface is required by the
 * CoordinatePair class.
 * <p>
 * The nearest image is the pair of atom images that are closest when all
 * periodic-boundary images are considered. This separation is defined by a
 * Boundary class, and typically it is a Boundary instance that is used when
 * this interface is specified.
 * 
 */
public interface INearestImageTransformer {

    public void nearestImage(IVector dr);
}
