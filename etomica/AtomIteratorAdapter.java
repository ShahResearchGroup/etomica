/*
 * History
 * Created on Aug 13, 2004 by kofke
 */
package etomica;

/**
 * Adapater class that wraps another AtomIterator to implement the
 * methods of an iterator.  In the default case, all methods of the
 * adapter merely front the methods of the wrapped iterator.
 * Typically, subclasses will introduce additional
 * methods that configure the wrapped iterator in a particular way to
 * prepare it for iteration.  Subclasses may also override individual
 * methods as appropriate to their implementation.
 */
public abstract class AtomIteratorAdapter extends AtomsetIteratorAdapter implements AtomIterator {

	protected final AtomIterator atomIterator;
	
	/**
	 * Constructor takes the wrapped iterator as an argument. The
	 * iterator is final and cannot be subsequently replaced by
	 * another iterator.
	 */
	public AtomIteratorAdapter(AtomIterator iterator) {
		super(iterator);
		atomIterator = iterator;
	}
	
	public Atom nextAtom() {
		return atomIterator.nextAtom();
	}
}
