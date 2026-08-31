package com.hm.homemaking.domain;

import java.util.Date;

public class HmPortalContent
{
    private Long contentId;
    private String contentType;
    private String title;
    private String subTitle;
    private String coverImage;
    private String coverImageStorage;
    private String coverImageObjectKey;
    private String summary;
    private String contentHtml;
    private Integer sortNo;
    private String publishStatus;
    private Date publishTime;
    private Date updateTime;

    public Long getContentId()
    {
        return contentId;
    }

    public void setContentId(Long contentId)
    {
        this.contentId = contentId;
    }

    public String getContentType()
    {
        return contentType;
    }

    public void setContentType(String contentType)
    {
        this.contentType = contentType;
    }

    public String getTitle()
    {
        return title;
    }

    public void setTitle(String title)
    {
        this.title = title;
    }

    public String getSubTitle()
    {
        return subTitle;
    }

    public void setSubTitle(String subTitle)
    {
        this.subTitle = subTitle;
    }

    public String getCoverImage()
    {
        return coverImage;
    }

    public void setCoverImage(String coverImage)
    {
        this.coverImage = coverImage;
    }

    public String getCoverImageStorage()
    {
        return coverImageStorage;
    }

    public void setCoverImageStorage(String coverImageStorage)
    {
        this.coverImageStorage = coverImageStorage;
    }

    public String getCoverImageObjectKey()
    {
        return coverImageObjectKey;
    }

    public void setCoverImageObjectKey(String coverImageObjectKey)
    {
        this.coverImageObjectKey = coverImageObjectKey;
    }

    public String getSummary()
    {
        return summary;
    }

    public void setSummary(String summary)
    {
        this.summary = summary;
    }

    public String getContentHtml()
    {
        return contentHtml;
    }

    public void setContentHtml(String contentHtml)
    {
        this.contentHtml = contentHtml;
    }

    public Integer getSortNo()
    {
        return sortNo;
    }

    public void setSortNo(Integer sortNo)
    {
        this.sortNo = sortNo;
    }

    public String getPublishStatus()
    {
        return publishStatus;
    }

    public void setPublishStatus(String publishStatus)
    {
        this.publishStatus = publishStatus;
    }

    public Date getPublishTime()
    {
        return publishTime;
    }

    public void setPublishTime(Date publishTime)
    {
        this.publishTime = publishTime;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }
}
