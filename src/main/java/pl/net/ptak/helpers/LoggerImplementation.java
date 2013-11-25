package pl.net.ptak.helpers;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.logging.Logger;

/**
 * This is a Plexus Logger wrapper over Maven Log object. It simply sucks that there is no clear and simple way of
 * getting one from another.
 * 
 * @author Tomasz Ptak
 */
public class LoggerImplementation
    implements Logger
{
    Log log;

    public LoggerImplementation( Log log )
    {
        this.log = log;
    }

    public void warn( String message, Throwable throwable )
    {
        log.warn( message, throwable );
    }

    public void warn( String message )
    {
        log.warn( message );
    }

    public void setThreshold( int threshold )
    {
        // Just ignore this guy
    }

    public boolean isWarnEnabled()
    {
        return log.isWarnEnabled();
    }

    public boolean isInfoEnabled()
    {
        return log.isInfoEnabled();
    }

    public boolean isFatalErrorEnabled()
    {
        return log.isErrorEnabled();
    }

    public boolean isErrorEnabled()
    {
        return log.isErrorEnabled();
    }

    public boolean isDebugEnabled()
    {
        return log.isDebugEnabled();
    }

    public void info( String message, Throwable throwable )
    {
        log.info( message, throwable );
    }

    public void info( String message )
    {
        log.info( message );
    }

    public int getThreshold()
    {
        // return anything
        return 0;
    }

    public String getName()
    {
        return log.toString();
    }

    public Logger getChildLogger( String name )
{
        return this;
    }

    public void fatalError( String message, Throwable throwable )
    {
        log.error( message, throwable );
}

    public void fatalError( String message )
    {
        log.error( message );
    }

    public void error( String message, Throwable throwable )
    {
        log.error( message, throwable );
    }

    public void error( String message )
    {
        log.error( message );
    }

    public void debug( String message, Throwable throwable )
    {
        log.debug( message, throwable );
    }

    public void debug( String message )
    {
        log.debug( message );
    }
}
