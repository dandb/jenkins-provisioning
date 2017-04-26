"""
  Jenkins Credential Helper
  
  Scope:
  Create or Update credentials within the global jenkins credential store
      AWSCredentialsImpl
      StringCredentials
      BasicSSHUserPrivateKey
      UsernamePasswordCredentialsImpl

  Notes:
  This does not allow for changing credential ID, as this would impact consumers
  of credentials downstream - bad developer bad!.  

"""
import jenkins.*
import jenkins.model.*
import hudson.*
import hudson.model.*
import hudson.util.Secret
import com.cloudbees.plugins.credentials.domains.*
import com.cloudbees.plugins.credentials.SystemCredentialsProvider.StoreImpl
import com.cloudbees.jenkins.plugins.sshcredentials.impl.*
import com.cloudbees.jenkins.plugins.sshcredentials.impl.BasicSSHUserPrivateKey
import com.cloudbees.plugins.credentials.impl.*
import org.jenkinsci.plugins.plaincredentials.*
import org.jenkinsci.plugins.plaincredentials.impl.*
import groovy.json.*
import groovy.json.internal.LazyMap



class CredentialHelper {
  
  private Domain domain
  private StoreImpl store
  private ArrayList credentials
  
  CredentialHelper() {
    this.domain = Domain.global()
    
    this.store = Jenkins.instance.getExtensionList(
          com.cloudbees.plugins.credentials.SystemCredentialsProvider.class
      )[0].getStore()
    
    this.credentials = com.cloudbees.plugins.credentials.CredentialsProvider.lookupCredentials(
          com.cloudbees.plugins.credentials.Credentials.class,
          Jenkins.instance,null,null
      )
  }
  
  /*
   *  Example data:
   *
   *  data = [
   *     id: "uuid",              // The UUID that will be used to identify       
   *     key:"",                  // The AWS access key id
   *     secret:"SECRET",         // The AWS access key secret
   *     description:"",          // The description of this secret
   *     scope: "GLOBAL",         // The scope of this key, GLOBAL or SYSTEM
   *     iamRoleArn: null,        // The IAM Role arn to apply
   *     iamMfaSerial: null       // The MFA Serial arn - if applicable
   *   ]
   */
  def createUpdateAWS(LazyMap data) {
    def current = this.credentials.findResult { it.id == data.id ? it : null };
    
    // Credential as input
    def updated = new com.cloudbees.jenkins.plugins.awscredentials.AWSCredentialsImpl(
      com.cloudbees.plugins.credentials.CredentialsScope[data.scope],
      data.id,
      data.key,
      data.secret,
      data.description,
      data.iamRoleArn,  // iamRoleArn
      data.iamMfaSerial // iamMfaSerial
    );
    
    // Update/Create
    this.createUpdate(current, updated)
  }
  

  /*
   *  Example data:
   *  data = [
   *     id: "uuid",                  // The UUID that will be used to identify
   *     secret: "secrettext",        // The secret plain text
   *     description: "Description",  // The description of this secret
   *     scope: "GLOBAL"              // The scope of this secret, GLOBAL or SYSTEM
   *   ]
   *
   */
  def createUpdateSecret(LazyMap data) {
    def current = this.credentials.findResult { it.id == data.id ? it : null };
    
    // Credential as input
    def updated = new org.jenkinsci.plugins.plaincredentials.impl.StringCredentialsImpl(
      com.cloudbees.plugins.credentials.CredentialsScope[data.scope],
      data.id,
      data.description,
      Secret.fromString(data.secret)
    );
    
    // Update/Create
    this.createUpdate(current, updated)
  }
  

  /*
   */
  def createUpdateSSHKey(LazyMap data) {
    def current = this.credentials.findResult { it.id == data.id ? it : null }
    
    def updated = new BasicSSHUserPrivateKey(
      com.cloudbees.plugins.credentials.CredentialsScope[data.scope],
      data.id,
      data.username,
      new BasicSSHUserPrivateKey.DirectEntryPrivateKeySource(data.privateKeySource),
      data.passphrase,
      data.description
    )
        
    // Update/Create
    this.createUpdate(current, updated)
  }
  

  /*
   */
  def createUpdateUsernamePassword(LazyMap data) {
    def current = this.credentials.findResult { it.id == data.id ? it : null }

    def updated = new UsernamePasswordCredentialsImpl(
      com.cloudbees.plugins.credentials.CredentialsScope[data.scope],
      data.id,
      data.description,
      data.username,
      data.password
    )
        
    // Update/Create
    this.createUpdate(current, updated)
  }


  /*
   *  Create or update credentials in the store
   */
  private def createUpdate(current, updated){
    if (current) {
      def result = store.updateCredentials(
        this.domain,
        current,
        updated
      );
    }else{
      def result = this.store.addCredentials(this.domain, updated);
    } 
  }
    
}

// Create Credentials
def json = new JsonSlurper();
def ch = new CredentialHelper()

// Load Secrets from JSON
def secrets = json.parseText("""{
    "sshkey": [
        {
            "description": "Test Key",
            "id": "001",
            "passphrase": "12345",
            "privateKeySource": "KEYCONTENTS",
            "scope": "GLOBAL",
            "username": "jenkins"
        },
        {
            "description": "Test Key 2",
            "id": "002",
            "passphrase": "12345",
            "privateKeySource": "KEYCONTENTS",
            "scope": "GLOBAL",
            "username": "root"
        },
        {
            "description": "Test Key 3",
            "id": "003",
            "passphrase": "....",
            "privateKeySource": "...",
            "scope": "GLOBAL",
            "username": "jenkins2"
        }
    ],
    "string": [
        {
            "description": "API_KEY_1",
            "id": "004",
            "scope": "GLOBAL",
            "secret": "supersecret_key"
        }
    ],
    "username_password": [
        {
            "description": "unknown",
            "id": "005",
            "password": "12345",
            "scope": "GLOBAL",
            "username": "admin"
        }
    ]
}""")

if(secrets){
  for (grp in secrets) {
    switch(grp.key){
      case "string":
      for (s in grp.value){
        ch.createUpdateSecret(s) 
      }
      break
      case "sshkey":
      for (s in grp.value){
        ch.createUpdateSSHKey(s) 
      }
      break
      case "username_password":
      for (s in grp.value){
        ch.createUpdateUsernamePassword(s) 
      }
      break
    }
  }
}
