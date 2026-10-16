$env:JAVA_HOME = "C:\Program Files\Java\jdk-21"
Write-Output "Stopping any existing Gradle daemons..."
.\gradlew.bat --stop

Write-Output "Starting local compilation with Java 21..."
.\gradlew.bat clean assemble -x test -x cucumber -x rat -x spotlessJava -x spotlessCheck -x checkstyleMain -x checkstyleTest -x spotbugsMain -x spotbugsTest -x modernizer --no-daemon --console=plain
