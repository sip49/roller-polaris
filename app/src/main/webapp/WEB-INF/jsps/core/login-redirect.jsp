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
<%@ page language="java" contentType="text/html; charset=UTF-8" %>
<%@ page import="org.apache.roller.weblogger.pojos.*" %>
<%@ page import="org.apache.roller.weblogger.business.*" %>
<%@ page import="org.springframework.security.core.Authentication" %>
<%@ page import="org.springframework.security.core.context.SecurityContextHolder" %>
<%@ page import="java.util.List" %>
<%
Authentication auth = SecurityContextHolder.getContext().getAuthentication();
UserManager mgr = WebloggerFactory.getWeblogger().getUserManager();
User user = mgr.getUserByUserName(auth.getName());
List<UserWeblogRole> roles = (user != null) ? mgr.getWeblogRoles(user) : null;

if (user == null) {
    // this will force a login, and then a Spring security redirect to the menu page.  Forwarding
    // to login directly does not work well as no forwarding occurs (remains on login page).
    response.sendRedirect(request.getContextPath()+"/tb-ui/menu.rol");
} else if (!user.isGlobalAdmin() && roles != null && roles.size() == 1) {
    Weblog weblog = roles.get(0).getWeblog();
    response.sendRedirect(request.getContextPath()+"/tb-ui/authoring/entryAdd.rol?weblog=" + weblog.getHandle());
} else {
    response.sendRedirect(request.getContextPath()+"/tb-ui/menu.rol");
}
%>