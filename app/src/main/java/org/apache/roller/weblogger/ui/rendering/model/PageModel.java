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

package org.apache.roller.weblogger.ui.rendering.model; 

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.weblogger.WebloggerException;
import org.apache.roller.weblogger.business.PropertiesManager;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.business.UserManager;
import org.apache.roller.weblogger.business.WeblogEntryManager;
import org.apache.roller.weblogger.business.themes.ThemeManager;
import org.apache.roller.weblogger.pojos.ThemeTemplate;
import org.apache.roller.weblogger.pojos.UserWeblogRole;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.pojos.WeblogCategory;
import org.apache.roller.weblogger.pojos.WeblogEntry;
import org.apache.roller.weblogger.ui.core.menu.Menu;
import org.apache.roller.weblogger.ui.core.menu.MenuHelper;
import org.apache.roller.weblogger.ui.rendering.mobile.MobileDeviceRepository.DeviceType;
import org.apache.roller.weblogger.ui.rendering.pagers.WeblogEntriesTimePager;
import org.apache.roller.weblogger.ui.rendering.pagers.WeblogEntriesTimePager.PagingInterval;
import org.apache.roller.weblogger.ui.rendering.pagers.WeblogEntriesPager;
import org.apache.roller.weblogger.ui.rendering.pagers.WeblogEntriesPermalinkPager;
import org.apache.roller.weblogger.ui.rendering.util.WeblogEntryCommentForm;
import org.apache.roller.weblogger.ui.rendering.util.WeblogPageRequest;
import org.apache.roller.weblogger.ui.rendering.util.WeblogRequest;


/**
 * Model which provides information needed to render a weblog page.
 */
public class PageModel implements Model {
    
    private static Log log = LogFactory.getLog(PageModel.class);
    
    private WeblogPageRequest pageRequest = null;
    private WeblogEntryCommentForm commentForm = null;
    private Map requestParameters = null;
    protected Weblog weblog = null;
    protected Weblog wrappedWeblog = null;
    private DeviceType deviceType = null;

    protected boolean isPreview = false;
    protected URLStrategy urlStrategy = null;

    public void setUrlStrategy(URLStrategy urlStrategy) {
        this.urlStrategy = urlStrategy;
    }

    protected WeblogEntryManager weblogEntryManager;

    public void setWeblogEntryManager(WeblogEntryManager weblogEntryManager) {
        this.weblogEntryManager = weblogEntryManager;
    }

    private UserManager userManager;

    public void setUserManager(UserManager userManager) {
        this.userManager = userManager;
    }

    protected PropertiesManager propertiesManager;

    public void setPropertiesManager(PropertiesManager propertiesManager) {
        this.propertiesManager = propertiesManager;
    }

    protected ThemeManager themeManager;

    public void setThemeManager(ThemeManager themeManager) {
        this.themeManager = themeManager;
    }

    /**
     * Creates an un-initialized new instance, Weblogger calls init() to complete
     * construction.
     */
    public PageModel() {}
    
    
    /** 
     * Template context name to be used for model.
     */
    public String getModelName() {
        return "model";
    }
    
    
    /** 
     * Init page model based on request. 
     */
    public void init(Map initData) throws WebloggerException {
        
        // we expect the init data to contain a weblogRequest object
        WeblogRequest weblogRequest = (WeblogRequest) initData.get("parsedRequest");
        if(weblogRequest == null) {
            throw new WebloggerException("expected weblogRequest from init data");
        }
        
        // PageModel only works on page requests, so cast weblogRequest
        // into a WeblogPageRequest and if it fails then throw exception
        if(weblogRequest instanceof WeblogPageRequest) {
            this.pageRequest = (WeblogPageRequest) weblogRequest;
        } else {
            throw new WebloggerException("weblogRequest is not a WeblogPageRequest."+
                    "  PageModel only supports page requests.");
        }
        
        // see if there is a comment form
        this.commentForm = (WeblogEntryCommentForm) initData.get("commentForm");
        
        // custom request parameters
        this.requestParameters = (Map)initData.get("requestParameters");
        
        // extract weblog object
        weblog = pageRequest.getWeblog();
        wrappedWeblog = weblog.templateCopy();

        this.deviceType = weblogRequest.getDeviceType();
    }    
    
    
    /**
     * Get the weblog locale used to render this page, null if no locale.
     */
    public String getLocale() {
        return null;
    }
    
    
    /**
     * Get weblog being displayed.
     */
    public Weblog getWeblog() {
        return wrappedWeblog;
    }

    /**
     * Is this page considered a permalink?
     */
    public boolean isPermalink() {
        return (pageRequest.getWeblogAnchor() != null);
    }

    /**
     * Is page in preview mode?
     */
    public boolean isPreview() {
        return isPreview;
    }

    /**
     * Is this page showing search results?
     */
    public boolean isSearchResults() {
        // the search results model will extend this class and override this
        return false;
    }
    
    
    /**
     * Get weblog entry being displayed or null if none specified by request.
     */
    public WeblogEntry getWeblogEntry() {
        if(pageRequest.getWeblogEntry() != null) {
            return pageRequest.getWeblogEntry().templateCopy();
        }
        return null;
    }
    
    
    /**
     * Get weblog entry being displayed or null if none specified by request.
     */
    public ThemeTemplate getWeblogPage() {
        if(pageRequest.getWeblogPageName() != null) {
            return pageRequest.getWeblogPage().templateCopy();
        } else {
            try {
                return themeManager.getTheme(weblog).getTemplateByAction(ThemeTemplate.ComponentType.WEBLOG).templateCopy();
            } catch (WebloggerException ex) {
                log.error("Error getting default page", ex);
            }
        }
        return null;
    }

    public ThemeTemplate getTemplateByName(String name)
            throws WebloggerException {
        ThemeTemplate templateToWrap = themeManager.getTheme(weblog).getTemplateByName(name);
        return templateToWrap.templateCopy();
    }

    /**
     * Get weblog category specified by request, or null if the category name
     * found in the request does not exist in the current weblog.
     */
    public WeblogCategory getWeblogCategory() {
        if(pageRequest.getWeblogCategory() != null) {
            return pageRequest.getWeblogCategory().templateCopy();
        }
        return null;
    }


    /**
     * Returns the list of tags specified in the request /tags/foo+bar
     */
    public List getTags() {
        return pageRequest.getTags();
    }
    

	/**
	 * Access to device type, which is either 'mobile' or 'standard'
	 * @return device type
	 */
	public String getDeviceType() {
		return deviceType.toString();
	}


    /**
     * A map of entries representing this page. The collection is grouped by 
     * days of entries.  Each value is a list of entry objects keyed by the 
     * date they were published.
     */
    public WeblogEntriesPager getWeblogEntriesPager() {
        return getWeblogEntriesPager(null, null);
    }
    
    public WeblogEntriesPager getWeblogEntriesPager(String catArgument, String tagArgument) {
        
        // category specified by argument wins over request parameter
        String cat = pageRequest.getWeblogCategoryName();
        if (catArgument != null && !StringUtils.isEmpty(catArgument) && !"nil".equals(catArgument)) {
            cat = catArgument;
        }
        
        List<String> tags = pageRequest.getTags();
        if (tagArgument != null && !StringUtils.isEmpty(tagArgument) && !"nil".equals(tagArgument)) {
            tags = new ArrayList<>();
            tags.add(tagArgument);
        }
        
        String dateString = pageRequest.getWeblogDate();
        
        // determine which mode to use
        if (pageRequest.getWeblogAnchor() != null) {
            return new WeblogEntriesPermalinkPager(
                    weblogEntryManager,
                    urlStrategy,
                    weblog,
                    pageRequest.getWeblogPageName(),
                    pageRequest.getWeblogAnchor(),
                    cat,
                    tags,
                    true);
        } else {
            PagingInterval interval = PagingInterval.LATEST;

            if (dateString != null) {
                int len = dateString.length();
                if (len == 8) {
                    interval = PagingInterval.DAY;
                } else if (len == 6) {
                    interval = PagingInterval.MONTH;
                }
            }

            return new WeblogEntriesTimePager(
                    interval,
                    weblogEntryManager,
                    propertiesManager,
                    urlStrategy,
                    weblog,
                    pageRequest.getWeblogDate(),
                    cat,
                    tags,
                    pageRequest.getPageNum());
        }
    }

    /**
     * Get comment form to be displayed, may contain preview data.
     *
     * @return Comment form object
     */
    public WeblogEntryCommentForm getCommentForm() {
        
        if(commentForm == null) {
            commentForm = new WeblogEntryCommentForm();
        }
        return commentForm;
    }
    
    /**
     * Get request parameter by name.
     */
    public String getRequestParameter(String paramName) {
        if (requestParameters != null) {
            String[] values = (String[])requestParameters.get(paramName);
            if (values != null && values.length > 0) {
                return values[0];
            }
        }
        return null;
    }

    /**
     * Get a Menu representing the editor UI action menu, if the user is
     * currently logged in.
     */
    public Menu getEditorMenu() {
        try {
            if (pageRequest.isLoggedIn()) {
                UserWeblogRole uwr = userManager.getWeblogRole(pageRequest.getUser(), weblog);
                return MenuHelper.generateMenu("editor", null, pageRequest.getUser(), pageRequest.getWeblog(), uwr.getWeblogRole());
            } else {
                return null;
            }
        } catch (Exception e) {
            log.error("GetWeblogRole() failed for user: " + pageRequest.getUser() + " and weblog: " + pageRequest.getWeblog());
            return null;
        }
    }
}
