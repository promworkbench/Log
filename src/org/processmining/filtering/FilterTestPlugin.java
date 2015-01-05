package org.processmining.filtering;

import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.processmining.contexts.uitopia.UIPluginContext;
import org.processmining.contexts.uitopia.annotations.UITopiaVariant;
import org.processmining.filter.factories.FilterFactory;
import org.processmining.filter.interfaces.Filter;
import org.processmining.framework.plugin.annotations.Plugin;
import org.processmining.framework.plugin.annotations.PluginVariant;
import org.processmining.xflog.implementations.XFLogImpl;
import org.processmining.xflog.interfaces.XFLog;

@Plugin(name = "Filter test", parameterLabels = {"Event log"}, returnLabels = {"Log"}, returnTypes = {XFLog.class})
@SuppressWarnings("unused")
public class FilterTestPlugin {

    @UITopiaVariant(affiliation = "Eindhoven University of Technology", author = "S.J. van Zelst", email = "s.j.v.zelst@tue.nl")
    @PluginVariant(variantLabel = "Filter test", requiredParameterLabels = {0})
    /**
     * ProM entry point for streaming LPEngineLpSolveImpl plug-in
     *
     * @param context
     * @param log
     * @return
     */
    public XFLog filter(final UIPluginContext context, final XLog log) {
        Filter<XTrace> traceFilter = new LeaveEventOutIfConceptNameContainsFilterImpl("e");
        Filter<XAttributeMap> attributeMapFilter = FilterFactory.mirrorFilter();

        XFLog fLog = new XFLogImpl(log, traceFilter, attributeMapFilter);

        return fLog;
    }

}
