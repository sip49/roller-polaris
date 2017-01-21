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
<%@ include file="/WEB-INF/jsps/tightblog-taglibs.jsp" %>

<%-- Success Messages --%>
<s:if test="!actionMessages.isEmpty">
    <div id="messages" class="messages">
        <s:actionmessage />
    </div>
</s:if>

<%-- Error Messages --%>
<s:if test="!actionErrors.isEmpty || !fieldErrors.isEmpty">
    <div id="errors" class="errors">
        <ul>
            <s:iterator var="actionError" value="actionErrors">
                <li><s:property value="#actionError"/></li>
            </s:iterator>
            <s:iterator var="fieldErrorName" value="fieldErrors.keySet()">
                <s:iterator var="fieldErrorValue" value="fieldErrors[#fieldErrorName]">
                    <li><s:property value="#fieldErrorValue"/></li>
                </s:iterator>
            </s:iterator>
        </ul>
    </div>
</s:if>
