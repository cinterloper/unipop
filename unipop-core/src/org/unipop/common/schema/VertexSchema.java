package org.unipop.common.schema;

import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.unipop.query.predicates.PredicatesHolder;
import org.unipop.query.predicates.PredicatesHolder;

import java.util.List;

public interface VertexSchema extends ElementSchema<Vertex> {
    PredicatesHolder toPredicates(List<Vertex> vertices);
}
