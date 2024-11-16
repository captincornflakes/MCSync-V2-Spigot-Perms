f:\Workspace\apache-maven-3.9.9\bin\mvn clean package

setlocal

rem Define source and destination directories
set "source=.\target"
set "destination=F:\Workspace\Spigot test server\plugins"

rem Create the destination directory if it doesn't exist
if not exist "%destination%" (
    mkdir "%destination%"
)

rem Copy .jar files from the source to the destination
xcopy "MCSync-V2-Spigot-2.0.0.jar" "%destination%\" /y

echo Copy completed.
endlocal
