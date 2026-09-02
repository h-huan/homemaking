package com.hm.homemaking.dto;

public class MiniBindMobileRequest
{
    private String phoneCode;
    private String encryptedData;
    private String iv;

    public String getPhoneCode()
    {
        return phoneCode;
    }

    public void setPhoneCode(String phoneCode)
    {
        this.phoneCode = phoneCode;
    }

    public String getEncryptedData()
    {
        return encryptedData;
    }

    public void setEncryptedData(String encryptedData)
    {
        this.encryptedData = encryptedData;
    }

    public String getIv()
    {
        return iv;
    }

    public void setIv(String iv)
    {
        this.iv = iv;
    }
}
