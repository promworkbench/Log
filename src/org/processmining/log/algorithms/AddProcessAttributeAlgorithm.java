package org.processmining.log.algorithms;

import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.impl.XAttributeLiteralImpl;
import org.processmining.log.parameters.AddProcessAttributeParameters;

public class AddProcessAttributeAlgorithm {

	public XLog apply(XLog log, AddProcessAttributeParameters parameters) {

		XLog clonedLog = (XLog) log.clone();

		for (XTrace trace : clonedLog) {
			int last = parameters.isBackward() ? -1 : trace.size();
			int inc = parameters.isBackward() ? -1 : 1;

			for (int i = 0; i < trace.size(); i++) {
				parameters.getProcessAttributeValues().clear();
				for (int j = i + inc; j != last; j += inc) {
					if (parameters.getProcessAttributeValues().size() < parameters.getMaxCollectionSize()) {
						StringBuffer buffer = new StringBuffer();
						String separator = "";
						for (String attributeKey : parameters.getAttributeKeys()) {
							XAttribute attribute = trace.get(j).getAttributes().get(attributeKey);
							buffer.append(separator);
							separator = "+";
							buffer.append(attribute != null ? attribute.toString() : "");
						}
						String value = buffer.toString();
						if (parameters.getProcessAttributeValueFilter().contains(value)) {
							parameters.getProcessAttributeValues().add(value);
						}
					}
				}
				trace.get(i).getAttributes().put(parameters.getProcessAttributeKey(), new XAttributeLiteralImpl(
						parameters.getProcessAttributeKey(), parameters.getProcessAttributeValues().toString()));
			}
		}
		return clonedLog;
	}
}
