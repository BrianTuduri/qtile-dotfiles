
def yamlConfigMap = readYaml file: 'config.yml'

println yamlConfigMap["docker"]["image"]