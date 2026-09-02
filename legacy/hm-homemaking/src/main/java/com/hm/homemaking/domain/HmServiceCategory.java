package com.hm.homemaking.domain;

import com.hm.common.core.domain.BaseEntity;

public class HmServiceCategory extends BaseEntity
{
    private Long categoryId;
    private Long parentId;
    private String categoryName;
    private String categoryIcon;
    private String bannerImage;
    private String bannerImageStorage;
    private String bannerImageObjectKey;
    private Integer sortNo;
    private String showInMiniapp;
    private String showInPortal;
    private String status;
    private String delFlag;

    public Long getCategoryId()
    {
        return categoryId;
    }

    public void setCategoryId(Long categoryId)
    {
        this.categoryId = categoryId;
    }

    public Long getParentId()
    {
        return parentId;
    }

    public void setParentId(Long parentId)
    {
        this.parentId = parentId;
    }

    public String getCategoryName()
    {
        return categoryName;
    }

    public void setCategoryName(String categoryName)
    {
        this.categoryName = categoryName;
    }

    public String getCategoryIcon()
    {
        return categoryIcon;
    }

    public void setCategoryIcon(String categoryIcon)
    {
        this.categoryIcon = categoryIcon;
    }

    public String getBannerImage()
    {
        return bannerImage;
    }

    public void setBannerImage(String bannerImage)
    {
        this.bannerImage = bannerImage;
    }

    public String getBannerImageStorage()
    {
        return bannerImageStorage;
    }

    public void setBannerImageStorage(String bannerImageStorage)
    {
        this.bannerImageStorage = bannerImageStorage;
    }

    public String getBannerImageObjectKey()
    {
        return bannerImageObjectKey;
    }

    public void setBannerImageObjectKey(String bannerImageObjectKey)
    {
        this.bannerImageObjectKey = bannerImageObjectKey;
    }

    public Integer getSortNo()
    {
        return sortNo;
    }

    public void setSortNo(Integer sortNo)
    {
        this.sortNo = sortNo;
    }

    public String getShowInMiniapp()
    {
        return showInMiniapp;
    }

    public void setShowInMiniapp(String showInMiniapp)
    {
        this.showInMiniapp = showInMiniapp;
    }

    public String getShowInPortal()
    {
        return showInPortal;
    }

    public void setShowInPortal(String showInPortal)
    {
        this.showInPortal = showInPortal;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public String getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(String delFlag)
    {
        this.delFlag = delFlag;
    }
}
