/**
 * Copyright (C) 2010-2017 Structr GmbH
 *
 * This file is part of Structr <http://structr.org>.
 *
 * Structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * Structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Structr.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.structr.core.entity;

import java.util.LinkedHashMap;
import org.structr.api.graph.Direction;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.GraphObject;
import org.structr.core.app.App;
import org.structr.core.app.StructrApp;
import org.structr.core.graph.NodeInterface;
import org.structr.core.graph.RelationshipInterface;
import org.structr.core.notion.Notion;
import org.structr.core.notion.RelationshipNotion;

/**
 *
 *
 */
public abstract class OneToMany<S extends NodeInterface, T extends NodeInterface> extends AbstractRelationship<S, T> implements Relation<S, T, OneStartpoint<S>, ManyEndpoint<T>> {

	@Override
	public Multiplicity getSourceMultiplicity() {
		return Multiplicity.One;
	}

	@Override
	public Multiplicity getTargetMultiplicity() {
		return Multiplicity.Many;
	}

	@Override
	public OneStartpoint<S> getSource() {
		return new OneStartpoint<>(this);
	}

	@Override
	public ManyEndpoint<T> getTarget() {
		return new ManyEndpoint<>(this);
	}

	@Override
	public int getCascadingDeleteFlag() {
		return Relation.NONE;
	}

	@Override
	public int getAutocreationFlag() {
		return Relation.NONE;
	}

	@Override
	public void ensureCardinality(final SecurityContext securityContext, final NodeInterface sourceNode, final NodeInterface targetNode) throws FrameworkException {

		final App app = StructrApp.getInstance();

		if (targetNode != null) {

			final LinkedHashMap<String, Object> props = new LinkedHashMap();
			props.put("targetId",       targetNode.getUuid());
			props.put("structrRelType", getClass().getSimpleName());

			for (GraphObject incomingRel : app.cypher("MATCH ()-[r]->(t:" + targetNode.getType() + ") WHERE r.type = {structrRelType} AND t.id = {targetId} RETURN r", props)) {
				app.delete((RelationshipInterface) incomingRel);
			}
		}
	}

	@Override
	public Notion getEndNodeNotion() {
		return new RelationshipNotion(getTargetIdProperty());

	}

	@Override
	public Notion getStartNodeNotion() {
		return new RelationshipNotion(getSourceIdProperty());
	}

	@Override
	public Direction getDirectionForType(final Class<? extends NodeInterface> type) {
		return super.getDirectionForType(getSourceType(), getTargetType(), type);
	}

	@Override
	public Class getOtherType(final Class type) {

		switch (getDirectionForType(type)) {

			case INCOMING: return getSourceType();
			case OUTGOING: return getTargetType();
			case BOTH:     return getSourceType();	// don't know...
		}

		return null;
	}

	@Override
	public boolean isHidden() {
		return false;
	}

	@Override
	public boolean isInternal() {
		return false;
	}
}