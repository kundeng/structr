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
package org.structr.core.graph;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.structr.api.Predicate;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;

/**
 * Encapsulates a bulk graph operation.
 *
 *
 */
public abstract class BulkGraphOperation<T> {

	private static final Logger logger = Logger.getLogger(BulkGraphOperation.class.getName());

	public abstract void handleGraphObject(SecurityContext securityContext, T obj) throws FrameworkException;

	public void handleThrowable(final SecurityContext securityContext, final Throwable t, final T currentObject) {
		logger.log(Level.WARNING, "Exception in bulk graph operation.");
		logger.log(Level.WARNING, "", t);
	}

	public void handleTransactionFailure(final SecurityContext securityContext, final Throwable t) {
		logger.log(Level.WARNING, "Transaction failure in bulk graph operation.");
		logger.log(Level.WARNING, "", t);
	}

	public Predicate<Long> getCondition() {
		return null;
	}
}
