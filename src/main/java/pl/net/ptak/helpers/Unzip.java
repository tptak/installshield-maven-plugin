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

import org.codehaus.plexus.components.io.fileselectors.IncludeExcludeFileSelector;

/**
 * The Class Unzip. Represents the dependency to unzip.
 * 
 * @author pc
 */
public class Unzip
{

    /**
     * What to unpack. This is selected by simple startsWith comparison on Artifact identifier in form
     * groupId:artifactId:type:version. If you write just a part of that, it will still work. If more than one element
     * is found, all will be unpacked.
     */
    private String what;

    /**
     * Where to unpack. Name of a subfolder under target/dependency. If not set, form of artifactId_type will be used
     */
    private String where;

    /** The paths to extract from the archive. */
    private IncludeExcludeFileSelector files;


    /**
     * Gets the what.
     * 
     * @return the what
     */
    public String getWhat()
    {
        return what;
    }

    /**
     * Sets what to unpack. This is selected by simple startsWith comparison on Artifact identifier in form
     * groupId:artifactId:type:version. If you write just a part of that, it will still work. If more than one element
     * is found, all will be unpacked.
     * 
     * @param what the value to compare upon.
     */
    public void setWhat( String what )
    {
        this.what = what;
    }

    /**
     * Gets the where.
     * 
     * @return the where
     */
    public String getWhere()
    {
        return where;
    }

    /**
     * Sets Where to unpack. Name of a subfolder under target/dependency. If not set, form of artifactId_type will be
     * used
     * 
     * @param where where to unpack
     */
    public void setWhere( String where )
    {
        this.where = where;
    }

    /**
     * FileSet what to include/exclude from the extraction
     * 
     * @return the files
     */
    public IncludeExcludeFileSelector getFiles()
    {
        return files;
    }

    /**
     * FileSet what to include/exclude from the extraction
     * 
     * @param files the files to set
     */
    public void setFiles( IncludeExcludeFileSelector files )
    {
        this.files = files;
    }


}
