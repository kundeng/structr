/**
 * Copyright (C) 2010-2013 Axel Morgner, structr <structr@structr.org>
 *
 * This file is part of structr <http://structr.org>.
 *
 * structr is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * structr is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with structr.  If not, see <http://www.gnu.org/licenses/>.
 */



package org.structr.web.resource;

import org.structr.common.MailHelper;
import org.structr.common.SecurityContext;
import org.structr.common.error.FrameworkException;
import org.structr.core.Result;
import org.structr.core.Services;
import org.structr.core.entity.AbstractNode;
import org.structr.core.graph.CreateNodeCommand;
import org.structr.core.graph.NodeAttribute;
import org.structr.core.property.PropertyKey;
import org.structr.rest.RestMethodResult;
import org.structr.rest.exception.NotAllowedException;
import org.structr.rest.resource.Resource;
import org.structr.web.entity.User;

//~--- JDK imports ------------------------------------------------------------

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.structr.core.graph.StructrTransaction;
import org.structr.core.graph.TransactionCommand;
import org.structr.core.graph.search.Search;
import org.structr.core.graph.search.SearchNodeCommand;
import org.structr.core.graph.search.SearchOperator;
import org.structr.web.entity.dom.Content;
import org.structr.web.entity.mail.MailTemplate;

//~--- classes ----------------------------------------------------------------

/**
 * A resource to register new users
 *
 * @author Axel Morgner
 */
public class RegistrationResource extends Resource {

	private static final Logger logger = Logger.getLogger(RegistrationResource.class.getName());
	
	private enum TemplateKey {
		SENDER_NAME,
		SENDER_ADDRESS,
		SUBJECT,
		TEXT_BODY,
		HTML_BODY,
		BASE_URL
	}

	private static String localeString;
	
	//~--- methods --------------------------------------------------------

	@Override
	public boolean checkAndConfigure(String part, SecurityContext securityContext, HttpServletRequest request) {

		this.securityContext = securityContext;

		return ("registration".equals(part));

	}

	@Override
	public Result doGet(PropertyKey sortKey, boolean sortDescending, int pageSize, int page, String offsetId) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doPut(Map<String, Object> propertySet) throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doPost(Map<String, Object> propertySet) throws FrameworkException {

		if (propertySet.containsKey(User.email.jsonName())) {

			final String emailString  = (String) propertySet.get(User.email.jsonName());
			localeString = (String) propertySet.get(MailTemplate.locale.jsonName());
			
			
			User newUser = Services.command(securityContext, TransactionCommand.class).execute(new StructrTransaction<User>() {

				@Override
				public User execute() throws FrameworkException {

					return (User) Services.command(securityContext, CreateNodeCommand.class).execute(
						new NodeAttribute(AbstractNode.type, User.class.getSimpleName()),
						new NodeAttribute(User.email, emailString));
				}

			});

			if (newUser != null) {

				sendInvitationLink(newUser);
			}

		}

		// return 200 OK
		return new RestMethodResult(HttpServletResponse.SC_OK);

	}

	@Override
	public RestMethodResult doHead() throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public RestMethodResult doOptions() throws FrameworkException {

		throw new NotAllowedException();

	}

	@Override
	public Resource tryCombineWith(Resource next) throws FrameworkException {

		return null;

	}

	private void sendInvitationLink(final User user) {

		Map<String, String> replacementMap = new HashMap();

		String userEmail = user.getProperty(User.email);
		
		replacementMap.put(toPlaceholder(User.email.jsonName()), userEmail);
		replacementMap.put(toPlaceholder("link"), getTemplateText(TemplateKey.BASE_URL) + "/register?id=" + user.getUuid());

		String textMailTemplate = getTemplateText(TemplateKey.TEXT_BODY);
		String htmlMailTemplate = getTemplateText(TemplateKey.HTML_BODY);
		String textMailContent  = MailHelper.replacePlaceHoldersInTemplate(textMailTemplate, replacementMap);
		String htmlMailContent  = MailHelper.replacePlaceHoldersInTemplate(htmlMailTemplate, replacementMap);

		try {

			MailHelper.sendHtmlMail(
				getTemplateText(TemplateKey.SENDER_ADDRESS),
				getTemplateText(TemplateKey.SENDER_NAME),
				userEmail, "", null, null, null,
				getTemplateText(TemplateKey.SUBJECT),
				htmlMailContent, textMailContent);

		} catch (Exception e) {

			logger.log(Level.SEVERE, "Unable to send e-mail", e);
		}

	}

	private String getTemplateText(final TemplateKey key) {
		try {
			List<MailTemplate> templates = (List<MailTemplate>) Services.command(SecurityContext.getSuperUserInstance(), SearchNodeCommand.class).execute(
				Search.andExactType(MailTemplate.class.getSimpleName()),
				Search.andExactName(key.name()),
				Search.andMatchExactValues(MailTemplate.locale, localeString, SearchOperator.AND)
			).getResults();
			
			if (!templates.isEmpty()) {
				
				return templates.get(0).getProperty(MailTemplate.text).getProperty(Content.content);
				
			}
			
		} catch (FrameworkException ex) {
			
			Logger.getLogger(RegistrationResource.class.getName()).log(Level.WARNING, "Could not get mail template for key " + key, ex);
			
		}
		
		return null;
		
	}
	
	private static String toPlaceholder(final String key) {

		return "${".concat(key).concat("}");

	}
	
	
	//~--- get methods ----------------------------------------------------

	@Override
	public Class getEntityClass() {

		return null;

	}

	@Override
	public String getUriPart() {

		return "registration";

	}

	@Override
	public String getResourceSignature() {

		return getUriPart();

	}

	@Override
	public boolean isCollectionResource() {

		return false;

	}

}
