'''
*  Jenkins Groovy Script to install plugins
*
*  To find what plugins you have installed see the api
*  curl -u admin:admin http://localhost/pluginManager/api/xml?depth=1&xpath=/*/*/shortName|/*/*/version&wrapper=plugins
*
'''
import jenkins.model.*
import java.util.logging.Logger

def logger = Logger.getLogger("")

def installed = false
def initialized = false

def plugins = [
  [version: "1.19",       name: "aws-credentials"],
  [version: "1.11.119",   name: "aws-java-sdk"],
  [version: "0.3.15",     name: "awseb-deployment-plugin"],
  [version: "1.11",       name: "amazon-ecs"],
  [version: "1.19",       name: "ssh-credentials"],
  [version: "1.0.1",      name: "blueocean"],
  [version: "1.1.5",      name: "bitbucket"],
  [version: "1.3.3",      name: "bitbucket-build-status-notifier"],
  [version: "2.4.2",      name: "Office-365-Connector"],
  [version: "1.36",       name: "ec2"],
  [version: "2.15.1",     name: "maven-plugin"],
  [version: "2.1.0",      name: "saltstack"],
  [version: "2.6.1",      name: "sonar"],
  [version: "1.15",       name: "ssh-agent"],
  [version: "1.9",        name: "embeddable-build-status"],
  [version: "1.10",       name: "matrix-project"]
]

def instance = Jenkins.getInstance()

def pluginManager = instance.getPluginManager()
def updateCenter = instance.getUpdateCenter()


updateCenter.updateAllSites()


def plugin
def version
for (p in plugins) {
  
  plugin = p.name
  version = p.version

  def currentPlugin = pluginManager.getPlugin(plugin)
  if (currentPlugin) {
    def currentVersion = currentPlugin.getVersion()
    if (!currentVersion == version){
      //
      logger.info("Plugin: ${plugin} version mismatch current: ${currentVersion} expected: ${version}")
    }else{
      logger.info("Plugin: ${plugin} is installed @ ${version}") 
    }

    logger.info("Plugin ${plugin} already installed. Skipping...")
    continue

  }else{

    logger.info("Plugin ${plugin} is not installed ")
    logger.info("Searching updateCenter for ${plugin}")

    if (!initialized) {
      updateCenter.updateAllSites()
      initialized = true
      logger.info("Initialized updateCenter")
    }

    def newPlugin = updateCenter.getPlugin(plugin)

    if (newPlugin.getInstalled() == null) {
      logger.info("Installing plugin ${plugin}")
      newPlugin.deploy()
      installed = true
    }

  }
}

if (installed) {
  logger.info("Plugins installed, initializing a restart!")
  instance.save()
  //instance.doSafeRestart()
}