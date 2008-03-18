package etomica.api;

import etomica.api.IAtom;
import etomica.api.IAtomSet;
import etomica.api.IBoundary;
import etomica.api.IBoxEventManager;
import etomica.api.IMolecule;
import etomica.api.ISimulation;
import etomica.api.ISpecies;
import etomica.api.IVector;
import etomica.units.Dimension;

public interface IBox {

	/* (non-Javadoc)
	 * @see etomica.box.IBox#resetIndex(etomica.simulation.ISimulation)
	 */
	public abstract void resetIndex(ISimulation sim);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getIndex()
	 */
	public abstract int getIndex();

	/* (non-Javadoc)
	 * @see etomica.box.IBox#addNewMolecule(etomica.species.ISpecies)
	 */
	public abstract IMolecule addNewMolecule(ISpecies species);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#addMolecule(etomica.atom.IMolecule)
	 */
	public abstract void addMolecule(IMolecule molecule);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#removeMolecule(etomica.atom.IMolecule)
	 */
	public abstract void removeMolecule(IMolecule molecule);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#setNMolecules(etomica.species.ISpecies, int)
	 */
	public abstract void setNMolecules(ISpecies species, int n);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getNMolecules(etomica.species.ISpecies)
	 */
	public abstract int getNMolecules(ISpecies species);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getMoleculeList(etomica.species.ISpecies)
	 */
	public abstract IAtomSet getMoleculeList(ISpecies species);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getMoleculeList()
	 */
	public abstract IAtomSet getMoleculeList();

	/* (non-Javadoc)
	 * @see etomica.box.IBox#setBoundary(etomica.space.Boundary)
	 */
	public abstract void setBoundary(IBoundary b);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getBoundary()
	 */
	public abstract IBoundary getBoundary();

	public abstract void setDimensions(IVector d);

	public abstract void setDensity(double rho);

	public abstract Dimension getDensityDimension();

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getEventManager()
	 */
	public abstract IBoxEventManager getEventManager();

	public abstract void addSpeciesNotify(ISpecies species);

	/**
	 * Notifies the SpeciesMaster that a Species has been removed.  This method
	 * should only be called by the SpeciesManager.
	 */
	public abstract void removeSpeciesNotify(ISpecies species);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getLeafList()
	 */
	public abstract IAtomSet getLeafList();

	/* (non-Javadoc)
	 * @see etomica.box.IBox#requestGlobalIndex()
	 */
	public abstract int requestGlobalIndex();

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getMaxGlobalIndex()
	 */
	public abstract int getMaxGlobalIndex();

	/**
	 * Notifies the SpeciesMaster that the given number of new Atoms will be
	 * added to the system.  It's not required to call this method before
	 * adding atoms, but if adding many Atoms, calling this will improve
	 * performance.
	 */
	public abstract void notifyNewAtoms(int numNewAtoms, int numNewLeafAtoms);

	public abstract void addAtomNotify(IAtom newAtom);

	//updating of leaf atomList may not be efficient enough for repeated
	// use, but is probably ok
	public abstract void removeAtomNotify(IAtom oldAtom);

	/* (non-Javadoc)
	 * @see etomica.box.IBox#getLeafIndex(etomica.atom.IAtom)
	 */
	public abstract int getLeafIndex(IAtom atomLeaf);

}