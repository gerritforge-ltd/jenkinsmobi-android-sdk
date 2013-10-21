package com.jenkinsmobi;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import android.content.Context;

public class JenkinsCloudAPIClient {

  private static Logger log = Logger.getInstance();
  private Context ctx;
  private boolean cacheHit = false;
  private boolean forceRefresh = false;
  private boolean appendMode = false;

  public JenkinsCloudAPIClient(Context ctx) {
    this.ctx = ctx;
  }

  public void setForceRefresh(boolean refresh) {
    forceRefresh = refresh;
  }

  public void setAppendMode(boolean mode) {
    appendMode = mode;
  }

  public boolean isCacheHit() {
    return cacheHit;
  }

  public void callSync(final String path,
      final SyncCallback<JenkinsCloudNode> syncCallback,
      Map<String, String> headers, String itemsFrom, String items) {

	    
    final HudsonMobiSynchHttpClient client = new HudsonMobiSynchHttpClient();
    client.setUserHeaders(headers);
    String query =
        path;
    if (itemsFrom != null && items != null) {
    	query += (query.indexOf('?') > 0 ? "&":"?");
      query +=
          "x-jenkinscloud-accept-items=" + items
              + "&x-jenkinscloud-accept-from=" + itemsFrom;
    }
    client.call(forceRefresh, query, new SyncCallback<JenkinsCloudNode>() {

      @Override
      public void onSuccess(JenkinsCloudNode result) {
        Configuration.getInstance().setLastRefreshTimestamp(
            "" + new Date().getTime());
        Configuration.getInstance().save();
        syncCallback.onSuccess(result);
      }

      @Override
      public void onFailure(Throwable e) {
        syncCallback.onFailure(e);
      }
    });
  }
}
