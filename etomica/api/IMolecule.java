package etomica.api;


/**
 * Interface for a group of IAtoms, typically a molecule or a SpeciesAgent.
 */
public interface IMolecule extends IAtom {

    /**
     * Informs the IAtom of its index, which is used to construct the address.
     */
    public void setIndex(int index);

    /**
     * Adds the given Atom as a child of this Atom.  The given child Atom
     * should be parentless when this method is called.
     * @throws IllegalArgumentException if the given atom already has a parent.
     */
    public void addChildAtom(IAtomLeaf newChildAtom);

    /**
     * Removes the given child Atom from this AtomGroup.
     * @throws IllegalArgumentException if the given atom is not a child.
     */
    public void removeChildAtom(IAtomLeaf oldChildAtom);

    /**
     * @return the children as an AtomArrayList
     */
    public IAtomSet getChildList();
}