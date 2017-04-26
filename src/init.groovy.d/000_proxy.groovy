'''
Jenkins Proxy Configuration Script

    Reads the environment variable 'http_proxy' 
    Makes the configuration changes to the ProxyConfiguration 

'''
import jenkins.model.*
import hudson.ProxyConfiguration
import java.util.logging.Logger

def logger = Logger.getLogger("")
def instance = Jenkins.getInstance()
def env = System.getenv()

def URI proxy = new URI(env.http_proxy)

def cfg = [
  name: proxy.host,
  port: proxy.port,
  userName: "",
  password: "",
  noProxyHost: "",
  testUrl: "https://google.ie"  
]

def pc = new ProxyConfiguration(
  cfg.name,
  cfg.port,
  cfg.userName,
  cfg.password,
  cfg.noProxyHost,
  cfg.testUrl
)

// Enforce null for empty string
if(cfg.userName == ""){
  cfg.userName = null
}

def update = false
if (instance.proxy){
  def ProxyConfiguration currentCfg = instance.proxy
  for(item in cfg){
    if(item.key != "noProxyHost"){
      if(currentCfg."${item.key}" != item.value){
        println item.key
        println currentCfg."${item.key}"
        println "'" + item.value + "'"
        logger.info("Proxy configuration changed update required")
        update = true
        break
      }
    }else{
       // ToDo: noProxyHostPatterns Check
    }
  }
}else{
    update = true
}


println "UPDATE? ${update}"
if(update == true){
  logger.info("Updating Proxy configuration")
  instance.proxy = pc
  instance.proxy.save()
  instance.save()
  instance.doSafeRestart()
}else{
  logger.info("No Proxy Update needed")
}
