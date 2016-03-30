/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  The ASF licenses this file to You
 * under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.  For additional information regarding
 * copyright in this work, please see the NOTICE file in the top level
 * directory of this distribution.
 *
 * Source file modified from the original ASF source; all changes made
 * are also under Apache License.
 */

package org.apache.roller.weblogger.ui.struts2.editor;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerCommon;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.WeblogManager;
import org.apache.roller.weblogger.business.WebloggerFactory;
import org.apache.roller.weblogger.pojos.WeblogTemplateRendition;
import org.apache.roller.weblogger.pojos.GlobalRole;
import org.apache.roller.weblogger.pojos.TemplateRendition.RenditionType;
import org.apache.roller.weblogger.pojos.TemplateRendition.TemplateLanguage;
import org.apache.roller.weblogger.pojos.ThemeTemplate.ComponentType;
import org.apache.roller.weblogger.pojos.WeblogTemplate;
import org.apache.roller.weblogger.ui.struts2.util.UIAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Templates listing page.
 */
public class Templates extends UIAction {

	private static Log log = LogFactory.getLog(Templates.class);

	// list of templates to display
	private List<WeblogTemplate> templates = Collections.emptyList();

	// list of template action types user is allowed to create
	private Map<ComponentType, String> availableActions = Collections.emptyMap();

	// name and action of new template if we are adding a template
	private String newTmplName = null;
	private ComponentType newTmplAction = null;

    private WeblogManager weblogManager;

    public void setWeblogManager(WeblogManager weblogManager) {
        this.weblogManager = weblogManager;
    }

    public Templates() {
		this.actionName = "templates";
		this.desiredMenu = "editor";
		this.pageTitle = "pagesForm.title";
	}

    @Override
    public GlobalRole requiredGlobalRole() {
        return GlobalRole.BLOGGER;
    }

	public String execute() {

		// query for templates list
		try {

			// get current list of templates, minus custom stylesheet
			List<WeblogTemplate> raw = weblogManager.getTemplates(getActionWeblog());
			List<WeblogTemplate> pages = new ArrayList<>();
			pages.addAll(raw);
			setTemplates(pages);

			// build list of action types that may be added
			Map<ComponentType, String> actionsMap = new EnumMap<>(ComponentType.class);
			addComponentTypeToMap(actionsMap, ComponentType.WEBLOG);
			addComponentTypeToMap(actionsMap, ComponentType.PERMALINK);
			addComponentTypeToMap(actionsMap, ComponentType.SEARCH);
			addComponentTypeToMap(actionsMap, ComponentType.TAGSINDEX);
			addComponentTypeToMap(actionsMap, ComponentType.JAVASCRIPT);
			addComponentTypeToMap(actionsMap, ComponentType.STYLESHEET);
			addComponentTypeToMap(actionsMap, ComponentType.CUSTOM);

			// remove from above list any already existing for the theme
            for (WeblogTemplate tmpPage : getTemplates()) {
                if (tmpPage.getAction().isSingleton()) {
                    actionsMap.remove(tmpPage.getAction());
                }
            }
			setAvailableActions(actionsMap);

		} catch (WebloggerException ex) {
			log.error("Error getting templates for weblog - " + getActionWeblog().getHandle(), ex);
			addError("Error getting template list - check server logfiles");
		}

		return LIST;
	}

    private void addComponentTypeToMap(Map<ComponentType, String> map, ComponentType component) {
        map.put(component, component.getReadableName());
    }

	/**
	 * Save a new template.
	 */
	public String add() {

		// validation
		myValidate();

		if (!hasActionErrors()) {
            try {

                WeblogTemplate newTemplate = new WeblogTemplate();
                newTemplate.setWeblog(getActionWeblog());
                newTemplate.setAction(getNewTmplAction());
                newTemplate.setName(getNewTmplName());
                newTemplate.setHidden(false);
                newTemplate.setNavbar(false);
                newTemplate.setLastModified(new Date());

                if (!getNewTmplAction().isSingleton()) {
                    newTemplate.setLink(getNewTmplName());
                }

                // Make sure we have always have a Weblog main page. Stops
                // deleting main page in custom theme mode also.
                if (ComponentType.WEBLOG.equals(getNewTmplAction())) {
                    newTemplate.setName(ComponentType.WEBLOG.name());
                }

                // save the new Template
                weblogManager.saveTemplate(newTemplate);

                // Create weblog template codes for available types.
                WeblogTemplateRendition standardRendition = new WeblogTemplateRendition(
                        newTemplate, RenditionType.STANDARD);
                standardRendition.setTemplate(getText("pageForm.newTemplateContent"));
                standardRendition.setTemplateLanguage(TemplateLanguage.VELOCITY);
                weblogManager.saveTemplateRendition(standardRendition);

                /* TBI -- need a way for user to specify dual or single template
                WeblogTemplateRendition mobileRendition = new WeblogTemplateRendition(
                        newTemplate.getId(), RenditionType.MOBILE);
                mobileRendition.setTemplate(newTemplate.getContents());
                mobileRendition.setTemplateLanguage(TemplateLanguage.VELOCITY);
                weblogManager.saveTemplateRendition(mobileRendition);
                */

                // if this person happened to create a Weblog template from
                // scratch then make sure and set the defaultPageId. What does
                // this do????
                if (ComponentType.WEBLOG.name().equals(newTemplate.getName())) {
                    weblogManager.saveWeblog(getActionWeblog());
                }

                // flush results to db
                WebloggerFactory.flush();

                // reset form fields
                setNewTmplName(null);
                setNewTmplAction(null);

            } catch (WebloggerException ex) {
                log.error("Error adding new template for weblog - "
                        + getActionWeblog().getHandle(), ex);
                addError("Error adding new template - check TightBlog logs");
            }
        }

		return execute();
	}

	// validation when adding a new template
	private void myValidate() {

		// make sure name is non-null and within proper size
		if (StringUtils.isEmpty(getNewTmplName())) {
			addError("Template.error.nameNull");
		} else if (getNewTmplName().length() > WebloggerCommon.TEXTWIDTH_255) {
			addError("Template.error.nameSize");
		}

		// make sure action is a valid
		if (getNewTmplAction() == null) {
			addError("Template.error.actionNull");
		}

		// check if template by that name already exists
		try {
			WeblogTemplate existingPage = weblogManager.getTemplateByName(getActionWeblog(),
                    getNewTmplName());
			if (existingPage != null) {
				addError("pagesForm.error.alreadyExists", getNewTmplName());
			}
		} catch (WebloggerException ex) {
			log.error("Error checking for existing template", ex);
		}

	}

	public List<WeblogTemplate> getTemplates() {
		return templates;
	}

	public void setTemplates(List<WeblogTemplate> templates) {
		this.templates = templates;
	}

	public Map<ComponentType, String> getAvailableActions() {
		return availableActions;
	}

	public void setAvailableActions(Map<ComponentType, String> availableActions) {
		this.availableActions = availableActions;
	}

	public String getNewTmplName() {
		return newTmplName;
	}

	public void setNewTmplName(String newTmplName) {
		this.newTmplName = newTmplName;
	}

	public ComponentType getNewTmplAction() {
		return newTmplAction;
	}

	public void setNewTmplAction(ComponentType newTmplAction) {
		this.newTmplAction = newTmplAction;
	}

}
