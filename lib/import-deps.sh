#!/bin/bash


if [ $# -lt 2 ]
then
	echo "Provide API-Gateway installation folder and version. "
	echo "For example: "
	echo "./lib/import-deps.sh /opt/Axway/APIM/apigateway 7.7.20200930"
	exit 10
fi

local_repo=apigateway-dependencies

# Copy required jars from an existing installation folder
gatewayInstallationFolder=$1
gatewayVersion=$2

copyDeps() {
    local givenFile=$1
	local group=$2
    local artifact=$3
    local version=$4
	
	# Locate the JAR within the Gateway installation folder
	file=`ls -1 ${givenFile}`
	
    mvn install:install-file \
            -Dfile=${file} \
            -DgroupId=${group} \
            -DartifactId=${artifact} \
            -Dversion=${version} \
            -Dpackaging=jar \
            -DgeneratePom=true \
            -DlocalRepositoryPath=${local_repo}
}


copyDeps "${gatewayInstallationFolder}/system/lib/plugins/vordel-mime-7.7.0*.jar" com.vordel.mime vordel-mime ${gatewayVersion}
copyDeps "${gatewayInstallationFolder}/system/lib/plugins/apigw-common-2.4.0.jar" com.axway.apigw apigw-common 2.3.0
#copyDeps "${gatewayInstallationFolder}/system/lib/pluings/vordel-common-7.7.0*.jar" com.vordel.common vordel-common ${gatewayVersion}
copyDeps "${gatewayInstallationFolder}/system/lib/plugins/vordel-trace-7.7.0*.jar" com.vordel.trace vordel-trace ${gatewayVersion}
copyDeps "${gatewayInstallationFolder}/system/lib/vordel-api-model-7.7.0*.jar" com.vordel vordel-api-model ${gatewayVersion}
copyDeps "${gatewayInstallationFolder}/system/lib/vordel-core-runtime-7.7.0*.jar" com.vordel vordel-core-runtime ${gatewayVersion}
