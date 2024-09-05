//
// Copyright (c) 2016, Brian Frank
// Licensed under the Academic Free License version 3.0
//
// History:
//   31 May 2016  Matthew Giannini  Creation
//
package org.projecthaystack.util;

import org.projecthaystack.HDict;
import org.projecthaystack.HGrid;
import a75f.io.logger.CcuLog;

public class WebUtil
{
  public static boolean isToken(String s)
  {
    if (s == null || s.isEmpty()) return false;
    for (int i = 0; i< s.length(); ++i)
    {
      if (!isTokenChar(s.codePointAt(i))) return false;
    }
    return true;

  }

  public static boolean isTokenChar(final int codePoint)
  {
    return codePoint < 127 && tokenChars[codePoint];
  }

  private static boolean[] tokenChars;
  static
  {
    boolean[] m = new boolean[127];
    for (int i = 0; i < 127; ++i)
    {
      m[i] = i > 0x20;
    }
    m['(']  = false;  m[')'] = false;  m['<']  = false;  m['>'] = false;
    m['@']  = false;  m[','] = false;  m[';']  = false;  m[':'] = false;
    m['\\'] = false;  m['"'] = false;  m['/']  = false;  m['['] = false;
    m[']']  = false;  m['?'] = false;  m['=']  = false;  m['{'] = false;
    m['}']  = false;  m[' '] = false;  m['\t'] = false;
    tokenChars = m;
  }
  public static int getResponsePageSize(HGrid response, int pageSize) {
    HDict meta = response.meta();
    if (meta.has("total")) {
      int entitySize = (int) Double.parseDouble(meta.get("total").toString());
      CcuLog.i("CCU_HCLIENT", "Total Entity size " + entitySize);
      return entitySize / pageSize;
    }
    CcuLog.i("CCU_HCLIENT", "No total field in metadata");
    return 0;
  }

}
