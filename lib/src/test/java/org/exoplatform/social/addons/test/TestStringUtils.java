package org.exoplatform.social.addons.test;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.social.addons.updater.utils.StringUtil;

public class TestStringUtils extends BaseExoTestCase {  
  
  public void testRemoveLongUTF() {
    String str = "az😀↷♠️®️©️𩸽𝌆𝞢09";
    //remove 4 bytes utf-8 characters
    assertEquals("az↷♠️®️©️09", StringUtil.removeLongUTF(str));
  }
}
