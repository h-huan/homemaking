package com.hm.homemaking.mapper;

import com.hm.homemaking.domain.HmWorkerProfile;
import java.util.List;

public interface HmWorkerMapper
{
    List<HmWorkerProfile> selectWorkerList(HmWorkerProfile query);

    HmWorkerProfile selectWorkerById(Long workerId);

    int insertWorkerProfile(HmWorkerProfile workerProfile);

    int updateWorkerProfile(HmWorkerProfile workerProfile);

    int deleteWorkerServices(Long workerId);

    int insertWorkerService(Long workerId, Long serviceItemId);

    int deleteWorkerAreas(Long workerId);

    int insertWorkerArea(Long workerId, Long areaId);

    List<Long> selectServiceIdsByWorkerId(Long workerId);

    List<Long> selectAreaIdsByWorkerId(Long workerId);

    List<HmWorkerProfile> selectRecommendWorkerList();
}
