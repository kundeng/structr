/**
 * Copyright (C) 2010-2016 Structr GmbH
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
package org.structr.core.property;

import org.structr.common.SecurityContext;
import org.structr.core.GraphObject;
import org.structr.core.app.StructrApp;
import org.structr.core.converter.PropertyConverter;
import org.structr.core.converter.RelatedNodePropertyMapper;

/**
 * A property that can be used to make the property of a related node
 * appear to be a local property. Use this property type if you want
 * to reproduce the value of a given node on a node with a different
 * type. This propert works in both directions, i.e. you can get and
 * set the value as if it was a local property.
 *
 *
 */
public class RelatedNodeProperty<T> extends AbstractPrimitiveProperty<T> {

	private PropertyKey sourceKey  = null;
	private PropertyKey<T> targetKey = null;

	public RelatedNodeProperty(String name, PropertyKey sourceKey, PropertyKey<T> targetKey) {
		super(name);

		this.sourceKey  = sourceKey;
		this.targetKey  = targetKey;

		// make us known to the entity context
		StructrApp.getConfiguration().registerConvertedProperty(this);
	}

	@Override
	public String typeName() {
		return "FIXME: RelatedNodeProperty.java:49";
	}

	@Override
	public Class valueType() {
		return sourceKey.valueType();
	}

	@Override
	public Property<T> indexed() {

		// related node properties are always indexed passively
		// (because they can change without setProperty())
		super.passivelyIndexed();
		return this;
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext) {
		return databaseConverter(securityContext, null);
	}

	@Override
	public PropertyConverter<T, ?> databaseConverter(SecurityContext securityContext, GraphObject currentObject) {
		return new RelatedNodePropertyMapper(securityContext, currentObject, sourceKey, targetKey);
	}

	@Override
	public PropertyConverter<?, T> inputConverter(SecurityContext securityContext) {
		return targetKey.inputConverter(securityContext);
	}

	@Override
	public Object fixDatabaseProperty(Object value) {
		return null;
	}

	@Override
	public Integer getSortType() {
		return targetKey.getSortType();
	}
}
