package org.processmining.log.utils;

import static org.junit.Assert.assertEquals;

import org.deckfour.xes.model.XLog;
import org.junit.Test;

public class XUtilsTest {
	
	@Test
	public void testConceptName() {
		XLog log = XLogBuilder.newInstance().startLog("test").addTrace("trace").addEvent("event").build();
		assertEquals("test", XUtils.getConceptName(log));
		assertEquals("trace", XUtils.getConceptName(log.get(0)));
		assertEquals("event", XUtils.getConceptName(log.get(0).get(0)));
	}

}
