#!/bin/bash
echo "🔧 Building WebHarvest Frontend..."
cd /Users/robertbala/Documents/Git/web-harvest-git
mvn clean package -DskipTests -pl webharvest-ide -am
echo ""
echo "✅ Build Complete!"
echo "📦 JAR Location: webharvest-ide/target/webharvest-ide-2.3.1.jar"
echo ""
echo "🚀 To start IDE:"
echo "java -jar webharvest-ide/target/webharvest-ide-2.3.1.jar"

