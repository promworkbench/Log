package org.processmining.filtering.filter.implementations;

import org.processmining.filtering.filter.interfaces.Filter;

/**
 * A mirror filter is a standard defined filter that (as the name suggests)
 * mirrors each element. Thus mirror function m : T -> T with m(t) = t.
 * 
 * @param <T>
 *            generic type on which this filter is applied.
 * 
 * @author S.J. van Zelst
 */
public class MirrorFilterImpl<T> implements Filter<T> {

	@Override
	public T apply(T t) {
		return t;
	}

	@SuppressWarnings("unchecked")
	public Object clone() {
		MirrorFilterImpl<T> clone = null;
		try {
			clone = (MirrorFilterImpl<T>) super.clone();
		} catch (CloneNotSupportedException e) {
			e.printStackTrace();
		}
		return clone;
	}
}
