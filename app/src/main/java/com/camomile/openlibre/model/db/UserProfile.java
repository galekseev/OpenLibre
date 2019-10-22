package com.camomile.openlibre.model.db;

import java.util.ArrayList;
import java.util.List;

public class UserProfile {

    public enum AccountType{
        MASTER,
        LINKED,
        REQUESTED,
        DENIED
    }

    private String mEmail;
    private String mUid;
    private AccountType mType;
    private String mMasterEmail;
    private String mMasterUid;
    private List<String> mRequests;
    private List<String> mTokens;
    private List<String> mLinked;

    public UserProfile(){
        mRequests = new ArrayList<>();
        mTokens = new ArrayList<>();
        mLinked = new ArrayList<>();
        mType = AccountType.MASTER;
        mMasterEmail = null;
        mMasterUid = null;
    }

    public UserProfile(String email, String uid)
    {
        this();
        mUid = uid;
        mEmail = email;
    }

    public List<String> getRequests(){
        return this.mRequests;
    }
    public void setRequests(List<String> requests){
        mRequests.clear();
        mRequests.addAll(requests);
    }

    public List<String> getTokens(){
        return this.mTokens;
    }
    public void setTokens(List<String> tokens){
        mTokens.clear();
        mTokens.addAll(tokens);
    }


    public List<String> getLinked() { return this.mLinked; }
    public void setLinked(List<String> linked) {
        mLinked.clear();
        mLinked.addAll(linked);
    }

    public String getEmail() {
        return mEmail;
    }
    public void setEmail(String mName) {
        this.mEmail = mName;
    }

    public String getUid() {
        return mUid;
    }
    public void setUid(String mUid) {
        this.mUid = mUid;
    }

    public AccountType getType() {
        return mType;
    }
    public void setType(AccountType mType) {
        this.mType = mType;
    }

    public String getMaster() {
        return mMasterEmail;
    }
    public void setMaster(String masterEmail) {
        this.mMasterEmail = masterEmail;
    }

    public String getMasterUid() { return mMasterUid; }
    public void setMasterUid(String mMasterUid) { this.mMasterUid = mMasterUid; }
}
