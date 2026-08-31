package com.hm.homemaking.domain;

import java.util.Date;

public class HmReview
{
    private Long reviewId;
    private Long orderId;
    private Long customerId;
    private Long workerId;
    private Integer score;
    private String tagsJson;
    private String content;
    private String imagesJson;
    private String isAnonymous;
    private String status;
    private Date createTime;

    public Long getReviewId()
    {
        return reviewId;
    }

    public void setReviewId(Long reviewId)
    {
        this.reviewId = reviewId;
    }

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Long getCustomerId()
    {
        return customerId;
    }

    public void setCustomerId(Long customerId)
    {
        this.customerId = customerId;
    }

    public Long getWorkerId()
    {
        return workerId;
    }

    public void setWorkerId(Long workerId)
    {
        this.workerId = workerId;
    }

    public Integer getScore()
    {
        return score;
    }

    public void setScore(Integer score)
    {
        this.score = score;
    }

    public String getTagsJson()
    {
        return tagsJson;
    }

    public void setTagsJson(String tagsJson)
    {
        this.tagsJson = tagsJson;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public String getImagesJson()
    {
        return imagesJson;
    }

    public void setImagesJson(String imagesJson)
    {
        this.imagesJson = imagesJson;
    }

    public String getIsAnonymous()
    {
        return isAnonymous;
    }

    public void setIsAnonymous(String isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }
}
