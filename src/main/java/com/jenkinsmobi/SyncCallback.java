package com.jenkinsmobi;

public interface SyncCallback <T> {

  public abstract void onSuccess(T result);
  
  public abstract void onFailure(Throwable e);
}
