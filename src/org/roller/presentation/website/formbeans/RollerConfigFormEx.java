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

package org.roller.presentation.website.formbeans;

import javax.servlet.http.HttpServletRequest;

import org.apache.struts.action.ActionMapping;
import org.roller.RollerException;
import org.roller.pojos.RollerConfigData;
import org.roller.presentation.forms.RollerConfigForm;

/**
 * These properties are not persistent and are only needed for the UI.
 *
 * @struts.form name="rollerConfigFormEx"
 * @author Lance Lavandowska
 */
public class RollerConfigFormEx extends RollerConfigForm
{
    public RollerConfigFormEx()
    {
    }

    public RollerConfigFormEx( RollerConfigData config, java.util.Locale locale ) throws RollerException
    {
        super(config, locale);
    }

    /**
     * Override for non-primitive values
     */
    public void copyFrom(org.roller.pojos.RollerConfigData dataHolder, java.util.Locale locale) throws RollerException
    {
    	super.copyFrom(dataHolder, locale);
        fixNulls();
        this.uploadMaxFileMB = dataHolder.getUploadMaxFileMB();
        this.uploadMaxDirMB = dataHolder.getUploadMaxDirMB();
    }

    /**
     * Override for non-primitive values
     */
    public void copyTo(org.roller.pojos.RollerConfigData dataHolder, java.util.Locale locale) throws RollerException
    {
        fixNulls();
        super.copyTo(dataHolder, locale);
        dataHolder.setUploadMaxFileMB(this.uploadMaxFileMB);
        dataHolder.setUploadMaxDirMB(this.uploadMaxDirMB);
    }

    /**
     * Method allows Struts to handle empty checkboxes for booleans
     */
	public void reset(ActionMapping mapping, HttpServletRequest request) 
	{
        setAbsoluteURL( null );
        fixNulls();
	}
    
    private void fixNulls()
    {
        if (getRssUseCache() == null) setRssUseCache( Boolean.FALSE );
        if (getNewUserAllowed() == null) setNewUserAllowed( Boolean.FALSE );
        if (getEnableAggregator() == null) setEnableAggregator( Boolean.FALSE );
        if (getUploadEnabled() == null) setUploadEnabled( Boolean.FALSE );
        if (getMemDebug() == null) setMemDebug( Boolean.FALSE );
        if (getAutoformatComments() == null) setAutoformatComments( Boolean.FALSE );
        if (getEscapeCommentHtml() == null) setEscapeCommentHtml( Boolean.FALSE );
        if (getEmailComments() == null) setEmailComments( Boolean.FALSE );
        if (getEnableLinkback() == null) setEnableLinkback( Boolean.FALSE );        
        if (getEncryptPasswords() == null) setEncryptPasswords( Boolean.FALSE );        
    }
}