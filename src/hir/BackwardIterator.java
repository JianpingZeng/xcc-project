package hir;

import java.util.ListIterator;

public class BackwardIterator<E> implements ListIterator<E> {

	private ListIterator<E> itr;
		
	public BackwardIterator(ListIterator<E> itr)
	{
		this.itr = itr;
	}
	
	@Override
	public boolean hasNext() {
		return itr.hasPrevious();
	}

	@Override
	public E next() {
		return itr.previous();
	}

	@Override
	public boolean hasPrevious() {
		return itr.hasNext();
	}

	@Override
	public E previous() {
		
		return itr.next();
	}

	@Override
	public int nextIndex() {

		return itr.previousIndex();
	}

	@Override
	public int previousIndex() {
		return itr.nextIndex();
	}

	@Override
	public void remove() {
		itr.remove();		
	}

	@Override
	public void set(E e) {			
		itr.set(e);
	}

	@Override
	public void add(E e) {
		
		itr.add(e);
	}		
}
