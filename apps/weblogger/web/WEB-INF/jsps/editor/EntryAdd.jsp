<%--
  Licensed to the Apache Software Foundation (ASF) under one or more
   contributor license agreements.  The ASF licenses this file to You
  under the Apache License, Version 2.0 (the "License"); you may not
  use this file except in compliance with the License.
  You may obtain a copy of the License at

      http://www.apache.org/licenses/LICENSE-2.0

  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.  For additional information regarding
  copyright in this work, please see the NOTICE file in the top level
  directory of this distribution.
--%>
<%@ include file="/WEB-INF/jsps/taglibs-struts2.jsp" %>

<p class="subtitle">
    <s:text name="weblogEdit.subtitle.newEntry" >
        <s:param value="actionWeblog.handle" />
    </s:text>
</p>

<s:form id="entry" action="entryAdd!save">
    <s:hidden name="weblog" />
    
    <%-- ================================================================== --%>
    <%-- Title, category, dates and other metadata --%>
    
    <table class="entryEditTable" cellpadding="0" cellspacing="0" width="100%">   
        
        <tr>
            <td class="entryEditFormLabel">
                <label for="title"><s:text name="weblogEdit.title" /></label>
            </td>
            <td>
                <s:textfield name="bean.title" size="70" maxlength="255" tabindex="1" />
            </td>
        </tr>
        
        <tr>
            <td class="entryEditFormLabel">
                <label for="status"><s:text name="weblogEdit.status" /></label>
            </td>
            <td>
                <span style="color:red; font-weight:bold"><s:text name="weblogEdit.unsaved" /></span>
                <s:hidden name="bean.status" value="" />
            </td>
        </tr>
        
        <tr>
            <td class="entryEditFormLabel">
                <label for="categoryId"><s:text name="weblogEdit.category" /></label>
            </td>
            <td>
                <s:select name="bean.categoryId" list="actionWeblog.weblogCategories" listKey="id" listValue="path" size="1" />
            </td>
        </tr>
        
        <tr>
            <td class="entryEditFormLabel">
                <label for="title"><s:text name="weblogEdit.tags" /></label>
            </td>
            <td>
                <s:textfield id="entryEditTags" cssClass="entryEditTags" name="bean.tagsAsString" size="70" maxlength="255" tabindex="3" />
                <div id="entryEditTagsChoices" style="display:none" class="autocomplete"></div>
                <br/>
                <script type="text/javascript">
                <!--
		new RollerTagsAutoCompleter("entryEditTags", "entryEditTagsChoices", "<s:property value="jsonAutocompleteUrl" />", { tokens : [' ',','], minChars: 2 });
                // --></script>
            </td>
        </tr> 
        
        <s:if test="actionWeblog.enableMultiLang">
            <tr>
                <td class="entryEditFormLabel">
                    <label for="locale"><s:text name="weblogEdit.locale" /></label>
                </td>
                <td>
                    <s:select name="bean.locale" size="1" list="localesList" listValue="displayName" />
                </td>
            </tr>
        </s:if>
        <s:else>
            <s:hidden name="bean.locale"/>
        </s:else>
        
    </table>
    
    
    <%-- ================================================================== --%>
    <%-- Weblog edit or preview --%>
    
    <div style="width: 100%;"> <%-- need this div to control text-area size in IE 6 --%>
        <%-- include edit page --%>
        <div>
            <s:include value="%{editor.jspPage}" />
        </div>
    </div>
    
    <br />
    
    
    <%-- ================================================================== --%>
    <%-- plugin chooser --%>
    
    <s:if test="!entryPlugins.isEmpty">
        <div id="pluginControlToggle" class="controlToggle">
            <span id="ipluginControl">+</span>
            <a class="controlToggle" onclick="javascript:toggleControl('pluginControlToggle','pluginControl')">
            <s:text name="weblogEdit.pluginsToApply" /></a>
        </div>
        <div id="pluginControl" class="miscControl" style="display:none">
            <s:checkboxlist theme="roller" name="bean.plugins" list="entryPlugins" listKey="name" listValue="name" />
        </div>
    </s:if>

    
    <%-- ================================================================== --%>
    <%-- advanced settings  --%>
    
    <div id="miscControlToggle" class="controlToggle">
        <span id="imiscControl">+</span>
        <a class="controlToggle" onclick="javascript:toggleControl('miscControlToggle','miscControl')">
        <s:text name="weblogEdit.miscSettings" /></a>
    </div>
    <div id="miscControl" class="miscControl" style="display:none">
        
        <label for="link"><s:text name="weblogEdit.pubTime" /></label>
        <div>
            <s:select name="bean.hours" list="hoursList" />
            :
            <s:select name="bean.minutes" list="minutesList" />
            :
            <s:select name="bean.seconds" list="secondsList" />
            &nbsp;&nbsp;
            <script type="text/javascript" >
            <!--
            if (document.layers) { // Netscape 4 hack
                var cal = new CalendarPopup();
            } else {
                var cal = new CalendarPopup("datetagdiv");
                document.write(cal.getStyles());
            }
            // -->
            </script>
            <s:textfield name="bean.dateString" size="12" />
            <a href="#" id="anchorCal" name="anchorCal"
               onclick="cal.select(document.getElementById('entry_bean_dateString'),'anchorCal','MM/dd/yy'); return false">
            <img src='<s:url value="/images/calendar.png"/>' class="calIcon" alt="Calendar" /></a>
            <s:property value="actionWeblog.timeZone" />
        </div>   
        <br />
        
        <s:checkbox name="bean.allowComments" onchange="onAllowCommentsChange()" />
        <s:text name="weblogEdit.allowComments" />
        <s:text name="weblogEdit.commentDays" />
        <s:select name="bean.commentDays" list="commentDaysList" size="1" listKey="key" listValue="value" />
        <br />
        
        <s:checkbox name="bean.rightToLeft" />
        <s:text name="weblogEdit.rightToLeft" />
        <br />
        
        <s:if test="authenticatedUser.hasRole('admin')">
            <s:checkbox name="bean.pinnedToMain" />
            <s:text name="weblogEdit.pinnedToMain" />
            <br />
        </s:if>
        <br />
        
        <s:text name="weblogEdit.enclosureURL" />: <s:textfield name="bean.enclosureURL" size="40" maxlength="255" />
    </div>
    
    
    <%-- ================================================================== --%>
    <%-- the button box --%>
    
    <br>
    <div class="control">
        <s:if test="userAnAuthor">
            <s:submit key="weblogEdit.post" onclick="document.getElementById('entry_bean_status').value='PUBLISHED';"/>
        </s:if>
        <s:else>
            <s:submit key="weblogEdit.submitForReview" onclick="document.getElementById('entry_bean_status').value='PENDING';"/>
        </s:else>
        <s:submit key="weblogEdit.save" onclick="document.getElementById('entry_bean_status').value='DRAFT';" />
    </div>
    
</s:form>
