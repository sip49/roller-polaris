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

package org.apache.roller.weblogger.ui.rendering.pagers;

import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.commons.lang3.StringUtils;
import org.apache.roller.weblogger.WebloggerUtils;
import org.apache.roller.weblogger.config.WebloggerRuntimeConfig;
import org.apache.roller.weblogger.pojos.Weblog;
import org.apache.roller.weblogger.business.URLStrategy;
import org.apache.roller.weblogger.util.I18nMessages;

/**
 * An abstract implementation of a WeblogEntriesPager.
 *
 * This implementation lays out the basic functionality of an entries pager so
 * that subclasses can easily tweak only the few things necessary to handle
 * paging their own way.
 */
public abstract class AbstractWeblogEntriesPager implements WeblogEntriesPager {
    
    // message utils for doing i18n messages
    I18nMessages messageUtils = null;
    
    // url strategy for building urls
    URLStrategy urlStrategy = null;
    
    Weblog weblog = null;
    String pageLink = null;
    String entryAnchor = null;
    String dateString = null;
    String catName = null;
    List<String> tags = new ArrayList<>();
    int offset = 0;
    int page = 0;
    int length = 0;
    
    
    public AbstractWeblogEntriesPager(
            URLStrategy        strat,
            Weblog             weblog,
            String             pageLink,
            String             entryAnchor,
            String             dateString,
            String             catName,
            List<String>       tags,
            int                page) {
        
        this.urlStrategy = strat;
        
        this.weblog = weblog;
        this.pageLink = pageLink;
        this.entryAnchor = entryAnchor;
        this.dateString = dateString;
        this.catName = catName;
        
        if (tags != null) {
            this.tags = tags;
        }
        
        // make sure offset, length, and page are valid
        int maxLength = WebloggerRuntimeConfig.getIntProperty("site.pages.maxEntries");
        length = weblog.getEntryDisplayCount();
        if(length > maxLength) {
            length = maxLength;
        }
        
        if(page > 0) {
            this.page = page;
        }
        this.offset = length * page;
        
        // get a message utils instance to handle i18n of messages
        Locale viewLocale = weblog.getLocaleInstance();
        this.messageUtils = I18nMessages.getMessages(viewLocale);
    }
    
    
    public boolean hasMoreEntries() {
        return false;
    }
    
    
    public String getHomeLink() {
        return createURL(0, 0, weblog, pageLink, entryAnchor, dateString, catName, tags);
    }
    
    
    public String getHomeName() {
        return messageUtils.getString("weblogEntriesPager.latest.home");
    }
    
    
    public String getNextLink() {
        if (hasMoreEntries()) {
            return createURL(page, 1, weblog, pageLink, entryAnchor, dateString, catName, tags);
        }
        return null;
    }
    
    
    public String getNextName() {
        if (hasMoreEntries()) {
            return messageUtils.getString("weblogEntriesPager.latest.next");
        }
        return null;
    }
    
    
    public String getPrevLink() {
        if (page > 0) {
            return createURL(page, -1, weblog, pageLink, entryAnchor, dateString, catName, tags);
        }
        return null;
    }
    
    
    public String getPrevName() {
        if (page > 0) {
            return messageUtils.getString("weblogEntriesPager.latest.prev");
        }
        return null;
    }
    
    
    public String getNextCollectionLink() {
        return null;
    }
    
    
    public String getNextCollectionName() {
        return null;
    }
    
    
    public String getPrevCollectionLink() {
        return null;
    }
    
    
    public String getPrevCollectionName() {
        return null;
    }
    
    
    /**
     * Parse data as either 6-char or 8-char format.
     */
    protected Date parseDate(String dateString) {
        Date ret = null;
        if (   dateString!=null
                && dateString.length()==8
                && StringUtils.isNumeric(dateString) ) {
            FastDateFormat char8DateFormat = FastDateFormat.getInstance(WebloggerUtils.FORMAT_8CHARS,
                    weblog.getTimeZoneInstance(), weblog.getLocaleInstance());
            ParsePosition pos = new ParsePosition(0);
            ret = char8DateFormat.parse(dateString, pos);
            
            // make sure the requested date is not in the future
            Date today = getToday();
            if (ret.after(today)) {
                ret = today;
            }
        }
        if (   dateString!=null
                && dateString.length()==6
                && StringUtils.isNumeric(dateString) ) {
            FastDateFormat char6DateFormat = FastDateFormat.getInstance(WebloggerUtils.FORMAT_6CHARS,
                    weblog.getTimeZoneInstance(), weblog.getLocaleInstance());
            ParsePosition pos = new ParsePosition(0);
            ret = char6DateFormat.parse( dateString, pos );
            
            // make sure the requested date is not in the future
            Date today = getToday();
            if (ret.after(today)) {
                ret = today;
            }
        }
        return ret;
    }
    
    
    /**
     * Return today based on current blog's timezone/locale.
     */
    protected Date getToday() {
        Calendar todayCal = Calendar.getInstance(
                weblog.getTimeZoneInstance(), weblog.getLocaleInstance());
        todayCal.setTime(new Date());
        return todayCal.getTime();
    }
    
    
    /**
     * Create URL that encodes pager state using most appropriate forms of URL.
     * @param pageAdd To be added to page number, or 0 for no page number
     */
    protected String createURL(
            int                page,
            int                pageAdd,
            Weblog        website,
            String             pageLink,
            String             entryAnchor,
            String             dateString,
            String             catName,
            List               tags) {
        
        int pageNum = page + pageAdd;
        
        if (pageLink != null) {
            return urlStrategy.getWeblogPageURL(website, pageLink, entryAnchor, catName, dateString, tags, pageNum, false);
        } else if (entryAnchor != null) {
            return urlStrategy.getWeblogEntryURL(website, entryAnchor, true);
        }
        
        return urlStrategy.getWeblogCollectionURL(website, catName, dateString, tags, pageNum, false);
    }
    
}
