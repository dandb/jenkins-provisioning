'''
Jenkins Init script
    Disables FirstRun Wizard

    WARNING: INSECURE 
    Used for localhost development only!

    Sets default credentials
    Would be overwriten by the AD script

    ToDo:
    * Make idempotent
    * Generate use and write random password to file.
'''
import jenkins.model.*
import hudson.security.*

def instance = Jenkins.getInstance()


def hudsonRealm = new HudsonPrivateSecurityRealm(false)
hudsonRealm.createAccount('admin','admin')
instance.setSecurityRealm(hudsonRealm)

def strategy = new FullControlOnceLoggedInAuthorizationStrategy()
instance.setAuthorizationStrategy(strategy)
instance.install.runSetupWizard=false
instance.save()