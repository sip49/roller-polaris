/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  The ASF licenses this file to You
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
package org.tightblog.ui.restapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.tightblog.service.EmailService;
import org.tightblog.service.URLService;
import org.tightblog.service.UserManager;
import org.tightblog.domain.GlobalRole;
import org.tightblog.domain.User;
import org.tightblog.domain.UserCredentials;
import org.tightblog.domain.UserStatus;
import org.tightblog.domain.UserWeblogRole;
import org.tightblog.domain.Weblog;
import org.tightblog.domain.WeblogRole;
import org.tightblog.repository.UserCredentialsRepository;
import org.tightblog.repository.UserRepository;
import org.tightblog.repository.UserWeblogRoleRepository;
import org.tightblog.repository.WeblogRepository;
import org.tightblog.repository.WebloggerPropertiesRepository;
import org.tightblog.util.Utilities;
import org.tightblog.util.ValidationError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.tightblog.domain.WebloggerProperties;

import javax.mail.MessagingException;
import javax.persistence.RollbackException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import java.security.Principal;
import java.time.Instant;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
public class UserController {

    private static Logger log = LoggerFactory.getLogger(UserController.class);

    private static final Pattern PWD_PATTERN =
            Pattern.compile("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=])(?=\\S+$).{8,20}$");

    private WeblogRepository weblogRepository;
    private UserManager userManager;
    private UserWeblogRoleRepository userWeblogRoleRepository;
    private UserRepository userRepository;
    private UserCredentialsRepository userCredentialsRepository;
    private EmailService emailService;
    private MessageSource messages;
    private WebloggerPropertiesRepository webloggerPropertiesRepository;
    private URLService urlService;

    @Autowired
    public UserController(WeblogRepository weblogRepository, UserManager userManager,
                          UserWeblogRoleRepository userWeblogRoleRepository, MessageSource messageSource,
                          EmailService emailService, UserRepository userRepository,
                          UserCredentialsRepository userCredentialsRepository, URLService urlService,
                          WebloggerPropertiesRepository webloggerPropertiesRepository) {
        this.weblogRepository = weblogRepository;
        this.webloggerPropertiesRepository = webloggerPropertiesRepository;
        this.userManager = userManager;
        this.userWeblogRoleRepository = userWeblogRoleRepository;
        this.userRepository = userRepository;
        this.userCredentialsRepository = userCredentialsRepository;
        this.urlService = urlService;
        this.emailService = emailService;
        this.messages = messageSource;
    }

    @GetMapping(value = "/tb-ui/admin/rest/useradmin/userlist")
    public Map<String, String> getUserEditList() throws ServletException {
        return createUserMap(userRepository.findAll());
    }

    @GetMapping(value = "/tb-ui/admin/rest/useradmin/registrationapproval")
    public List<User> getRegistrationsNeedingApproval() throws ServletException {
        return userRepository.findUsersToApprove();
    }

    @PostMapping(value = "/tb-ui/admin/rest/useradmin/registrationapproval/{id}/approve")
    public void approveRegistration(@PathVariable String id, HttpServletResponse response) {
        User acceptedUser = userRepository.findByIdOrNull(id);
        if (acceptedUser != null) {
            if (!UserStatus.ENABLED.equals(acceptedUser.getStatus())) {
                acceptedUser.setStatus(UserStatus.ENABLED);
                userRepository.saveAndFlush(acceptedUser);
                emailService.sendRegistrationApprovedNotice(acceptedUser);
            }
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @PostMapping(value = "/tb-ui/admin/rest/useradmin/registrationapproval/{id}/reject")
    public void rejectRegistration(@PathVariable String id, HttpServletResponse response) {
        User rejectedUser = userRepository.findByIdOrNull(id);
        if (rejectedUser != null) {
            emailService.sendRegistrationRejectedNotice(rejectedUser);
            userManager.removeUser(rejectedUser);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping(value = "/tb-ui/authoring/rest/weblog/{weblogId}/potentialmembers")
    public Map<String, String> getPotentialNewBlogMembers(@PathVariable String weblogId, Principal p,
                                                          HttpServletResponse response)
            throws ServletException {

        Weblog weblog = weblogRepository.findById(weblogId).orElse(null);
        if (weblog != null && userManager.checkWeblogRole(p.getName(), weblog, WeblogRole.OWNER)) {
            // member list excludes inactive accounts
            List<User> potentialUsers = userRepository.findByStatusEnabled();

            // filter out people already members
            ListIterator<User> potentialIter = potentialUsers.listIterator();
            List<UserWeblogRole> currentUserList = userWeblogRoleRepository.findByWeblog(weblog);
            while (potentialIter.hasNext() && !currentUserList.isEmpty()) {
                User su = potentialIter.next();
                ListIterator<UserWeblogRole> alreadyIter = currentUserList.listIterator();
                while (alreadyIter.hasNext()) {
                    UserWeblogRole au = alreadyIter.next();
                    if (su.getId().equals(au.getUser().getId())) {
                        potentialIter.remove();
                        alreadyIter.remove();
                        break;
                    }
                }
            }
            return createUserMap(potentialUsers);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
    }

    private Map<String, String> createUserMap(List<User> users) {
        Map<String, String> userMap = new TreeMap<>();
        for (User user : users) {
            userMap.put(user.getId(), user.getScreenName() + " (" + user.getEmailAddress() + ")");
        }
        Map<String, String> sortedMap = userMap.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e2, LinkedHashMap::new));
        return sortedMap;
    }

    @GetMapping(value = "/tb-ui/admin/rest/useradmin/user/{id}")
    public UserData getUserData(@PathVariable String id, HttpServletResponse response) throws ServletException {
        User user = userRepository.findByIdOrNull(id);

        if (user != null) {
            UserData data = new UserData();
            UserCredentials creds = userCredentialsRepository.findByUserName(user.getUserName());
            data.setUser(user);
            data.setCredentials(creds);
            return data;
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @GetMapping(value = "/tb-ui/authoring/rest/userprofile/{id}")
    public User getProfileData(@PathVariable String id, Principal p, HttpServletResponse response) throws ServletException {
        User user = userRepository.findByIdOrNull(id);
        User authenticatedUser = userRepository.findEnabledByUserName(p.getName());

        if (user != null && user.getId().equals(authenticatedUser.getId())) {
            return user;
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
    }

    @PostMapping(value = "/tb-ui/register/rest/registeruser")
    public ResponseEntity registerUser(@Valid @RequestBody UserData newData, Locale locale, HttpServletResponse response)
            throws ServletException {
        ValidationError maybeError = advancedValidate(null, newData, true, locale);
        if (maybeError != null) {
            return ResponseEntity.badRequest().body(maybeError);
        }

        long userCount = userRepository.count();
        WebloggerProperties.RegistrationPolicy option = webloggerPropertiesRepository.findOrNull().getRegistrationPolicy();
        if (userCount == 0 || !WebloggerProperties.RegistrationPolicy.DISABLED.equals(option)) {
            boolean mustActivate = userCount > 0;
            if (mustActivate) {
                newData.user.setActivationCode(UUID.randomUUID().toString());
                newData.user.setStatus(UserStatus.REGISTERED);
            } else {
                // initial user is the Admin, is automatically enabled.
                newData.user.setStatus(UserStatus.ENABLED);
            }

            User user = new User();
            user.setUserName(newData.user.getUserName());
            user.setDateCreated(Instant.now());

            ResponseEntity re = saveUser(user, newData, null, response, true);

            if (re.getStatusCode() == HttpStatus.OK && mustActivate) {
                try {
                    UserData data = (UserData) re.getBody();
                    emailService.sendUserActivationEmail(data.getUser());
                } catch (MessagingException ignored) {
                }
            }
            return re;
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    @PostMapping(value = "/tb-ui/authoring/rest/userprofile/{id}")
    public ResponseEntity updateUserProfile(@PathVariable String id, @Valid @RequestBody UserData newData, Principal p,
                                            Locale locale, HttpServletResponse response) throws ServletException {
        User user = userRepository.findByIdOrNull(id);
        User authenticatedUser = userRepository.findEnabledByUserName(p.getName());

        if (user != null && user.getId().equals(authenticatedUser.getId())) {
            ValidationError maybeError = advancedValidate(null, newData, false, locale);
            if (maybeError != null) {
                return ResponseEntity.badRequest().body(maybeError);
            }
            return saveUser(user, newData, p, response, false);
        } else {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }
    }

    @PutMapping(value = "/tb-ui/admin/rest/useradmin/user/{id}")
    public ResponseEntity updateUser(@PathVariable String id, @Valid @RequestBody UserData newData, Principal p,
                                     Locale locale, HttpServletResponse response) throws ServletException {
        User user = userRepository.findByIdOrNull(id);
        ValidationError maybeError = advancedValidate(user, newData, false, locale);
        if (maybeError != null) {
            return ResponseEntity.badRequest().body(maybeError);
        }
        return saveUser(user, newData, p, response, false);
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class UserData {
        @Valid
        User user;

        UserCredentials credentials;

        UserData() {
        }

        public User getUser() {
            return user;
        }

        public void setUser(User user) {
            this.user = user;
        }

        public UserCredentials getCredentials() {
            return credentials;
        }

        public void setCredentials(UserCredentials credentials) {
            this.credentials = credentials;
        }
    }

    @GetMapping(value = "/tb-ui/authoring/rest/weblog/{weblogId}/members")
    public List<UserWeblogRole> getWeblogMembers(@PathVariable String weblogId, Principal p, HttpServletResponse response)
            throws ServletException {

        Weblog weblog = weblogRepository.findById(weblogId).orElse(null);
        if (weblog != null && userManager.checkWeblogRole(p.getName(), weblog, WeblogRole.OWNER)) {
            List<UserWeblogRole> uwrs = userWeblogRoleRepository.findByWeblog(weblog);
            return uwrs;
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return null;
        }
    }

    @PostMapping(value = "/tb-ui/authoring/rest/weblog/{weblogId}/memberupdate", produces = "text/plain")
    public ResponseEntity updateWeblogMembership(@PathVariable String weblogId, Principal p, Locale locale,
                                                 @RequestBody List<UserWeblogRole> roles)
            throws ServletException {

        Weblog weblog = weblogRepository.findById(weblogId).orElse(null);
        User user = userRepository.findEnabledByUserName(p.getName());
        if (user != null && weblog != null && userManager.checkWeblogRole(p.getName(), weblog, WeblogRole.OWNER)) {

            // must remain at least one admin
            List<UserWeblogRole> owners = roles.stream()
                    .filter(r -> r.getWeblogRole().equals(WeblogRole.OWNER))
                    .filter(r -> !r.isPending())
                    .collect(Collectors.toList());
            if (owners.size() < 1) {
                return ResponseEntity.badRequest().body(messages.getMessage("members.oneAdminRequired", null, locale));
            }

            // one iteration for each line (user) in the members table
            for (UserWeblogRole role : roles) {
                if (WeblogRole.NOBLOGNEEDED.equals(role.getWeblogRole())) {
                    userWeblogRoleRepository.delete(role);
                } else {
                    userManager.grantWeblogRole(
                            role.getUser(), role.getWeblog(), role.getWeblogRole(), role.isPending());
                }
            }
            String msg = messages.getMessage("members.membersChanged", null, locale);
            return ResponseEntity.ok(msg);
        } else {
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
    }

    private ResponseEntity saveUser(User user, UserData newData, Principal p, HttpServletResponse response, boolean add)
            throws ServletException {
        try {
            if (user != null) {
                user.setScreenName(newData.user.getScreenName().trim());
                user.setEmailAddress(newData.user.getEmailAddress().trim());

                if (!UserStatus.ENABLED.equals(user.getStatus()) && StringUtils.isNotEmpty(newData.user.getActivationCode())) {
                    user.setActivationCode(newData.user.getActivationCode());
                }

                if (add) {
                    user.setStatus(newData.user.getStatus());
                    if (userRepository.count() == 0) {
                        // first person in is always an admin
                        user.setGlobalRole(GlobalRole.ADMIN);
                    } else {
                        user.setGlobalRole(webloggerPropertiesRepository.findOrNull().isUsersCreateBlogs() ?
                                GlobalRole.BLOGCREATOR : GlobalRole.BLOGGER);
                    }
                } else {
                    // users can't alter own roles or status
                    if (!user.getUserName().equals(p.getName())) {
                        user.setGlobalRole(newData.user.getGlobalRole());
                        user.setStatus(newData.user.getStatus());
                    }
                }

                try {
                    userRepository.saveAndFlush(user);
                    // reset password if set
                    if (newData.credentials != null) {
                        if (!StringUtils.isEmpty(newData.credentials.getPasswordText())) {
                            userManager.updateCredentials(user.getId(), newData.credentials.getPasswordText());
                        }
                        // reset MFA secret if requested
                        if (newData.credentials.isEraseMfaSecret()) {
                            userCredentialsRepository.eraseMfaCode(user.getId());
                        }
                    }
                    response.setStatus(HttpServletResponse.SC_OK);
                } catch (RollbackException e) {
                    return ResponseEntity.status(HttpServletResponse.SC_CONFLICT).body("Persistence Problem");
                }
            } else {
                return new ResponseEntity<>(HttpStatus.NOT_FOUND);
            }
            UserData data = new UserData();
            data.setUser(user);
            UserCredentials creds = userCredentialsRepository.findByUserName(user.getUserName());
            data.setCredentials(creds);
            return ResponseEntity.ok(data);
        } catch (Exception e) {
            log.error("Error updating user", e);
            throw new ServletException(e);
        }
    }

    private ValidationError advancedValidate(User currentUser, UserData data, boolean isAdd, Locale locale) {
        BindException be = new BindException(data, "new data object");

        User testHasUserName = userRepository.findByUserName(data.user.getUserName());
        if (testHasUserName != null && !testHasUserName.getId().equals(data.user.getId())) {
            be.addError(new ObjectError("User object", messages.getMessage("error.add.user.userNameInUse",
                    null, locale)));
        }

        User testHasScreenName = userRepository.findByScreenName(data.user.getScreenName());
        if (testHasScreenName != null && !testHasScreenName.getId().equals(data.user.getId())) {
            be.addError(new ObjectError("User object", messages.getMessage("error.add.user.screenNameInUse",
                    null, locale)));
        }

        if (currentUser != null) {
            UserStatus currentStatus = currentUser.getStatus();
            if (currentStatus != data.getUser().getStatus()) {
                switch (currentStatus) {
                    case ENABLED:
                        if (data.getUser().getStatus() != UserStatus.DISABLED) {
                            be.addError(new ObjectError("User object",
                                    messages.getMessage("error.useradmin.enabled.only.disabled", null, locale)));
                        }
                        break;
                    case DISABLED:
                        if (data.getUser().getStatus() != UserStatus.ENABLED) {
                            be.addError(new ObjectError("User object",
                                    messages.getMessage("error.useradmin.disabled.only.enabled", null, locale)));
                        }
                        break;
                    case REGISTERED:
                    case EMAILVERIFIED:
                        if (data.getUser().getStatus() != UserStatus.ENABLED) {
                            be.addError(new ObjectError("User object",
                                    messages.getMessage("error.useradmin.nonenabled.only.enabled", null, locale)));
                        }
                        break;
                    default:
                }
            }
        }

        if (data.credentials != null) {
            String maybePassword = data.credentials.getPasswordText();
            if (!StringUtils.isEmpty(maybePassword)) {
                if (!maybePassword.equals(data.credentials.getPasswordConfirm())) {
                    be.addError(new ObjectError("User object",
                            messages.getMessage("error.add.user.passwordConfirmFail", null, locale)));
                } else {
                    if (!PWD_PATTERN.matcher(maybePassword).matches()) {
                        be.addError(new ObjectError("User object",
                                messages.getMessage("error.add.user.passwordComplexityFail", null, locale)));
                    }
                }
            } else {
                if (!StringUtils.isEmpty(data.credentials.getPasswordConfirm())) {
                    // confirm provided but password field itself not filled out
                    be.addError(new ObjectError("User object",
                            messages.getMessage("error.add.user.passwordConfirmFail", null, locale)));
                }
            }

            if (isAdd && StringUtils.isEmpty(data.credentials.getPasswordText())) {
                be.addError(new ObjectError("User object",
                        messages.getMessage("error.add.user.missingPassword", null, locale)));
            }
        }

        return be.getErrorCount() > 0 ? ValidationError.fromBindingErrors(be) : null;
    }

    @GetMapping(value = "/tb-ui/admin/rest/useradmin/user/{id}/weblogs")
    public List<UserWeblogRole> getUsersWeblogs(@PathVariable String id, HttpServletResponse response) throws ServletException {
        User user = userRepository.findByIdOrNull(id);
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        List<UserWeblogRole> uwrs = userWeblogRoleRepository.findByUser(user);
        for (UserWeblogRole uwr : uwrs) {
            uwr.getWeblog().setAbsoluteURL(urlService.getWeblogURL(uwr.getWeblog()));
            uwr.setUser(null);
        }
        return uwrs;
    }

    @GetMapping(value = "/tb-ui/authoring/rest/loggedinuser/weblogs")
    public List<UserWeblogRole> getLoggedInUsersWeblogs(Principal p, HttpServletResponse response)
            throws ServletException {
        User user = userRepository.findEnabledByUserName(p.getName());
        if (user == null) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return null;
        }
        return getUsersWeblogs(user.getId(), response);
    }

    @PostMapping(value = "/tb-ui/authoring/rest/weblog/{weblogId}/user/{userId}/role/{role}/invite", produces = "text/plain")
    public ResponseEntity inviteUser(@PathVariable String weblogId, @PathVariable String userId,
                                     @PathVariable WeblogRole role, Principal p, Locale locale) {

        Weblog weblog = weblogRepository.findById(weblogId).orElse(null);
        User invitee = userRepository.findByIdOrNull(userId);
        User invitor = userRepository.findEnabledByUserName(p.getName());

        if (weblog != null && invitee != null && invitor != null &&
                userManager.checkWeblogRole(p.getName(), weblog, WeblogRole.OWNER)) {

            UserWeblogRole roleChk = userWeblogRoleRepository.findByUserAndWeblog(invitee, weblog);
            if (roleChk != null) {
                return ResponseEntity.badRequest().body(messages.getMessage(roleChk.isPending() ?
                        "members.userAlreadyInvited" : "members.userAlreadyMember", null, locale));
            }
            userManager.grantWeblogRole(invitee, weblog, role, true);
            emailService.sendWeblogInvitation(invitee, weblog);
            return ResponseEntity.ok(messages.getMessage("members.userInvited", null, locale));
        } else {
            return ResponseEntity.status(HttpServletResponse.SC_FORBIDDEN).build();
        }

    }

    @PostMapping(value = "/tb-ui/authoring/rest/weblogrole/{id}/attach")
    public void acceptWeblogInvitation(@PathVariable String id, Principal p, HttpServletResponse response) {
        UserWeblogRole uwr = userWeblogRoleRepository.findByIdOrNull(id);
        if (uwr != null && uwr.getUser().getUserName().equals(p.getName())) {
            uwr.setPending(false);
            userWeblogRoleRepository.saveAndFlush(uwr);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @PostMapping(value = "/tb-ui/authoring/rest/weblogrole/{id}/detach")
    public void resignFromWeblog(@PathVariable String id, Principal p, HttpServletResponse response) {
        UserWeblogRole uwr = userWeblogRoleRepository.findByIdOrNull(id);
        if (uwr != null && uwr.getUser().getUserName().equals(p.getName())) {
            userWeblogRoleRepository.delete(uwr);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    @GetMapping(value = "/tb-ui/register/rest/useradminmetadata")
    public UserAdminMetadata getUserAdminMetadata() {
        UserAdminMetadata metadata = new UserAdminMetadata();

        metadata.userStatuses = Arrays.stream(UserStatus.values())
                .collect(Utilities.toLinkedHashMap(UserStatus::name, UserStatus::name));

        metadata.globalRoles = Arrays.stream(GlobalRole.values())
                .filter(r -> r != GlobalRole.NOAUTHNEEDED)
                .collect(Utilities.toLinkedHashMap(GlobalRole::name, GlobalRole::name));

        return metadata;
    }

    public class UserAdminMetadata {
        Map<String, String> locales;
        Map<String, String> userStatuses;
        Map<String, String> globalRoles;

        public Map<String, String> getLocales() {
            return locales;
        }

        public Map<String, String> getUserStatuses() {
            return userStatuses;
        }

        public Map<String, String> getGlobalRoles() {
            return globalRoles;
        }
    }

}