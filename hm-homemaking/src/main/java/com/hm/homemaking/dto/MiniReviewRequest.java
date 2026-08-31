package com.hm.homemaking.dto;

import java.util.List;

public class MiniReviewRequest
{
    private Long orderId;
    private Integer score;
    private List<String> tags;
    private String content;
    private List<String> images;
    private String isAnonymous;

    public Long getOrderId()
    {
        return orderId;
    }

    public void setOrderId(Long orderId)
    {
        this.orderId = orderId;
    }

    public Integer getScore()
    {
        return score;
    }

    public void setScore(Integer score)
    {
        this.score = score;
    }

    public List<String> getTags()
    {
        return tags;
    }

    public void setTags(List<String> tags)
    {
        this.tags = tags;
    }

    public String getContent()
    {
        return content;
    }

    public void setContent(String content)
    {
        this.content = content;
    }

    public List<String> getImages()
    {
        return images;
    }

    public void setImages(List<String> images)
    {
        this.images = images;
    }

    public String getIsAnonymous()
    {
        return isAnonymous;
    }

    public void setIsAnonymous(String isAnonymous)
    {
        this.isAnonymous = isAnonymous;
    }
}
