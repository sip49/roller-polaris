<%@ include file="/taglibs.jsp" %>
<%@ page import="org.roller.presentation.weblog.actions.WeblogEntryPageModel" %>
<%@ page import="org.roller.presentation.RollerRequest" %>
<%
WeblogEntryPageModel model = (WeblogEntryPageModel)request.getAttribute("model");
%>

<div class="sidebarfade">
    <div class="menu-tr">
        <div class="menu-tl">
            <div class="sidebarBody"> 
        
<h3><fmt:message key="weblogEdit.pendingEntries" /></h3>

<c:if test="${empty model.recentPendingEntries}">
   <fmt:message key="application.none" />
</c:if>
<c:forEach var="post" items="${model.recentPendingEntries}">
    <roller:link page="/editor/weblog.do">
       <roller:linkparam
           id="<%= RollerRequest.WEBLOGENTRYID_KEY %>"
           name="post" property="id" />
           <roller:linkparam id="method" value="edit" />
           <img src='<c:url value="/images/Edit16.png"/>' 
                align="absmiddle" border="0" alt="icon" title="Edit" />
           <str:truncateNicely lower="50">
              <c:out value="${post.title}" />
           </str:truncateNicely>
    </roller:link>
    <br />
</c:forEach>
    
         
<hr />            
<h3><fmt:message key="weblogEdit.draftEntries" /></h3>

<c:if test="${empty model.recentDraftEntries}">
   <fmt:message key="application.none" />
</c:if>
<c:forEach var="post" items="${model.recentDraftEntries}">
    <roller:link page="/editor/weblog.do">
       <roller:linkparam
           id="<%= RollerRequest.WEBLOGENTRYID_KEY %>"
           name="post" property="id" />
           <roller:linkparam id="method" value="edit" />
           <img src='<c:url value="/images/Edit16.png"/>' 
                align="absmiddle" border="0" alt="icon" title="Edit" />
           <str:truncateNicely lower="50">
              <c:out value="${post.title}" />
           </str:truncateNicely>
    </roller:link>
    <br />
</c:forEach>             


<c:if test="${model.userAuthorizedToAuthor}">
            
<hr />
<h3><fmt:message key="weblogEdit.publishedEntries" /></h3>

<c:if test="${empty model.recentPublishedEntries}">
   <fmt:message key="application.none" />
</c:if>
<c:forEach var="post" items="${model.recentPublishedEntries}">
    <roller:link page="/editor/weblog.do">
       <roller:linkparam
           id="<%= RollerRequest.WEBLOGENTRYID_KEY %>"
           name="post" property="id" />
           <roller:linkparam id="method" value="edit" />
           <img src='<c:url value="/images/Edit16.png"/>' 
                align="absmiddle" border="0" alt="icon" title="Edit" />
           <str:truncateNicely lower="50">
              <c:out value="${post.title}" />
           </str:truncateNicely>
    </roller:link>                    
    <br />
</c:forEach>
                
            </div>
        </div>
    </div>
</div>
 
</c:if>



