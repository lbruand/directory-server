/*
 *   Copyright 2004 The Apache Software Foundation
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 *
 */
package org.apache.eve.db;


import java.util.List;
import java.util.ArrayList;
import java.util.Collections;

import javax.naming.NamingException;
import javax.naming.NamingEnumeration;


/**
 * A enumeration decorator which filters database search results as they are
 * being enumerated back to the client caller.
 *
 * @see ResultFilter
 * @author <a href="mailto:directory-dev@incubator.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ResultFilteringEnumeration implements NamingEnumeration
{
    /** the list of filters to be applied */
    private final ArrayList filters;
    /** the underlying decorated enumeration */
    private final NamingEnumeration decorated;

    /** the first accepted search result that is prefetched */
    private DbSearchResult prefetched;
    /** flag storing closed state of this naming enumeration */
    private boolean isClosed = false;


    // ------------------------------------------------------------------------
    // C O N S T R U C T O R S
    // ------------------------------------------------------------------------


    /**
     * Creates a new database result filtering enumeration to decorate an
     * underlying enumeration.
     *
     * @param decorated the underlying decorated enumeration
     */
    public ResultFilteringEnumeration( NamingEnumeration decorated )
            throws NamingException
    {
        this.filters = new ArrayList();
        this.decorated = decorated;

        if ( ! decorated.hasMore() )
        {
            close();
            return;
        }

        prefetch();
    }


    // ------------------------------------------------------------------------
    // New ResultFilter management methods
    // ------------------------------------------------------------------------


    /**
     * Adds a database search result filter to this filtering enumeration at
     * the very end of the filter list.  Filters are applied in the order of
     * addition.
     *
     * @param filter a filter to apply to the results
     * @return the result of {@link List#add(Object)}
     */
    public boolean addResultFilter( ResultFilter filter )
    {
        return filters.add( filter );
    }


    /**
     * Removes a database search result filter from the filter list of this
     * filtering enumeration.
     *
     * @param filter a filter to remove from the filter list
     * @return the result of {@link List#remove(Object)}
     */
    public boolean removeResultFilter( ResultFilter filter )
    {
        return filters.remove( filter );
    }


    /**
     * Gets an unmodifiable list of filters.
     *
     * @return the result of {@link Collections#unmodifiableList(List)}
     */
    public List getFilters()
    {
        return Collections.unmodifiableList( filters );
    }


    // ------------------------------------------------------------------------
    // NamingEnumeration Methods
    // ------------------------------------------------------------------------


    public void close() throws NamingException
    {
        isClosed = true;
        decorated.close();
    }


    public boolean hasMore()
    {
        return !isClosed;
    }


    public Object next() throws NamingException
    {
        DbSearchResult retVal = this.prefetched;
        prefetch();
        return retVal;
    }


    // ------------------------------------------------------------------------
    // Enumeration Methods
    // ------------------------------------------------------------------------


    public boolean hasMoreElements()
    {
        return !isClosed;
    }


    public Object nextElement()
    {
        DbSearchResult retVal = this.prefetched;

        try
        {
            prefetch();
        }
        catch ( NamingException e )
        {
            e.printStackTrace();
        }

        return retVal;
    }


    // ------------------------------------------------------------------------
    // Private utility methods
    // ------------------------------------------------------------------------


    /**
     * Keeps getting results from the underlying decorated filter and applying
     * the filters until a result is accepted by all and set as the prefetced
     * result to return on the next() result request.  If no prefetched value
     * can be found before exhausting the decorated enumeration, then this and
     * the underlying enumeration is closed.
     *
     * @throws NamingException if there are problems getting results from the
     * underlying enumeration
     */
    private void prefetch() throws NamingException
    {
        DbSearchResult tmp = null;

        while( decorated.hasMore() )
        {
            boolean accepted = true;
            tmp = ( DbSearchResult ) decorated.next();

            // don't waste using a for loop if we got 0 or 1 element
            if ( filters.isEmpty() )
            {
                this.prefetched = tmp;
                return;
            }
            else if ( filters.size() == 1 )
            {
                accepted = ( ( ResultFilter ) filters.get( 0 ) ).accept( tmp );
                this.prefetched = tmp;
                return;
            }

            // apply all filters shorting their application on result denials
            for ( int ii = 0; ii < filters.size(); ii ++ )
            {
                ResultFilter filter = ( ResultFilter ) filters.get( ii );
                accepted &= filter.accept( tmp );

                if ( ! accepted )
                {
                    continue;
                }
            }

            /*
             * If we get here then a result has been accepted by all the
             * filters so we set the result as the prefetched value to return
             * on the following call to the next() or nextElement() methods
             */
            this.prefetched = tmp;
            return;
        }

        /*
         * If we get here then no result was found to be accepted by all
         * filters before we exhausted the decorated enumeration so we close
         */
        close();
    }
}
