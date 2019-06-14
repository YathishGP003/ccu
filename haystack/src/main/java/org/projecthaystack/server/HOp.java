//
// Copyright (c) 2012, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   25 Sep 2012  Brian Frank  Creation
//
package org.projecthaystack.server;

import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import org.projecthaystack.HRef;
import org.projecthaystack.HUri;
import org.projecthaystack.HVal;

/**
 * HOp is the base class for server side operations exposed by the REST API.
 * All methods on HOp must be thread safe.
 *
 * @see <a href='http://project-haystack.org/doc/Ops'>Project Haystack</a>
 */
public abstract class HOp
{


    /** Programatic name of the operation. */
  public abstract String name();

  /** Short one line summary of what the operation does. */
  public abstract String summary();


  /**
   * Service the request and return response.
   */
  public HGrid onService(HServer db, HGrid req)
    throws Exception
  {
    throw new UnsupportedOperationException(getClass().getName()+".onService(HServer,HGrid)");
  }



  HRef[] gridToIds(HServer db, HGrid grid)
  {
    HRef[] ids = new HRef[grid.numRows()];
    for (int i=0; i<ids.length; ++i)
    {
      HVal val = grid.row(i).get("id");
      ids[i] = valToId(db, val);
    }
    return ids;
  }

  HRef valToId(HServer db, HVal val)
  {
    if (val instanceof HUri)
    {
      HDict rec = db.navReadByUri((HUri)val, false);
      return rec == null ? HRef.nullRef : rec.id();
    }
    else
    {
      return (HRef)val;
    }
  }

}
