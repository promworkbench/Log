package org.processmining.filtering.filter.implementations;

import java.util.ArrayList;
import java.util.List;

import org.processmining.filtering.filter.interfaces.Filter;
import org.processmining.filtering.filter.interfaces.FilterStack;

/**
 * @see FilterStack
 * 
 * @author S.J. van Zelst
 * 
 * @param <T>
 *            generic type on which this filter stack should be applied.
 */
public class FilterStackImpl<T> extends ArrayList<Filter<T>> implements FilterStack<T> {

	public FilterStackImpl(List<Filter<T>> filters) {
		this.addAll(filters);
	}

	@Override
	public T apply(T t) {
		T result = t;
		for (Filter<T> filter : this) {
			result = filter.apply(result);
		}
		return result;
	}

}
