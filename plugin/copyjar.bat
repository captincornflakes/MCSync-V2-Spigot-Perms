@echo off
setlocal

rem Define source and destination directories
set "source=.\target"
set "destination=C:\Users\chron\OneDrive\Desktop\MCSync Test Server\plugins"

rem Create the destination directory if it doesn't exist
if not exist "%destination%" (
    mkdir "%destination%"
)

rem Copy .jar files from the source to the destination
xcopy "%source%\MCSync-V2-Permissions-Spigot-2.0.0.jar" "%destination%\" /y

echo Copy completed.
endlocal
