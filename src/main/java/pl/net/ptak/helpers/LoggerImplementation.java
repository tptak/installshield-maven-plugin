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

    /** The log. */
    Log log;

    /**
     * Wrapping constructor.
     * 
     * @param log Log object to wrap
     */
    public LoggerImplementation( Log log )
    {
        this.log = log;
    }

    /**
     * Wrapping method around warn method
     * 
     * @param message the message
     * @param throwable throwable being logged
     * @see org.apache.maven.plugin.logging.Log#warn(CharSequence, Throwable)
     */
    public void warn( String message, Throwable throwable )
    {
        log.warn( message, throwable );
    }

    /**
     * Wrapping method around warn method
     * 
     * @param message the message
     * @see org.apache.maven.plugin.logging.Log#warn(CharSequence)
     */
    public void warn( String message )
    {
        log.warn( message );
    }

    /**
     * Does nothing.
     * 
     * @param threshold this has no meaning
     */
    public void setThreshold( int threshold )
    {
        // Just ignore this guy
    }

    /**
     * Wrapping method around debug isWarnEnabled
     * 
     * @return the result of Log's isWarnEnabled
     * @see org.apache.maven.plugin.logging.Log#isWarnEnabled()
     */
    public boolean isWarnEnabled()
    {
        return log.isWarnEnabled();
    }

    /**
     * Wrapping method around isInfoEnabled method
     * 
     * @return the result of Log's isInfoEnabled
     * @see org.apache.maven.plugin.logging.Log#isInfoEnabled()
     */
    public boolean isInfoEnabled()
    {
        return log.isInfoEnabled();
    }

    /**
     * Wrapping method around isErrorEnabled method
     * 
     * @return the result of Log's isErrorEnabled
     * @see org.apache.maven.plugin.logging.Log#isErrorEnabled()
     */
    public boolean isFatalErrorEnabled()
    {
        return log.isErrorEnabled();
    }

    /**
     * Wrapping method around isErrorEnabled method
     * 
     * @return the result of Log's isErrorEnabled
     * @see org.apache.maven.plugin.logging.Log#isErrorEnabled()
     */
    public boolean isErrorEnabled()
    {
        return log.isErrorEnabled();
    }

    /**
     * Wrapping method around isDebugEnabled method
     * 
     * @return the result of Log's isDebugEnabled
     * @see org.apache.maven.plugin.logging.Log#isDebugEnabled()
     */
    public boolean isDebugEnabled()
    {
        return log.isDebugEnabled();
    }

    /**
     * Wrapping method around info method
     * 
     * @param message the message
     * @param throwable throwable being logged
     * @see org.apache.maven.plugin.logging.Log#info(CharSequence, Throwable)
     */
    public void info( String message, Throwable throwable )
    {
        log.info( message, throwable );
    }

    /**
     * Wrapping method around info method
     * 
     * @param message the message
     * @see org.apache.maven.plugin.logging.Log#info(CharSequence)
     */
    public void info( String message )
    {
        log.info( message );
    }

    /**
     * This will allways return 0
     * 
     * @return 0
     */
    public int getThreshold()
    {
        // return anything
        return 0;
    }

    /**
     * Wrapping method around toString method
     * 
     * @return the result of Log's toString
     * @see org.apache.maven.plugin.logging.Log#toString()
     */
    public String getName()
    {
        return log.toString();
    }

    /**
     * Just returns null
     * 
     * @param name this has no meaning
     * @return null
     */
    public Logger getChildLogger( String name )
{
        return null;
    }

    /**
     * Wrapping method around error method
     * 
     * @param message the message
     * @param throwable throwable being logged
     * @see org.apache.maven.plugin.logging.Log#error(CharSequence, Throwable)
     */
    public void fatalError( String message, Throwable throwable )
    {
        log.error( message, throwable );
}

    /**
     * Wrapping method around error method
     * 
     * @param message the message
     * @see org.apache.maven.plugin.logging.Log#error(CharSequence)
     */
    public void fatalError( String message )
    {
        log.error( message );
    }

    /**
     * Wrapping method around error method
     * 
     * @param message the message
     * @param throwable throwable being logged
     * @see org.apache.maven.plugin.logging.Log#error(CharSequence, Throwable)
     */
    public void error( String message, Throwable throwable )
    {
        log.error( message, throwable );
    }

    /**
     * Wrapping method around error method
     * 
     * @param message the message
     * @see org.apache.maven.plugin.logging.Log#error(CharSequence)
     */
    public void error( String message )
    {
        log.error( message );
    }

    /**
     * Wrapping method around debug method
     * 
     * @param message the message
     * @param throwable throwable being logged
     * @see org.apache.maven.plugin.logging.Log#debug(CharSequence, Throwable)
     */
    public void debug( String message, Throwable throwable )
    {
        log.debug( message, throwable );
    }

    /**
     * Wrapping method around debug method
     * 
     * @param message the message
     * @see org.apache.maven.plugin.logging.Log#debug(CharSequence)
     */
    public void debug( String message )
    {
        log.debug( message );
    }
}
