package com.hm.homemaking.domain;

import com.hm.common.core.domain.BaseEntity;
import java.math.BigDecimal;
import java.util.List;

public class HmWorkerProfile extends BaseEntity
{
    private Long workerProfileId;
    private Long userId;
    private String workerName;
    private String mobile;
    private String gender;
    private String avatar;
    private String idCardNo;
    private String workerType;
    private String intro;
    private BigDecimal serviceStar;
    private Integer serviceCount;
    private String employmentStatus;
    private String workStatus;
    private Long orgId;
    private String status;
    private List<Long> serviceItemIdList;
    private List<Long> areaIdList;

    public Long getWorkerProfileId()
    {
        return workerProfileId;
    }

    public void setWorkerProfileId(Long workerProfileId)
    {
        this.workerProfileId = workerProfileId;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public String getWorkerName()
    {
        return workerName;
    }

    public void setWorkerName(String workerName)
    {
        this.workerName = workerName;
    }

    public String getMobile()
    {
        return mobile;
    }

    public void setMobile(String mobile)
    {
        this.mobile = mobile;
    }

    public String getGender()
    {
        return gender;
    }

    public void setGender(String gender)
    {
        this.gender = gender;
    }

    public String getAvatar()
    {
        return avatar;
    }

    public void setAvatar(String avatar)
    {
        this.avatar = avatar;
    }

    public String getIdCardNo()
    {
        return idCardNo;
    }

    public void setIdCardNo(String idCardNo)
    {
        this.idCardNo = idCardNo;
    }

    public String getWorkerType()
    {
        return workerType;
    }

    public void setWorkerType(String workerType)
    {
        this.workerType = workerType;
    }

    public String getIntro()
    {
        return intro;
    }

    public void setIntro(String intro)
    {
        this.intro = intro;
    }

    public BigDecimal getServiceStar()
    {
        return serviceStar;
    }

    public void setServiceStar(BigDecimal serviceStar)
    {
        this.serviceStar = serviceStar;
    }

    public Integer getServiceCount()
    {
        return serviceCount;
    }

    public void setServiceCount(Integer serviceCount)
    {
        this.serviceCount = serviceCount;
    }

    public String getEmploymentStatus()
    {
        return employmentStatus;
    }

    public void setEmploymentStatus(String employmentStatus)
    {
        this.employmentStatus = employmentStatus;
    }

    public String getWorkStatus()
    {
        return workStatus;
    }

    public void setWorkStatus(String workStatus)
    {
        this.workStatus = workStatus;
    }

    public Long getOrgId()
    {
        return orgId;
    }

    public void setOrgId(Long orgId)
    {
        this.orgId = orgId;
    }

    public String getStatus()
    {
        return status;
    }

    public void setStatus(String status)
    {
        this.status = status;
    }

    public List<Long> getServiceItemIdList()
    {
        return serviceItemIdList;
    }

    public void setServiceItemIdList(List<Long> serviceItemIdList)
    {
        this.serviceItemIdList = serviceItemIdList;
    }

    public List<Long> getAreaIdList()
    {
        return areaIdList;
    }

    public void setAreaIdList(List<Long> areaIdList)
    {
        this.areaIdList = areaIdList;
    }
}
