package com.jenkinsmobi;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;

public class JenkinsMobiClient {

  private static final String JENKINSMOBI_INTENT_DEEPLINK = "jenkinsmobi://%s?APK-MD5=%s&PackageName=%s";
  private Activity activity;
  private Configuration configuration;
  private JenkinsCloudAPIClient api;
  private String updateDeepLink;
  private Logger log;
  private boolean running;

  public JenkinsMobiClient(Activity activity) {
    this.activity = activity;
    this.configuration = Configuration.getInstance(activity);
    this.api = new JenkinsCloudAPIClient(activity);
    this.log = Logger.getInstance();
    configuration.detectSubscriberId();
  }

  public void onStart() {
    running = true;
    checkForUpdates(new OnUpdateAvailable() {

      @Override
      public void update() {
        showUpgradeAlert();
      }
    });
  }
  
  public void onPause() {
    running = false;
  }
  
  public void onResume() {
    running = true;
  }

  private void showUpgradeAlert() {
    if(!running) {
      log.info("App not running: upgrade alert not shown");
      return;
    }
    
    final AlertDialog alertDialog = new AlertDialog.Builder(activity).create();

    alertDialog.setTitle("Update available");
    alertDialog
        .setMessage("There is a new version available for this App: do you want to review it / upgrade ?");
    alertDialog.setButton(AlertDialog.BUTTON_POSITIVE, "Yes",
        new DialogInterface.OnClickListener() {
          public void onClick(DialogInterface dialog, int which) {
            deepLinkToUpdate();
            alertDialog.dismiss();
          }
        });
    alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "No",
        new DialogInterface.OnClickListener() {
          @Override
          public void onClick(DialogInterface arg0, int arg1) {
          }
        });
    alertDialog.show();
  }

  public void deepLinkToUpdate() {
    if (updateDeepLink == null) {
      checkForUpdates(new OnUpdateAvailable() {

        @Override
        public void update() {
          activity.startActivity(new Intent("android.intent.action.VIEW", Uri
              .parse(updateDeepLink)));
        }
      });
    } else {
      activity.startActivity(new Intent("android.intent.action.VIEW", Uri
          .parse(updateDeepLink)));
    }
  }

  private class CheckForUpdates extends AsyncTask<Void, Void, Boolean> {
    private OnUpdateAvailable updateCallback;

    public CheckForUpdates(OnUpdateAvailable updateCallback) {
      this.updateCallback = updateCallback;
    }

    @Override
    protected Boolean doInBackground(Void... arg0) {
      api.callSync("/qaexplorer/upgrade", new SyncCallback<JenkinsCloudNode>() {

        @Override
        public void onSuccess(JenkinsCloudNode result) {
          if (result instanceof JenkinsCloudDataNode) {
            updateDeepLink =
                getDeepLink("/qaexplorer/upgrade",
                    ((JenkinsCloudDataNode) result).path);
          }
        }

        @Override
        public void onFailure(Throwable e) {
          log.error("Check for application updates has failed", e);
          updateDeepLink = null;
        }
      }, configuration.getRequestHeaders(), null, null);

      return updateDeepLink != null;
    }

    @Override
    protected void onPostExecute(Boolean result) {
      if (result) {
        updateCallback.update();
      }
    }

  }

  public void checkForUpdates(OnUpdateAvailable updateCallback) {
    updateDeepLink = null;
    new CheckForUpdates(updateCallback).execute(new Void[] {});
  }

  private String getDeepLink(String basePath, String path) {
    String fullPath;

    if (path.startsWith("/")) {
      fullPath = path;
    } else {
      fullPath = basePath + (basePath.endsWith("/") ? "" : "/") + path;
    }

    return String.format(JENKINSMOBI_INTENT_DEEPLINK, fullPath,
        configuration.apkFileMD5, configuration.packageName);
  }

}
