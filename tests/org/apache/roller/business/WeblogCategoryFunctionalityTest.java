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
 */

package org.apache.roller.business;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import junit.framework.TestCase;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.roller.TestUtils;
import org.apache.roller.pojos.UserData;
import org.apache.roller.pojos.WeblogCategoryData;
import org.apache.roller.pojos.WeblogEntryData;
import org.apache.roller.pojos.WebsiteData;
import org.apache.roller.util.Utilities;


/**
 * Test Weblog Category related business operations.
 */
public class WeblogCategoryFunctionalityTest extends TestCase {
    
    public static Log log = LogFactory.getLog(WeblogCategoryFunctionalityTest.class);
    
    UserData testUser = null;
    WebsiteData testWeblog = null;
    WeblogCategoryData cat1 = null;
    WeblogCategoryData cat2 = null;
    WeblogCategoryData cat3 = null;
    WeblogCategoryData testCat = null;
    
    
    /**
     * All tests in this suite require a user and a weblog.
     */
    public void setUp() {
        
        log.info("BEGIN");
        
        try {
            testUser = TestUtils.setupUser("categoryTestUser");
            testWeblog = TestUtils.setupWeblog("categoryTestWeblog", testUser);
            
            // setup a category tree to use for testing
            cat1 = TestUtils.setupWeblogCategory(testWeblog, "catTest-cat1", null);
            cat2 = TestUtils.setupWeblogCategory(testWeblog, "catTest-cat2", cat1);
            cat3 = TestUtils.setupWeblogCategory(testWeblog, "catTest-cat3", cat2);
            
            // a simple test cat at the root level
            testCat = TestUtils.setupWeblogCategory(testWeblog, "catTest-testCat", null);
            
            TestUtils.endSession(true);
        } catch (Throwable t) {
            log.error(t);
        }
        
        log.info("END");
    }
    
    public void tearDown() {
        
        log.info("BEGIN");
        
        try {
            TestUtils.teardownWeblog(testWeblog.getId());
            TestUtils.teardownUser(testUser.getId());
            TestUtils.endSession(true);
        } catch (Throwable t) {
            log.error(t);
        }
        
        log.info("END");
    }
    
    
    /**
     * Test that we can walk a category tree.
     */
    public void testWalkCategoryTree() throws Exception {
        
        log.info("BEGIN");
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        // start at root
        WeblogCategoryData root = mgr.getRootWeblogCategory(TestUtils.getManagedWebsite(testWeblog));
        
        // walk first level
        Set cats = root.getWeblogCategories();
        assertEquals(2, cats.size());
        assertTrue(cats.contains(testCat));
        
        // find cat1
        WeblogCategoryData cat = null;
        for(Iterator it = cats.iterator(); it.hasNext(); ) {
            cat = (WeblogCategoryData) it.next();
            if(cat.getName().equals(cat1.getName())) {
                break;
            }
        }
        
        // walk second level
        cats = cat.getWeblogCategories();
        assertEquals(1, cats.size());
        assertTrue(cats.contains(cat2));
        
        // find cat2
        cat = (WeblogCategoryData) cats.iterator().next();
        
        // walk third level
        cats = cat.getWeblogCategories();
        assertEquals(1, cats.size());
        assertTrue(cats.contains(cat3));
        
        // find cat3
        cat = (WeblogCategoryData) cats.iterator().next();
        
        // make sure this is the end of the tree
        cats = cat.getWeblogCategories();
        assertEquals(0, cats.size());
        
        log.info("END");
    }
    
    
    /**
     * Test the hasCategory() method on WeblogCategoryData.
     */
    public void testHasCategory() throws Exception {
        
        log.info("BEGIN");
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        WeblogCategoryData root = mgr.getRootWeblogCategory(TestUtils.getManagedWebsite(testWeblog));
        
        // check that root has category
        assertTrue(root.hasCategory(testCat.getName()));
        
        log.info("END");
    }
    
    
    /**
     * Lookup category by id.
     */
    public void testLookupCategoryById() throws Exception {
        
        log.info("BEGIN");
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        WeblogCategoryData cat = mgr.getWeblogCategory(testCat.getId());
        assertNotNull(cat);
        assertEquals(cat, testCat);
        
        log.info("END");
    }
    
    
    /**
     * Lookup category by path.
     */
    public void testLookupCategoryByPath() throws Exception {
        
        log.info("BEGIN");
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        testWeblog = TestUtils.getManagedWebsite(testWeblog);
        WeblogCategoryData cat = mgr.getWeblogCategoryByPath(testWeblog, "/catTest-cat1");
        assertNotNull(cat);
        assertEquals(cat, cat1);
        
        cat = mgr.getWeblogCategoryByPath(testWeblog, "/catTest-cat1/catTest-cat2/catTest-cat3");
        assertNotNull(cat);
        assertEquals(cat, cat3);
        
        // test lazy lookup, specifying just a name without slashes
        cat = mgr.getWeblogCategoryByPath(testWeblog, "catTest-cat1");
        assertNotNull(cat);
        assertEquals(cat, cat1);
        
        // if no path is specified we should get the root category
        cat = mgr.getWeblogCategoryByPath(testWeblog, null);
        assertNotNull(cat);
        assertEquals(cat.getPath(), "/");
        
        log.info("END");
    }
    
    
    /**
     * Lookup all categories for a weblog.
     */
    public void testLookupAllCategoriesByWeblog() throws Exception {
        
        log.info("BEGIN");
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        // including root
        testWeblog = TestUtils.getManagedWebsite(testWeblog);
        List cats = mgr.getWeblogCategories(testWeblog, true);
        assertNotNull(cats);
        assertEquals(5, cats.size());
        
        // not including root
        cats = mgr.getWeblogCategories(testWeblog, false);
        assertNotNull(cats);
        assertEquals(4, cats.size());
        
        log.info("END");
    }
    
    
    /**
     * Test moving one category into another.
     */
    public void testMoveWeblogCategory() throws Exception {
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        testWeblog = TestUtils.getManagedWebsite(testWeblog);
        testUser = TestUtils.getManagedUser(testUser);
        WeblogCategoryData root = mgr.getRootWeblogCategory(testWeblog);
        
        // add some categories and entries to test with
        WeblogCategoryData dest = new WeblogCategoryData(testWeblog, root, "c0", null, null);
        mgr.saveWeblogCategory(dest);
        
        WeblogCategoryData c1 = new WeblogCategoryData(testWeblog, root, "c1", null, null);
        mgr.saveWeblogCategory(c1);
        
        WeblogCategoryData c2 = new WeblogCategoryData(testWeblog, c1, "c2", null, null);
        mgr.saveWeblogCategory(c2);
        
        WeblogCategoryData c3 = new WeblogCategoryData(testWeblog, c2, "c3", null, null);
        mgr.saveWeblogCategory(c3);
        
        TestUtils.endSession(true);
        
        c1 = mgr.getWeblogCategory(c1.getId());
        c2 = mgr.getWeblogCategory(c2.getId());
        c3 = mgr.getWeblogCategory(c3.getId());
        dest = mgr.getWeblogCategory(dest.getId());
        
        WeblogEntryData e1 = TestUtils.setupWeblogEntry("e1", c1, testWeblog, testUser);
        WeblogEntryData e2 = TestUtils.setupWeblogEntry("e2", c2, testWeblog, testUser);
        WeblogEntryData e3 = TestUtils.setupWeblogEntry("e3", c3, testWeblog, testUser);
        
        TestUtils.endSession(true);
        
        // need to query for cats again because we closed the session
        c1 = mgr.getWeblogCategory(c1.getId());
        c2 = mgr.getWeblogCategory(c2.getId());
        c3 = mgr.getWeblogCategory(c3.getId());
        dest = mgr.getWeblogCategory(dest.getId());
        
        // verify number of entries in each category
        assertEquals(0, dest.retrieveWeblogEntries(true).size());
        assertEquals(0, dest.retrieveWeblogEntries(false).size());
        assertEquals(1, c1.retrieveWeblogEntries(false).size());
        assertEquals(3, c1.retrieveWeblogEntries(true).size());
        
        // move contents of source category c1 to destination catetory dest
        mgr.moveWeblogCategory(c1, dest);
        TestUtils.endSession(true);
        
        // after move, verify number of entries in each category
        dest = mgr.getWeblogCategory(dest.getId());
        c1 = mgr.getWeblogCategory(c1.getId());
        c2 = mgr.getWeblogCategory(c2.getId());
        c3 = mgr.getWeblogCategory(c3.getId());
        
        assertEquals(3, dest.retrieveWeblogEntries(true).size());
        assertEquals(0, dest.retrieveWeblogEntries(false).size());
        
        assertEquals(dest, c1.getParent());
        assertEquals(c1,   c2.getParent());
        assertEquals(c2,   c3.getParent());
        
        assertEquals(1, c1.retrieveWeblogEntries(false).size());
        assertEquals(1, c2.retrieveWeblogEntries(false).size());
        assertEquals(1, c3.retrieveWeblogEntries(false).size());
        
        List entries = c1.retrieveWeblogEntries(true);
        assertEquals(3, entries.size());
    }
    
    
    /**
     * Test moving entries in category to new category.
     */
    public void testMoveWeblogCategoryContents() throws Exception {
        
        WeblogManager mgr = RollerFactory.getRoller().getWeblogManager();
        
        testWeblog = TestUtils.getManagedWebsite(testWeblog);
        testUser = TestUtils.getManagedUser(testUser);
        WeblogCategoryData root = mgr.getRootWeblogCategory(testWeblog);
        
        // add some categories and entries to test with
        WeblogCategoryData dest = new WeblogCategoryData(testWeblog, root, "c0", null, null);
        mgr.saveWeblogCategory(dest);
        
        WeblogCategoryData c1 = new WeblogCategoryData(testWeblog, root, "c1", null, null);
        mgr.saveWeblogCategory(c1);
        
        WeblogCategoryData c2 = new WeblogCategoryData(testWeblog, c1, "c2", null, null);
        mgr.saveWeblogCategory(c2);
        
        WeblogCategoryData c3 = new WeblogCategoryData(testWeblog, c2, "c3", null, null);
        mgr.saveWeblogCategory(c3);
        
        TestUtils.endSession(true);
        
        c1 = mgr.getWeblogCategory(c1.getId());
        c2 = mgr.getWeblogCategory(c2.getId());
        c3 = mgr.getWeblogCategory(c3.getId());
        dest = mgr.getWeblogCategory(dest.getId());
        
        testWeblog = TestUtils.getManagedWebsite(testWeblog);
        testUser = TestUtils.getManagedUser(testUser);
        WeblogEntryData e1 = TestUtils.setupWeblogEntry("e1", c1, testWeblog, testUser);
        WeblogEntryData e2 = TestUtils.setupWeblogEntry("e2", c2, testWeblog, testUser);
        WeblogEntryData e3 = TestUtils.setupWeblogEntry("e3", c3, testWeblog, testUser);
        
        TestUtils.endSession(true);
        
        // need to query for cats again since session was closed
        c1 = mgr.getWeblogCategory(c1.getId());
        c2 = mgr.getWeblogCategory(c2.getId());
        c3 = mgr.getWeblogCategory(c3.getId());
        dest = mgr.getWeblogCategory(dest.getId());
        
        // verify number of entries in each category
        assertEquals(0, dest.retrieveWeblogEntries(true).size());
        assertEquals(0, dest.retrieveWeblogEntries(false).size());
        assertEquals(1, c1.retrieveWeblogEntries(false).size());
        assertEquals(3, c1.retrieveWeblogEntries(true).size());
        
        // move contents of source category c1 to destination category dest
        mgr.moveWeblogCategoryContents(c1, dest);
        mgr.saveWeblogCategory(c1);
        TestUtils.endSession(true);
        
        // after move, verify number of entries in each category
        dest = mgr.getWeblogCategory(dest.getId());
        c1 = mgr.getWeblogCategory(c1.getId());
        
        // Hierarchy is flattened under dest      
        assertEquals(3, dest.retrieveWeblogEntries(true).size());
        assertEquals(3, dest.retrieveWeblogEntries(false).size());
        
        // c1 category should be empty now
        assertEquals(0, c1.retrieveWeblogEntries(false).size());

    }
    
}
