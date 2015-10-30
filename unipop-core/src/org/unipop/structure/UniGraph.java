package org.unipop.structure;

import groovyjarjarcommonscli.MissingArgumentException;
import org.apache.commons.collections4.iterators.TransformIterator;
import org.apache.commons.configuration.Configuration;
import org.apache.tinkerpop.gremlin.process.computer.GraphComputer;
import org.apache.tinkerpop.gremlin.process.traversal.P;
import org.apache.tinkerpop.gremlin.process.traversal.TraversalStrategies;
import org.apache.tinkerpop.gremlin.process.traversal.step.util.HasContainer;
import org.apache.tinkerpop.gremlin.structure.*;
import org.apache.tinkerpop.gremlin.structure.util.ElementHelper;
import org.apache.tinkerpop.gremlin.structure.util.StringFactory;
import org.apache.tinkerpop.gremlin.util.iterator.ArrayIterator;
import org.unipop.controller.Predicates;
import org.unipop.controllerprovider.ControllerManager;
import org.unipop.process.UniGraphStrategy;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoTest$GraphSONTest", method = "shouldReadLegacyGraphSON",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoTest$GraphMLTest", method = "shouldReadGraphML",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoTest$GraphMLTest", method = "shouldReadGraphMLAnAllSupportedDataTypes",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoTest$GraphMLTest", method = "shouldReadGraphMLUnorderedElements",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoGraphTest", method = "shouldReadWriteClassic", specific="graphml",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoGraphTest", method = "shouldReadWriteClassicToFileWithHelpers", specific="graphml",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoGraphTest", method = "shouldMigrateClassicGraph", specific="graphml",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoGraphTest", method = "shouldReadWriteClassic", specific="gryo",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoGraphTest", method = "shouldReadWriteClassicToFileWithHelpers", specific="gryo",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.io.IoGraphTest", method = "shouldMigrateClassicGraph", specific="gryo",
        reason = "https://github.com/rmagen/elastic-gremlin/issues/52")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.VertexTest$BasicVertexTest", method = "shouldNotGetConcurrentModificationException",
        reason = "java.lang.IllegalStateException: Edge with id ... was removed.")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.FeatureSupportTest$VertexPropertyFunctionalityTest", method = "shouldSupportNumericIdsIfNumericIdsAreGeneratedFromTheGraph",
        reason = "need to handle ids in VertexProperties")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.GraphTest", method = "shouldHaveExceptionConsistencyWhenFindVertexByIdThatIsNonExistentViaIterator",
        reason = "elasticsearch - IllegalArgumentException[no terms provided]")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.structure.GraphTest", method = "shouldHaveExceptionConsistencyWhenFindEdgeByIdThatIsNonExistentViaIterator",
        reason = "elasticsearch - IllegalArgumentException[no terms provided]")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SubgraphTest$Traversals", method = "g_V_withSideEffectXsgX_repeatXbothEXcreatedX_subgraphXsgX_outVX_timesX5X_name_dedup",
        reason = "elasticsearch - IllegalArgumentException[no terms provided]")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.sideEffect.SubgraphTest$Traversals", method = "g_V_withSideEffectXsgX_outEXknowsX_subgraphXsgX_name_capXsgX",
        reason = "elasticsearch - IllegalArgumentException[no terms provided]")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.CountTest", method = "g_V_repeatXoutX_timesX8X_count",
        reason = "Takes too long.")
@Graph.OptOut(test = "org.apache.tinkerpop.gremlin.process.traversal.step.map.CountTest", method = "g_V_repeatXoutX_timesX3X_count",
        reason = "Takes too long.")
@Graph.OptIn(Graph.OptIn.SUITE_STRUCTURE_STANDARD)
@Graph.OptIn(Graph.OptIn.SUITE_PROCESS_STANDARD)
public class UniGraph implements Graph {
    static {
        TraversalStrategies.GlobalCache.registerStrategies(UniGraph.class, TraversalStrategies.GlobalCache.getStrategies(Graph.class).clone().addStrategies(UniGraphStrategy.instance()));
    }

    //for testSuite
    public static UniGraph open(final Configuration configuration) throws InstantiationException {
        return new UniGraph(configuration);
    }

    private UnipopFeatures features = new UnipopFeatures();
    private final Configuration configuration;
    private ControllerManager controllerManager;

    public UniGraph(Configuration configuration) throws InstantiationException {
        try {
            configuration.setProperty(Graph.GRAPH, UniGraph.class.getName());
            this.configuration = configuration;
            String queryHandlerName = configuration.getString("controllerManager");
            if(queryHandlerName != null) this.controllerManager = (ControllerManager)Class.forName(queryHandlerName).newInstance();
            else throw new MissingArgumentException("No ControllerManager configured.");//this.controllerManager = new SimpleControllerProvider();
            this.getControllerManager().init(this, configuration);
        } catch(Exception ex) {
            InstantiationException instantiationException = new InstantiationException();
            instantiationException.addSuppressed(ex);
            throw instantiationException;
        }
    }

    public ControllerManager getControllerManager() {
        return controllerManager;
    }

    public void commit() { controllerManager.commit(); }

    @Override
    public Configuration configuration() {
        return this.configuration;
    }

    @Override
    public String toString() {
        return StringFactory.graphString(this, controllerManager.toString());
    }

    @Override
    public void close() {
        controllerManager.close();
    }

    @Override
    public Features features() {
        return features;
    }

    @Override
    public Transaction tx() {
        throw Exceptions.transactionsNotSupported();
    }

    @Override
    public Variables variables() {
        throw Exceptions.variablesNotSupported();
    }


    @Override
    public <C extends GraphComputer> C compute(Class<C> graphComputerClass) throws IllegalArgumentException {
        throw Exceptions.graphComputerNotSupported();
    }

    @Override
    public GraphComputer compute() throws IllegalArgumentException {
        throw Exceptions.graphComputerNotSupported();
    }

    @Override
    public Iterator<Vertex> vertices(Object... ids) {
        if(ids.length == 0) return transform(controllerManager.vertices(new Predicates(), null));

        if (ids.length > 1 && !ids[0].getClass().equals(ids[1].getClass())) throw Graph.Exceptions.idArgsMustBeEitherIdOrElement();
        if (Vertex.class.isAssignableFrom(ids[0].getClass()))  return new ArrayIterator(ids);
        HasContainer hasContainer = new HasContainer(T.id.getAccessor(), P.eq(ids));
        Predicates predicates = new Predicates();
        predicates.hasContainers.add(hasContainer);
        return transform(controllerManager.vertices(predicates, null));
    }

    @Override
    public Iterator<Edge> edges(Object... ids) {
        if(ids.length == 0) return transform(controllerManager.edges(new Predicates(), null));

        if (ids.length > 1 && !ids[0].getClass().equals(ids[1].getClass())) throw Graph.Exceptions.idArgsMustBeEitherIdOrElement();
        if (Edge.class.isAssignableFrom(ids[0].getClass()))  return new ArrayIterator(ids);
        HasContainer hasContainer = new HasContainer(T.id.getAccessor(), P.eq(ids));
        Predicates predicates = new Predicates();
        predicates.hasContainers.add(hasContainer);
        return transform(controllerManager.edges(predicates, null));
    }

    @Override
    public Vertex addVertex(final Object... keyValues) {
        Map<String, Object> stringObjectMap = asMap(keyValues);
        Object idValue = ElementHelper.getIdValue(keyValues).orElse(null);
        final String label = ElementHelper.getLabelValue(keyValues).orElse(Vertex.DEFAULT_LABEL);
        stringObjectMap.remove("id");
        stringObjectMap.remove("label");
        return controllerManager.addVertex(idValue, label, stringObjectMap);
    }

    private <E,S>Iterator<E> transform(Iterator<S> source){
        return new TransformIterator<>(source, input -> (E) input);
    }

    public static Map<String, Object> asMap(Object[] keyValues){
        ElementHelper.legalPropertyKeyValueArray(keyValues);
        Map<String, Object> map = new HashMap<>();
        if (keyValues != null) {
            //if(keyValues.length % 2 == 1) throw Element.Exceptions.providedKeyValuesMustBeAMultipleOfTwo();
            for (int i = 0; i < keyValues.length; i = i + 2) {
                String key = keyValues[i].toString();
                Object value = keyValues[i + 1];
                ElementHelper.validateProperty(key,value);
                map.put(key, value);
            }
        }
        return map;
    }
}
