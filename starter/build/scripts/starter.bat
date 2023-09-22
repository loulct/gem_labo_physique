@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem

@if "%DEBUG%" == "" @echo off
@rem ##########################################################################
@rem
@rem  starter startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%" == "" set DIRNAME=.
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and STARTER_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if "%ERRORLEVEL%" == "0" goto execute

echo.
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH.
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo.
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME%
echo.
echo Please set the JAVA_HOME variable in your environment to match the
echo location of your Java installation.

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\starter-1.0.0-SNAPSHOT.jar;%APP_HOME%\lib\vertx-web-4.4.4.jar;%APP_HOME%\lib\vertx-web-client-4.4.4.jar;%APP_HOME%\lib\vertx-auth-properties-4.4.4.jar;%APP_HOME%\lib\vertx-mail-client-4.4.4.jar;%APP_HOME%\lib\vertx-web-templ-handlebars-4.4.4.jar;%APP_HOME%\lib\vertx-pg-client-4.4.4.jar;%APP_HOME%\lib\vertx-sql-client-4.4.4.jar;%APP_HOME%\lib\vertx-auth-mongo-4.4.4.jar;%APP_HOME%\lib\vertx-mongo-client-4.4.4.jar;%APP_HOME%\lib\vertx-web-common-4.4.4.jar;%APP_HOME%\lib\vertx-auth-common-4.4.4.jar;%APP_HOME%\lib\vertx-bridge-common-4.4.4.jar;%APP_HOME%\lib\vertx-uri-template-4.4.4.jar;%APP_HOME%\lib\vertx-core-4.4.4.jar;%APP_HOME%\lib\postgresql-1.12.4.jar;%APP_HOME%\lib\client-2.1.jar;%APP_HOME%\lib\postgresql-42.2.18.jar;%APP_HOME%\lib\netty-handler-proxy-4.1.94.Final.jar;%APP_HOME%\lib\netty-codec-http2-4.1.94.Final.jar;%APP_HOME%\lib\netty-codec-http-4.1.94.Final.jar;%APP_HOME%\lib\netty-resolver-dns-4.1.94.Final.jar;%APP_HOME%\lib\netty-handler-4.1.94.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.94.Final.jar;%APP_HOME%\lib\netty-codec-socks-4.1.94.Final.jar;%APP_HOME%\lib\netty-codec-dns-4.1.94.Final.jar;%APP_HOME%\lib\netty-codec-4.1.94.Final.jar;%APP_HOME%\lib\netty-transport-4.1.94.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.94.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.94.Final.jar;%APP_HOME%\lib\netty-common-4.1.94.Final.jar;%APP_HOME%\lib\jackson-core-2.15.0.jar;%APP_HOME%\lib\jdbc-1.12.4.jar;%APP_HOME%\lib\database-commons-1.12.4.jar;%APP_HOME%\lib\testcontainers-1.12.4.jar;%APP_HOME%\lib\tcp-unix-socket-proxy-1.0.2.jar;%APP_HOME%\lib\slf4j-api-1.7.29.jar;%APP_HOME%\lib\handlebars-4.3.0.jar;%APP_HOME%\lib\mongodb-driver-reactivestreams-4.8.1.jar;%APP_HOME%\lib\reactive-streams-1.0.3.jar;%APP_HOME%\lib\commons-collections4-4.2.jar;%APP_HOME%\lib\common-2.1.jar;%APP_HOME%\lib\checker-qual-3.5.0.jar;%APP_HOME%\lib\mongodb-driver-core-4.8.1.jar;%APP_HOME%\lib\bson-record-codec-4.8.1.jar;%APP_HOME%\lib\bson-4.8.1.jar;%APP_HOME%\lib\reactor-core-3.5.0.jar;%APP_HOME%\lib\saslprep-1.1.jar;%APP_HOME%\lib\fastdoubleparser-0.8.0.jar;%APP_HOME%\lib\stringprep-1.1.jar;%APP_HOME%\lib\junit-4.12.jar;%APP_HOME%\lib\annotations-17.0.0.jar;%APP_HOME%\lib\javax.annotation-api-1.3.2.jar;%APP_HOME%\lib\commons-compress-1.19.jar;%APP_HOME%\lib\jaxb-api-2.3.1.jar;%APP_HOME%\lib\duct-tape-1.0.8.jar;%APP_HOME%\lib\visible-assertions-2.1.2.jar;%APP_HOME%\lib\jna-platform-5.5.0.jar;%APP_HOME%\lib\hamcrest-core-1.3.jar;%APP_HOME%\lib\javax.activation-api-1.2.0.jar;%APP_HOME%\lib\junixsocket-native-common-2.0.4.jar;%APP_HOME%\lib\junixsocket-common-2.0.4.jar;%APP_HOME%\lib\jna-5.5.0.jar;%APP_HOME%\lib\native-lib-loader-2.0.2.jar


@rem Execute starter
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %STARTER_OPTS%  -classpath "%CLASSPATH%" io.vertx.core.Launcher %*

:end
@rem End local scope for the variables with windows NT shell
if "%ERRORLEVEL%"=="0" goto mainEnd

:fail
rem Set variable STARTER_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
if  not "" == "%STARTER_EXIT_CONSOLE%" exit 1
exit /b 1

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
