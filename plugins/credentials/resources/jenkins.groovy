/*
*  Copyright 2016 Mirantis, Inc.
*
*  Licensed under the Apache License, Version 2.0 (the "License"); you may
*  not use this file except in compliance with the License. You may obtain
*  a copy of the License at
*
*       http://www.apache.org/licenses/LICENSE-2.0
*
*  Unless required by applicable law or agreed to in writing, software
*  distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
*  WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
*  License for the specific language governing permissions and limitations
*  under the License.
*/

import jenkins.model.Jenkins
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*;
import com.cloudbees.plugins.credentials.*
import com.cloudbees.plugins.credentials.*;
import com.cloudbees.plugins.credentials.common.*
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.domains.*;
import com.cloudbees.plugins.credentials.impl.*
import com.cloudbees.plugins.credentials.impl.*;
import hudson.plugins.sshslaves.*;
import jenkins.model.*;

class Actions {
  Actions(out) { this.out = out }
  def out


  private credentials_for_username(String username) {
    def username_matcher = CredentialsMatchers.withUsername(username)
    def available_credentials =
      CredentialsProvider.lookupCredentials(
        StandardUsernameCredentials.class,
        Jenkins.getInstance(),
        hudson.security.ACL.SYSTEM,
        new SchemeRequirement("ssh")
      )
    return CredentialsMatchers.firstOrNull(
      available_credentials,
      username_matcher
    )
  }

  void update_credentials(String scope, String username, String password="", String description="", String private_key="") {

    //removing '' quotes, jenkins cli bug workaround
    scope = scope.replaceAll('^\'|\'$', '')
    username = username.replaceAll('^\'|\'$', '')
    password = password.replaceAll('^\'|\'$', '')
    description = description.replaceAll('^\'|\'$', '')
    private_key = private_key.replaceAll('^\'|\'$', '')

    def global_domain = Domain.global()
    def credentials_store =
      Jenkins.instance.getExtensionList(
        'com.cloudbees.plugins.credentials.SystemCredentialsProvider'
      )[0].getStore()

    def credentials_scope
    if (scope == "global") {
      credentials_scope = CredentialsScope.GLOBAL
    } else if (scope == "system") {
      credentials_scope = CredentialsScope.SYSTEM
    }

    def credentials
    if (private_key == "" ) {
      credentials = new UsernamePasswordCredentialsImpl(
        credentials_scope,
        null,
        description,
        username,
        password
      )
    } else {
      def key_source
      if (private_key.startsWith('-----BEGIN')) {
        key_source = new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(private_key)
      } else if (private_key.startsWith('from-jenkins-ssh-dir')) {
        key_source = new BasicSSHUserPrivateKey.UsersPrivateKeySource()
      } else {
        key_source = new BasicSSHUserPrivateKey.FileOnMasterPrivateKeySource(private_key)
      }
      credentials = new BasicSSHUserPrivateKey(
        credentials_scope,
        null,
        username,
        key_source,
        password,
        description
      )
    }
    // Create or update the credentials in the Jenkins instance
    def existing_credentials = credentials_for_username(username)
    if(existing_credentials != null) {
      credentials_store.updateCredentials(
        global_domain,
        existing_credentials,
        credentials
      )
    } else {
      credentials_store.addCredentials(global_domain, credentials)
    }
  }
}

///////////////////////////////////////////////////////////////////////////////
// CLI Argument Processing
///////////////////////////////////////////////////////////////////////////////

actions = new Actions(out)
action = args[0]
if (args.length < 2) {
  actions."$action"()
} else {
  actions."$action"(*args[1..-1])
}
